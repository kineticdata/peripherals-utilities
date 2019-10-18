package com.kineticdata.filehub.adapters.local;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class LocalFilestoreAdapter_RootDirectoryTest {
    
    private static final Path ROOT_DIRECTORY = Paths.get("src","test","resources","test-files");
    private static final Path HAS_VALID_PARENT_DIRECTORY = Paths.get(ROOT_DIRECTORY.toString(), "AutoCreatedDirectory");
    private static final Path HAS_VALID_NORMALIZED_PARENT_DIRECTORY = Paths.get(ROOT_DIRECTORY.toString(), "NoParent", "..", "AutoCreatedDirectory");


    @After
    @Before
    public void cleanup() throws Exception {
        Files.deleteIfExists(HAS_VALID_PARENT_DIRECTORY);
        Files.deleteIfExists(HAS_VALID_NORMALIZED_PARENT_DIRECTORY);
    }
    
    /*----------------------------------------------------------------------------------------------
     * TESTS
     *--------------------------------------------------------------------------------------------*/
    
    @Test
    public void test_DirectoryExists() throws Exception {
        Exception exception = null;
        final Path directory = ROOT_DIRECTORY;

        LocalFilestoreAdapter adapter = new LocalFilestoreAdapter();
        try {
            adapter.initialize(new LinkedHashMap<String,String>(){{
                put(LocalFilestoreAdapter.Properties.DIRECTORY, ROOT_DIRECTORY.toString());
            }});
        } catch (Exception e) {
            exception = e;
        }
        
        assertNull(exception);
    }
    
    @Test
    public void test_DirectoryNotExists_ParentExists() throws Exception {
        Exception exception = null;
        final Path directory = HAS_VALID_PARENT_DIRECTORY;

        LocalFilestoreAdapter adapter = new LocalFilestoreAdapter();
        try {
            adapter.initialize(new LinkedHashMap<String,String>(){{
                put(LocalFilestoreAdapter.Properties.DIRECTORY, directory.toString());
            }});
        } catch (Exception e) {
            exception = e;
        }

        assertNull(exception);

        Path adapterDirectory = Paths.get(adapter.getProperties().getValue(LocalFilestoreAdapter.Properties.DIRECTORY));
        assert(Files.exists(adapterDirectory));
    }
    
    @Test
    public void test_DirectoryNotExists_NormalizedParentExists() throws Exception {
        Exception exception = null;
        final Path directory = HAS_VALID_NORMALIZED_PARENT_DIRECTORY;

        LocalFilestoreAdapter adapter = new LocalFilestoreAdapter();
        try {
            adapter.initialize(new LinkedHashMap<String,String>(){{
                put(LocalFilestoreAdapter.Properties.DIRECTORY, directory.toString());
            }});
        } catch (Exception e) {
            exception = e;
        }

        assertNull(exception);
        
        Path adapterDirectory = Paths.get(adapter.getProperties().getValue(LocalFilestoreAdapter.Properties.DIRECTORY));
        assert(Files.exists(adapterDirectory));
    }
    
    @Test
    public void test_DirectoryNotExists_ParentNotExists() throws Exception {
        Exception exception = null;
        final Path directory = Paths.get(ROOT_DIRECTORY.toString(), "NoParent", "AutoCreatedDirectory");

        LocalFilestoreAdapter adapter = new LocalFilestoreAdapter();
        try {
            adapter.initialize(new LinkedHashMap<String,String>(){{
                put(LocalFilestoreAdapter.Properties.DIRECTORY, directory.toString());
            }});
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);

        Path adapterDirectory = Paths.get(adapter.getProperties().getValue(LocalFilestoreAdapter.Properties.DIRECTORY));
        assert(!Files.exists(adapterDirectory));
    }
    
    @Test
    public void test_DirectoryNotExists_NormalizedParentNotExists() throws Exception {
        Exception exception = null;
        final Path directory = Paths.get(ROOT_DIRECTORY.toString(), "NoParent", "NoSub", "..", "AutoCreatedDirectory");

        LocalFilestoreAdapter adapter = new LocalFilestoreAdapter();
        try {
            adapter.initialize(new LinkedHashMap<String,String>(){{
                put(LocalFilestoreAdapter.Properties.DIRECTORY, directory.toString());
            }});
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        
        Path adapterDirectory = Paths.get(adapter.getProperties().getValue(LocalFilestoreAdapter.Properties.DIRECTORY));
        assert(!Files.exists(adapterDirectory));
    }
    
    @Test
    public void test_NormalizedNoParent() throws Exception {
        Exception exception = null;
        final Path directory = Paths.get("/", "..", "..", "AutoCreatedDirectory");

        LocalFilestoreAdapter adapter = new LocalFilestoreAdapter();
        try {
            adapter.initialize(new LinkedHashMap<String,String>(){{
                put(LocalFilestoreAdapter.Properties.DIRECTORY, directory.toString());
            }});
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        
        Path adapterDirectory = Paths.get(adapter.getProperties().getValue(LocalFilestoreAdapter.Properties.DIRECTORY));
        assert(!Files.exists(adapterDirectory));
    }
    
}
