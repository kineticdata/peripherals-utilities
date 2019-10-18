package com.kineticdata.filehub.adapters.cloud;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CloudDocumentTest {
    
    CloudDocument contentDocument;
    CloudDocument contentTxtDocument;
    CloudDocument emptyDocument;
    CloudDocument emptyTxtDocument;
    CloudDocument imageDocument;
    
    @Before
    public void beforeEach() {
        contentDocument = null;
        contentTxtDocument = null;
        emptyDocument = null;
        emptyTxtDocument = null;
        imageDocument = null;
    }
    
    /*----------------------------------------------------------------------------------------------
     * TESTS
     *--------------------------------------------------------------------------------------------*/
    
    @Test
    @Ignore("Write test")
    public void test_getContentType() throws Exception {
        assertEquals("text/plain", contentDocument.getContentType());
        assertEquals("text/plain", contentTxtDocument.getContentType());
        assertEquals("application/octet-stream", emptyDocument.getContentType());
        assertEquals("text/plain", emptyTxtDocument.getContentType());
        assertEquals("image/png", imageDocument.getContentType());
    }
    
    @Test
    @Ignore("Write test")
    public void test_getName() throws Exception {
        assertEquals("content", contentDocument.getName());
        assertEquals("content.txt", contentTxtDocument.getName());
        assertEquals("empty", emptyDocument.getName());
        assertEquals("empty.txt", emptyTxtDocument.getName());
        assertEquals("image.png", imageDocument.getName());
    }
    
    @Test
    @Ignore("Write test")
    public void test_getSizeInBytes() throws Exception {
        assertEquals((Long)22L, contentDocument.getSizeInBytes());
        assertEquals((Long)22L, contentTxtDocument.getSizeInBytes());
        assertEquals((Long)0L, emptyDocument.getSizeInBytes());
        assertEquals((Long)0L, emptyTxtDocument.getSizeInBytes());
        assertEquals((Long)2549L, imageDocument.getSizeInBytes());
    }
    
    @Test
    @Ignore("Write test")
    public void test_openStream() throws Exception {
        
    }
    
}
