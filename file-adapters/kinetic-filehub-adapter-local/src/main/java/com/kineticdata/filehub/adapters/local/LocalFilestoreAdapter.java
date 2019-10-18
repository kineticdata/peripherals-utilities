package com.kineticdata.filehub.adapters.local;

import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import com.kineticdata.filehub.adapter.FilestoreAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFilestoreAdapter implements FilestoreAdapter {
    
    /** Static class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFilestoreAdapter.class);

    /** Defines the adapter display name. */
    public static final String NAME = "Local";
    
    /** Defines the collection of property names for the adapter. */
    public static class Properties {
        public static final String DIRECTORY = "Directory";
    }
    
    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try (
            InputStream inputStream = LocalFilestoreAdapter.class.getResourceAsStream(
                "/"+LocalFilestoreAdapter.class.getName()+".version")
        ) {
            java.util.Properties properties = new java.util.Properties();
            properties.load(inputStream);
            VERSION = properties.getProperty("version")+"-"+properties.getProperty("build");
        } catch (IOException e) {
            LOGGER.warn("Unable to load "+LocalFilestoreAdapter.class.getName()+" version properties.", e);
            VERSION = "Unknown";
        }
    }
    
    /** 
     * Specifies the configurable properties for the adapter.  These are populated as part of object
     * construction so that the collection of properties (default values, menu options, etc) are 
     * available before the adapter is configured.  These initial properties can be used to 
     * dynamically generate the list of configurable properties, such as when the Kinetic Filehub
     * application prepares the new Filestore display.
     */
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.DIRECTORY)
            .setIsRequired(true)
    );
    
    
    /*----------------------------------------------------------------------------------------------
     * CONFIGURATION
     *--------------------------------------------------------------------------------------------*/
    
    /**
     * Initializes the filestore adapter.  This method will be called when the properties are first
     * specified, and when the properties are updated.
     * 
     * @param propertyValues 
     */
    @Override
    public void initialize(Map<String, String> propertyValues) {
        // Ensure the directory is normalized
        if (propertyValues.containsKey(Properties.DIRECTORY)) {
            Path normalizedDirectoryPath = Paths.get(propertyValues.get(Properties.DIRECTORY)).normalize();
            propertyValues.put(Properties.DIRECTORY, normalizedDirectoryPath.toString());
        }
        // Set the configurable properties
        properties.setValues(propertyValues);

        // Prepare the root directory if it doesn't exist
        Path filestoreRootPath = Paths.get(properties.getValue(Properties.DIRECTORY));
        if (!filestoreRootPath.toFile().exists()) {
            if (filestoreRootPath.getParent() == null) {
                throw new RuntimeException(Properties.DIRECTORY+
                    " \""+properties.getValue(Properties.DIRECTORY)+"\" "+
                    "is invalid because it doesn't exist, and neither does the parent.");
            } else {
                try {
                    Files.createDirectory(filestoreRootPath);
                } catch (IOException e) {
                    throw new RuntimeException(Properties.DIRECTORY+
                        " \""+properties.getValue(Properties.DIRECTORY)+"\" "+
                        "is invalid because it doesn't exist and could not be created.", e);
                }
            }
        }
    }

    /**
     * Returns the display name for the adapter.
     * 
     * @return 
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Returns the collection of configurable properties for the adapter.
     * 
     * @return 
     */
    @Override
    public ConfigurablePropertyMap getProperties() {
        return properties;
    }
    
    
    /*----------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *--------------------------------------------------------------------------------------------*/

    @Override
    public void deleteDocument(String path) {
        // Prepare the document root path
        Path filestoreRootPath = Paths.get(properties.getValue(Properties.DIRECTORY));
        // Prepare the relative path by normalizing (removing all '.', '..', and duplicate '/' 
        // references to ensure the document roop can't be escaped)
        Path relativePath = Paths.get(path).normalize();
        // Concatenate the document root and root-relative path segments
        Path absolutePath = Paths.get(filestoreRootPath.toString(), relativePath.toString()).toAbsolutePath();
        // Ensure the root container can't be deleted
        if (absolutePath.toString().equals(filestoreRootPath.toString())) {
            throw new RuntimeException("Unable to delete root document path.");
        }
        // Delete the file
        try {
            if (Files.isRegularFile(absolutePath)) {
                Files.delete(absolutePath);
            } else {
                FileUtils.deleteDirectory(absolutePath.toFile());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete document", e);
        }
    }

    @Override
    public LocalDocument getDocument(String path) {
        // Prepare the document root path
        Path filestoreRootPath = Paths.get(properties.getValue(Properties.DIRECTORY));
        // Prepare the relative path by normalizing (removing all '.', '..', and duplicate '/' 
        // references to ensure the document roop can't be escaped)
        Path relativePath = Paths.get(path).normalize();
        // Concatenate the document root and root-relative path segments
        Path absolutePath = Paths.get(filestoreRootPath.toString(), relativePath.toString()).toAbsolutePath();
        // Return a new local document
        return new LocalDocument(filestoreRootPath, absolutePath);
    }

    @Override
    public List<LocalDocument> getDocuments(String path) throws IOException {
        // Initialize the result
        List<LocalDocument> results = new ArrayList<>();
        // Prepare the document root path
        Path filestoreRootPath = Paths.get(properties.getValue(Properties.DIRECTORY));
        // Prepare the relative path by normalizing (removing all '.', '..', and duplicate '/' 
        // references to ensure the document roop can't be escaped)
        Path relativePath = Paths.get(path).normalize();
        // Concatenate the document root and root-relative path segments
        Path absolutePath = Paths.get(filestoreRootPath.toString(), relativePath.toString()).toAbsolutePath();
        // Walk the directory looking for files
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(absolutePath)) {
            for (Path documentPath : directoryStream) {
                results.add(new LocalDocument(filestoreRootPath, documentPath));
            }
        }
        // Return the results
        return results;
    }
    
    @Override
    public String getRedirectDelegationUrl(String path, String friendlyFilename) {
        throw new RuntimeException("Redirect delegation is not supported.");
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void putDocument(String path, InputStream inputStream, String contentType) {
        // Prepare the document root path
        Path filestoreRootPath = Paths.get(properties.getValue(Properties.DIRECTORY));
        // Prepare the relative path by normalizing (removing all '.', '..', and duplicate '/' 
        // references to ensure the document roop can't be escaped)
        Path relativePath = Paths.get(path).normalize();
        // Concatenate the document root and root-relative path segments
        Path absolutePath = Paths.get(filestoreRootPath.toString(), relativePath.toString()).toAbsolutePath();
        // Write the file
        try {
            FileUtils.copyInputStreamToFile(inputStream, absolutePath.toFile());
        } catch (Exception e) {
            throw new RuntimeException("Unable to write document", e);
        }
    }

    @Override
    public boolean supportsRedirectDelegation() {
        return false;
    }

}
