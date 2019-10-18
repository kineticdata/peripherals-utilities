package com.kineticdata.filehub.adapters.cloud;

import com.kineticdata.filehub.adapter.Document;
import com.kineticdata.filehub.adapter.FilestoreAdapter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import static org.jclouds.blobstore.options.ListContainerOptions.Builder.inDirectory;
import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;
import org.jclouds.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CloudFilestoreAdapter implements FilestoreAdapter {
    
    /** Static class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFilestoreAdapter.class);

    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try (
            InputStream inputStream = CloudFilestoreAdapter.class.getResourceAsStream(
                "/"+CloudFilestoreAdapter.class.getName()+".version")
        ) {
            java.util.Properties properties = new java.util.Properties();
            properties.load(inputStream);
            VERSION = properties.getProperty("version")+"-"+properties.getProperty("build");
        } catch (IOException e) {
            LOGGER.warn("Unable to load "+CloudFilestoreAdapter.class.getName()+" version properties.", e);
            VERSION = "Unknown";
        }
    }
    
    /*----------------------------------------------------------------------------------------------
     * ABSTRACT METHODS
     *--------------------------------------------------------------------------------------------*/
    
    protected abstract BlobStoreContext buildBlobStoreContext();
    
    protected abstract String getContainer();
    
    protected abstract boolean supportsUploadMultipart();
    
    protected abstract boolean supportsUploadStream();
    
    
    /*----------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *--------------------------------------------------------------------------------------------*/
    
    @Override
    public void deleteDocument(String path) {
        path = getFullPath(path);
        try (
            BlobStoreContext context = buildBlobStoreContext()
        ) {
            // Obtain a reference to the blobstore
            BlobStore blobStore = context.getBlobStore();
            // Delete the record
            // TODO: This is broken; how to check if the path is a directory or file?
            if (path == null) {
                blobStore.deleteDirectory(getContainer(), path);
            } else {
                blobStore.removeBlob(getContainer(), path);
            }
        }
    }
    
    @Override
    public Document getDocument(String path) {
//        path = getFullPath(path);
        // Declare the result
        Document result;
        // Try to read from the jCloud BlobStoreContext
        try (
            BlobStoreContext context = buildBlobStoreContext()
        ) {
            // Obtain a reference to the blobstore
            BlobStore blobStore = context.getBlobStore();
            // Obtain a reference to the file
            Blob blob = blobStore.getBlob(getContainer(), getFullPath(path));
            // Prepare the cloud document
            result = new CloudDocument(path, blob);
        }
        // Return the result
        return result;
    }

    @Override
    public List<? extends Document> getDocuments(String path) throws IOException {
        path = getFullPath(path);
        // Declare the results
        List<CloudDocument> results = new ArrayList<>();
        // Try to read from the jCloud BlobStoreContext
        try (
            BlobStoreContext context = buildBlobStoreContext()
        ) {
            // Obtain a reference to the blobstore
            BlobStore blobStore = context.getBlobStore();
            // Get the list of metadatas
            PageSet<? extends StorageMetadata> metadatas;
            if (getContainer() == null) {
                metadatas = blobStore.list();
            } else if (path == null || "".equals(path)) {
                metadatas = blobStore.list(getContainer());
            } else {
                metadatas = blobStore.list(getContainer(), inDirectory(path));
            }
            // For each of the metadatas
            for (StorageMetadata metadata : metadatas) {
                // If the adapter is using a root folder, pull it off from the front of the metadata name
                String metadataName = metadata.getName();
                if (!getRootFolder().isEmpty()) metadataName = metadataName.replace(getRootFolder()+"/", "");
                // If the record is for a file
                if (BlobMetadata.class.isAssignableFrom(metadata.getClass())) {
                    Blob blob = blobStore.getBlob(getContainer(), metadata.getName());
                    // If the blob still exists (could have been removed between getting the list of
                    // files and retrieving the blob itself)
                    if (blob != null) {
                        results.add(new CloudDocument(metadataName, blob));
                    }
                }
                // If the record is a container
                else if (StorageType.CONTAINER.equals(metadata.getType())) {
                    results.add(new CloudDocument(metadataName, metadata));
                }
                // If the record is a container
                else {
                    results.add(new CloudDocument(metadataName, metadata));
                }
            }
        }
        // Return the results
        return results;
    }

    @Override
    public void putDocument(String path, InputStream inputStream, String contentType) {
        path = getFullPath(path);
        try (
            BlobStoreContext context = buildBlobStoreContext()
        ) {
            // Obtain a reference to the blobstore
            BlobStore blobStore = context.getBlobStore();

            // If the cloud implementation supports uploading as a stream
            if (supportsUploadStream()) {
                // Upload a file
                Blob blob = blobStore.blobBuilder(path)
                    .payload(inputStream)
                    .contentType(contentType)
                    .build();
                blobStore.putBlob(getContainer(), blob, multipart(supportsUploadMultipart()));
            }
            // If the cloud implementation does not support uploading as a stream
            else {
                try {
                    // Write the file to a temorary location
                    Path tempFile = Files.createTempFile("kinetic-filehub", ".tmp");
                    IOUtils.copyLarge(inputStream, new FileOutputStream(tempFile.toFile()));
                    // Upload a file
                    Blob blob = blobStore.blobBuilder(path)
                        .payload(new FileInputStream(tempFile.toFile()))
                        .contentLength(Files.size(tempFile))
                        .contentType(contentType)
                        .build();
                    blobStore.putBlob(getContainer(), blob, multipart(supportsUploadMultipart()));
                    // Delete the tempfile
                    tempFile.toFile().delete();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to stream file", e);
                }
            }
        }
    }

    @Override
    public String getRedirectDelegationUrl(String path, String friendlyFilename) {
        path = getFullPath(path);
        // Declare the result
        String result;
        // Build the redirect delegation url
        try (
            BlobStoreContext context = buildBlobStoreContext()
        ) {
            // Build the pre signed request valid for 5 seconds
            HttpRequest request = context.getSigner().signGetBlob(
                getContainer(), path, 5);
            // Set the result
            result = request.getEndpoint().toString();
        }
        // Return the result
        return result;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public boolean supportsRedirectDelegation() {
        return true;
    }
    
    protected String getRootFolder() {
        return "";
    }
    
    protected String getFullPath(String path) {
        if (!getRootFolder().isEmpty()) {
            return getRootFolder()+"/"+path;
        } else {
            return path;
        }
    }

}
