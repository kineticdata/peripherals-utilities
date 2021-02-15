package com.kineticdata.bridgehub.adapter.generic.rest;

import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GenericRestTest extends BridgeAdapterTestBase{
        
    @Override
    public Class getAdapterClass() {
        return GenericRestAdapter.class;
    }
    
    @Override
    public String getConfigFilePath() {
        return "src/test/resources/bridge-config.yml";
    }
    
    @Test
    public void test_no_fields() throws Exception{
        BridgeError error = null;
        
        assertNull(error);
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setQuery("");
        
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
    }
    
    @Test
    public void test_count() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("link");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setFields(fields);
        request.setQuery("query=<%=parameter[\"Search Term\"]%>:$.result.articles");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Search Term", "internet");
        }});
        
        Count count = null;
        try {
            count = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(count.getValue() > 0);
    }
    
    @Test
    public void test_count_single() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("link");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setFields(fields);
        request.setQuery("/<%=parameter[\"Id\"]%>:$.result");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Id", "0b48fd75474321009db4b5b08b9a71c2");
        }});
        
        Count count = null;
        try {
            count = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(count.getValue() > 0);
    }
    
    @Test
    public void test_retrieve_single() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("sys_id");
        fields.add("content");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setFields(fields);
        request.setQuery("/<%=parameter[\"Id\"]%>:$.result");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Id", "0b48fd75474321009db4b5b08b9a71c2");
        }});
        
        Record record = null;
        try {
            record =  getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(record.getRecord().size() > 0);
    }
    
    @Test
    public void test_retrieve() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("link");
        fields.add("id");
        fields.add("title");
        fields.add("snippet");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setFields(fields);
        request.setQuery("query=\"<%=parameter[\"Query Term\"]%>\":$.result.articles");
        
        Map parameters = new HashMap();
        parameters.put("Query Term", "How to set");
        request.setParameters(parameters);
        
        Record record = null;
        try {
            record =  getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(record.getRecord().size() > 0);
    }
    
    @Test
    public void test_search() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("link");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setFields(fields);
        request.setQuery("query=<%=parameter[\"Search Term\"]%>:$.result.articles");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Search Term", "internet");
        }});
        
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
    }
    
    @Test
    public void test_search_sort() throws Exception{
        BridgeError error = null;
               
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("link");
        fields.add("title");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("/api/sn_km_api/knowledge/articles");
        request.setFields(fields);
        request.setQuery("query=<%=parameter[\"Search Term\"]%>:$.result.articles");
                
        request.setMetadata(new HashMap() {{
            put("order", "<%=field[\"title\"]%>:ASC");
        }});
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Search Term", "internet");
        }});
        
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
    }
}