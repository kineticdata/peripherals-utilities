package com.kineticdata.bridgehub.adapter.generic.rest;

import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GenericRestTest_JSON extends BridgeAdapterTestBase{
        
    @Override
    public Class getAdapterClass() {
        return GenericRestAdapter.class;
    }
    
    @Override
    public String getConfigFilePath() {
        return "src/test/resources/bridge-config-json.yml";
    }
    
    @Test
    @Override
    public void test_invalidStructure() {
        BridgeError error = null;
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("NonexistantStructure");
        request.setQuery("");
        
        try {
            getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNotNull(error);
    }
    
    @Test
    public void test_singleRetrieveWithParams() throws Exception {
        // Create the Bridge Request
        BridgeRequest request = new BridgeRequest();
        request.setStructure(getStructure());
        request.setFields(getFields());
        request.setQuery("/<%=parameter[\"Id\"]%>");
        request.setParameters(new HashMap<String, String>() {{ 
            put("Id", "1");
        }});
        
        Record record = getAdapter().retrieve(request);
        Map<String,Object> recordMap = record.getRecord();
        
        assertNotNull(recordMap);
    }
    
    @Test
    public void test_singleRetrieve_NestedJSONPath() throws Exception {
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("city");
        fields.add("zipcode");
        BridgeRequest request = new BridgeRequest();
        request.setStructure(getStructure());
        request.setFields(fields);
        request.setQuery("/1:$.address");
        
        Record record = getAdapter().retrieve(request);
        Map<String,Object> recordMap = record.getRecord();
        
        assertNotNull(recordMap);
    }
    
          
    @Test
    @Override
    public void test_order(){
        BridgeError error = null;
               
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("username");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure(getStructure());
        request.setFields(getFields());
        request.setQuery("");
                
        request.setMetadata(new HashMap() {{
            put("order", "<%=field[\"id\"]%>:DESC");
        }});
                
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(
            (Integer)records.getRecords().get(0).getValue("id") 
            > (Integer)records.getRecords().get(1).getValue("id")
        );
    }
}