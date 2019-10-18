package com.kineticdata.filehub.adapters.cloud;

import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.s3.AWSS3Client;
import org.jclouds.aws.s3.blobstore.options.AWSS3PutObjectOptions;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.http.HttpRequest;
import org.jclouds.io.MutableContentMetadata;
import org.jclouds.io.payloads.BaseMutableContentMetadata;
import org.jclouds.s3.domain.ObjectMetadata;
import org.jclouds.s3.domain.S3Object;

public class CloudFilestoreAmazonS3Adapter extends CloudFilestoreAdapter {
    
    /** Defines the adapter display name. */
    public static final String NAME = "Amazon s3";
    
    /** Defines the collection of property names for the adapter. */
    public static class Properties {
        public static final String ENCRYPT_FILES = "Encrypt Files";
        public static final String ACCESS_KEY = "Access Key";
        public static final String BUCKET = "Bucket";
        public static final String SECRET_ACCESS_KEY = "Secret Access Key";
        public static final String REGION = "Region";
        public static final String ROOT_FOLDER = "Root Folder";
    }
    
    /** 
     * Specifies the configurable properties for the adapter.  These are populated as part of object
     * construction so that the collection of properties (default values, menu options, etc) are 
     * available before the adapter is configured.  These initial properties can be used to 
     * dynamically generate the list of configurable properties, such as when the Kinetic Filehub
     * application prepares the new Filestore display.
     */
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.ENCRYPT_FILES).setIsRequired(true)
            .setDescription("Add AES256 Server Side Encryption to files on upload")
            .addPossibleValues("Yes","No"),
        new ConfigurableProperty(Properties.ACCESS_KEY)
            .setIsRequired(true),
        new ConfigurableProperty(Properties.SECRET_ACCESS_KEY)
            .setIsRequired(true)
            .setIsSensitive(true),
        new ConfigurableProperty(Properties.BUCKET)
            .setIsRequired(true),
        new ConfigurableProperty(Properties.REGION)
            .setIsRequired(false),
        new ConfigurableProperty(Properties.ROOT_FOLDER)
            .setIsRequired(false)
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
        // Set the configurable properties
        properties.setValues(propertyValues);
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
    protected BlobStoreContext buildBlobStoreContext() {
        java.util.Properties overrides = new java.util.Properties();
        String region = properties.getValue(Properties.REGION);
        if (region != null && !region.isEmpty()) {
            overrides.setProperty("aws-s3.endpoint","https://s3-"+region.trim()+".amazonaws.com");
        }        
        
        return ContextBuilder.newBuilder("aws-s3")
            .credentials(
                properties.getValue(Properties.ACCESS_KEY), 
                properties.getValue(Properties.SECRET_ACCESS_KEY))
            .overrides(overrides)
            .buildView(BlobStoreContext.class);
    }

    @Override
    protected String getContainer() {
        return properties.getValue(Properties.BUCKET);
    }
    
    @Override
    protected String getRootFolder() {
        return properties.getValue(Properties.ROOT_FOLDER);
    }

    @Override
    protected boolean supportsUploadMultipart() {
        return true;
    }

    @Override
    protected boolean supportsUploadStream() {
        return false;
    }
    
    // TODO: Remove this override and use the super implementation once 
    // jclouds supports adding parameters to signed requests.
    //   https://issues.apache.org/jira/browse/JCLOUDS-1016
    @Override
    public boolean supportsRedirectDelegation() {
        return false;
    }
    
    @Override
    public void putDocument(String path, InputStream inputStream, String contentType) {
        Boolean encryptionEnabled = "Yes".equals(properties.getValue(Properties.ENCRYPT_FILES));
        if (encryptionEnabled) {
            path = getFullPath(path);
            try (
                BlobStoreContext context = buildBlobStoreContext()
            ) { 
                // Obtain the S3 Client from the context
                AWSS3Client s3Client = context.unwrapApi(AWSS3Client.class);

                try {
                    // Write the file to a temorary location
                    Path tempFile = Files.createTempFile("kinetic-filehub", ".tmp");
                    IOUtils.copyLarge(inputStream, new FileOutputStream(tempFile.toFile()));

                    // Set the encryption options
                    AWSS3PutObjectOptions options = new AWSS3PutObjectOptions();
                    options.serverSideEncryption(ObjectMetadata.ServerSideEncryption.AES256);

                    // Set the content type/length metadata
                    MutableContentMetadata md = new BaseMutableContentMetadata();
                    md.setContentLength(Files.size(tempFile));
                    md.setContentType(contentType);

                    // Create the S3 Object
                    S3Object s3Object = s3Client.newS3Object();
                    s3Object.setPayload(new FileInputStream(tempFile.toFile()));
                    s3Object.getMetadata().setKey(path);
                    s3Object.getPayload().setContentMetadata(md);

                    // Put the encrypted object to s3
                    s3Client.putObject(getContainer(), s3Object, options);
                    // Delete the tempfile
                    tempFile.toFile().delete();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to stream file", e);
                }
            }
        } else {
            super.putDocument(path, inputStream, contentType);
        }
    }
    
    @Override
    public String getRedirectDelegationUrl(String path, String friendlyFilename) {
        // Declare the result
        String result;
        
        // Build the redirect delegation url
        try (
            BlobStoreContext context = buildBlobStoreContext()
        ) {
            // Build the pre signed request valid for 5 seconds
            HttpRequest request = context.getSigner().signGetBlob(
                getContainer(), path, 5);

            // Add the S3 path parameter to set the Content-Disposition header so the downloaded attachment
            // has the original file name instead of the random file name it is stored as.
            // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectGET.html#RESTObjectGET-requests
            //
            // NOTE: Apparently not yet possible (2016-01-07) to add this using jclouds:
            //   https://issues.apache.org/jira/browse/JCLOUDS-1016
//            result = request.getEndpoint().toString()
//                + "&response-content-disposition=inline;filename="
//                + UrlEscapers.urlFragmentEscaper().escape(friendlyFilename);
            
            // Set the result
            result = request.getEndpoint().toString();
        }
        // Return the result
        return result;
    }
}
