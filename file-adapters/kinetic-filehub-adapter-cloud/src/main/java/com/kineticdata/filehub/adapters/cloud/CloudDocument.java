package com.kineticdata.filehub.adapters.cloud;

import com.kineticdata.filehub.adapter.Document;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.io.Payload;

public class CloudDocument implements Document {
    
    private final Blob blob;
    private final String name;
    private final String path;
    private final StorageMetadata storageMetadata;
    
    public CloudDocument(String path, Blob blob) {
        this.blob = blob;
        this.name = path.contains("/") ? path.substring(path.lastIndexOf("/")+1) : path;
        this.path = path;
        this.storageMetadata = blob.getMetadata();
    }
    
    public CloudDocument(String path, StorageMetadata storageMetadata) {
        // Remove a possible trailing / from a s3 directory
        String strippedPath = path.endsWith("/") ? path.replaceAll("/$", "") : path;
        this.blob = null;
        this.name = strippedPath.contains("/") ? strippedPath.substring(strippedPath.lastIndexOf("/")+1) : strippedPath;
        this.path = strippedPath;
        this.storageMetadata = storageMetadata;
    }

    
    /*----------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *--------------------------------------------------------------------------------------------*/
    
    @Override
    public Date getCreatedAt() {
        Date result = storageMetadata.getCreationDate();
        if (result == null) {
            result = storageMetadata.getLastModified();
        }
        return result;
    }

    @Override
    public String getContentType() {
        String result = null;
        if (isFile()) {
            BlobMetadata blobMetadata = (BlobMetadata)storageMetadata;
            result = blobMetadata.getContentMetadata().getContentType();
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Long getSizeInBytes() {
        Long result = null;
        if (isFile()) {
            BlobMetadata blobMetadata = (BlobMetadata)storageMetadata;
            result = blobMetadata.getContentMetadata().getContentLength();
        }
        return result;
    }

    @Override
    public Date getUpdatedAt() {
        Date result = storageMetadata.getLastModified();
        if (result == null) {
            result = storageMetadata.getCreationDate();
        }
        return result;
    }

    @Override
    public boolean isFile() {
        return blob != null;
    }

    @Override
    public InputStream openStream() throws FileNotFoundException {
        InputStream result = null;
        if (blob == null) {
            throw new RuntimeException("The requested document is not a file.");
        }
        Payload payload = blob.getPayload();
        if (payload == null) {
            throw new RuntimeException("The requested document payload is null.");
        }
        try {
            result = payload.openStream();
        } catch (Exception e) {
            throw new RuntimeException("Unable to open stream", e);
        }
        return result;
    }

}
