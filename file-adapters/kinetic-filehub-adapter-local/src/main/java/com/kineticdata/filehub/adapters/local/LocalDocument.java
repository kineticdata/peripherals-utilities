package com.kineticdata.filehub.adapters.local;

import com.kineticdata.filehub.adapter.Document;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalDocument implements Document {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDocument.class);
    
    private static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";
    
    private BasicFileAttributes attributes;
    private final Path absoluteDocumentPath;
    private final Path filestoreRootPath;

    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTORS
     *--------------------------------------------------------------------------------------------*/
    
    public LocalDocument(Path filestoreRootPath, Path absoluteDocumentPath) {
        this.absoluteDocumentPath = absoluteDocumentPath;
        this.filestoreRootPath = filestoreRootPath;
    }
    

    /*----------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *--------------------------------------------------------------------------------------------*/
    
    @Override
    public String getContentType() {
        String result;
        try {
            Tika tika = new Tika();
            result = tika.detect(absoluteDocumentPath.toFile());
        } catch (IOException e) {
            LOGGER.trace("Unable to determine file content type.", e);
            result = UNKNOWN_CONTENT_TYPE;
        }
        return result;
    }

    @Override
    public Date getCreatedAt() {
        return new Date(getAttributes().creationTime().toMillis());
    }

    @Override
    public String getName() {
        return absoluteDocumentPath.getFileName().toString();
    }

    @Override
    public String getPath() {
        return absoluteDocumentPath.toString().substring(filestoreRootPath.toString().length()+1).replaceAll("\\\\","/");
    }

    @Override
    public Long getSizeInBytes() {
        return absoluteDocumentPath.toFile().length();
    }

    @Override
    public Date getUpdatedAt() {
        return new Date(getAttributes().lastModifiedTime().toMillis());
    }

    @Override
    public boolean isFile() {
        return getAttributes().isRegularFile();
    }

    @Override
    public InputStream openStream() throws FileNotFoundException {
        return new FileInputStream(absoluteDocumentPath.toString());
    }
    
    
    /*----------------------------------------------------------------------------------------------
     * METHODS
     *--------------------------------------------------------------------------------------------*/
    
    public BasicFileAttributes getAttributes() {
        if (attributes == null) {
            try {
                attributes = Files.readAttributes(absoluteDocumentPath, BasicFileAttributes.class);
            } catch (Exception e) {
                throw new RuntimeException("Unable to retrieve created at.", e);
            }
        }
        return attributes;
    }
    
    public Path getAbsoluteDocumentPath() {
        return this.absoluteDocumentPath;
    }

}
