package com.kineticdata.filehub.adapters.local;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.tika.io.IOUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class LocalDocumentTest {
    
    private static final String DIRECTORY = "src/test/resources/test-files";
    
    LocalDocument contentDocument;
    LocalDocument contentTxtDocument;
    LocalDocument emptyDocument;
    LocalDocument emptyTxtDocument;
    LocalDocument imageDocument;
    
    @Before
    public void beforeEach() {
        Path filestoreRootPath = Paths.get(DIRECTORY);
        contentDocument = new LocalDocument(filestoreRootPath, Paths.get(DIRECTORY, "content"));
        contentTxtDocument = new LocalDocument(filestoreRootPath, Paths.get(DIRECTORY, "content.txt"));
        emptyDocument = new LocalDocument(filestoreRootPath, Paths.get(DIRECTORY, "empty"));
        emptyTxtDocument = new LocalDocument(filestoreRootPath, Paths.get(DIRECTORY, "empty.txt"));
        imageDocument = new LocalDocument(filestoreRootPath, Paths.get(DIRECTORY, "image.png"));
    }
    
    /*----------------------------------------------------------------------------------------------
     * TESTS
     *--------------------------------------------------------------------------------------------*/
    
    @Test
    public void test_getContentType() throws Exception {
        assertEquals("text/plain", contentDocument.getContentType());
        assertEquals("text/plain", contentTxtDocument.getContentType());
        assertEquals("application/octet-stream", emptyDocument.getContentType());
        assertEquals("text/plain", emptyTxtDocument.getContentType());
        assertEquals("image/png", imageDocument.getContentType());
    }
    
    @Test
    public void test_getName() throws Exception {
        assertEquals("content", contentDocument.getName());
        assertEquals("content.txt", contentTxtDocument.getName());
        assertEquals("empty", emptyDocument.getName());
        assertEquals("empty.txt", emptyTxtDocument.getName());
        assertEquals("image.png", imageDocument.getName());
    }
    
    @Test
    public void test_getSizeInBytes() throws Exception {
        assertEquals((Long)22L, contentDocument.getSizeInBytes());
        assertEquals((Long)22L, contentTxtDocument.getSizeInBytes());
        assertEquals((Long)0L, emptyDocument.getSizeInBytes());
        assertEquals((Long)0L, emptyTxtDocument.getSizeInBytes());
        assertEquals((Long)2549L, imageDocument.getSizeInBytes());
    }
    
    @Test
    public void test_openStream() throws Exception {
        assertEquals("This file has content.", IOUtils.toString(contentDocument.openStream()));
        assertEquals("This file has content.", IOUtils.toString(contentTxtDocument.openStream()));
        assertEquals("", IOUtils.toString(emptyDocument.openStream()));
        assertEquals("", IOUtils.toString(emptyTxtDocument.openStream()));
        assertTrue(IOUtils.contentEquals(new FileInputStream(DIRECTORY+"/image.png"), 
            imageDocument.openStream()));
    }
    
}
