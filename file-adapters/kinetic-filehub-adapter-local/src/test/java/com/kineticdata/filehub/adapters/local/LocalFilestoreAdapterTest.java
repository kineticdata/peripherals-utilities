package com.kineticdata.filehub.adapters.local;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import org.apache.tika.io.IOUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LocalFilestoreAdapterTest {
    
    private static final String DIRECTORY = "src/test/resources/test-files";

    File tempDirectory;
    LocalFilestoreAdapter adapter;
    
    @Before
    public void beforeEach() {
        adapter = new LocalFilestoreAdapter();
        adapter.initialize(new LinkedHashMap<String,String>(){{
            put(LocalFilestoreAdapter.Properties.DIRECTORY, DIRECTORY);
        }});
    }
    
    /*----------------------------------------------------------------------------------------------
     * TESTS
     *--------------------------------------------------------------------------------------------*/

    @Test
    @Ignore("Write test")
    public void test_getDelegateUrl() throws Exception {
        
    }
    
    @Test
    public void test_getDocument() throws Exception {
        LocalDocument document = adapter.getDocument("content.txt");
        assertEquals("text/plain", document.getContentType());
        assertEquals("content.txt", document.getName());
        assertEquals((Long)22L, document.getSizeInBytes());
        assertEquals("This file has content.", IOUtils.toString(document.openStream()));
    }
    
    @Test
    public void test_getDocument_AttemptingToEscapeRoot() throws Exception {
        LocalDocument document = adapter.getDocument("//../.././A.txt");
        String expectedPath = new File(DIRECTORY).getCanonicalPath()+"/A.txt";
        assertEquals(expectedPath, document.getAbsoluteDocumentPath().toString());
    }
    
    @Test
    public void test_getDocument_FromSubdirectory() throws Exception {
        LocalDocument document = adapter.getDocument("subdirectory/subdirectory.txt");
        assertEquals("text/plain", document.getContentType());
        assertEquals("subdirectory.txt", document.getName());
        assertEquals((Long)31L, document.getSizeInBytes());
    }
    
    @Test
    public void test_getDocuments() throws Exception {
        List<LocalDocument> documents = adapter.getDocuments("/");
        assertEquals(6, documents.size());
        TreeSet<String> expectedDocumentNames = new TreeSet<String>() {{
            add("content");
            add("content.txt");
            add("empty");
            add("empty.txt");
            add("image.png");
            add("subdirectory");
        }};
        TreeSet<String> actualDocumentNames = new TreeSet<>();
        for (LocalDocument document : documents) {
            actualDocumentNames.add(document.getName());
        }
        assertEquals(expectedDocumentNames, actualDocumentNames);
    }
    
}
