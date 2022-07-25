package com.kineticdata.bridgehub.adapter.generic.rest;

import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.Record;
import java.util.ArrayList;
import java.util.List;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author chadrehm
 */
public class GenericRest_HelperMethodTest_JSON {
    private String jsonString = "{\"First Name\": \"Foo\", \"Hourly Rate\": "
    + "{\"decimal\": 0.00,\"currency\":"
    + " \"USD\", \"conversionDate\": \"1970-01-01T00:00:00.000+0000\","
    + " \"functionalValues\": { \"USD\": 0.00, \"GBP\": null, \"EUR\": null,"
    + " \"JPY\": null,\"CAD\": null }}}";
    
    private String jsonString2 = "{\"First Name\": \"Foo\", \"Hourly Rate\": "
    + "{\"decimal\": 0.00,\"currency\":"
    + " \"USD\", \"conversionDate\": \"1970-01-01T00:00:00.000+0000\","
    + " \"functionalValues\": { \"USD\": 0.00, \"GBP\": null, \"EUR\": null,"
    + " \"JPY\": null,\"CAD\": null }}}";

    @Test
    public void test_build_record() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        List<String> fields = new ArrayList();
        fields.add("$['Hourly Rate'].currency");
        fields.add("First Name");

        JSONObject jsonobj = (JSONObject)JSONValue.parse(jsonString);
        
        JsonPathException error = null;
        Record record = new Record();
        try {
            record = helper.buildRecord(fields, jsonobj);
        } catch (JsonPathException e) {
            error = e;
        }
        
        // Set up control object for comparison of equality.
        JSONObject recordControl = new JSONObject();
        recordControl.put("$['Hourly Rate'].currency", "USD");
        recordControl.put("First Name", "Foo");
        
        assertNull(error);        
        assertTrue(record.getRecord().equals(recordControl));
    }
    
    @Test
    public void test_build_record_error() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        List<String> fields = new ArrayList();
        fields.add("$['Hourly Rate'].currency");
        fields.add("First Name");

        JSONObject jsonobj = (JSONObject)JSONValue.parse(jsonString);
        
        Record record = helper.buildRecord(fields, jsonobj);
        
        fields.add("$[Hourly Rate].currenty");
        
        JsonPathException error = null;
        try {
            helper.buildRecord(fields, jsonobj);
        } catch (JsonPathException e) {
            error = e;
        }
        
        assertNotNull(error);
    }
    
    // This tests if an array of JSON is returned correctly. 
    @Test
    public void test_parseresults_array() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        String jsonStringArray = String.format("[%s,%s]", jsonString, jsonString2);
        Object jsonval = JSONValue.parse(jsonStringArray);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonval, null);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);        
        assertTrue(jsonArray.equals(((JSONArray)jsonval)));
    }
    
    // This tests if the method recieves a JSON array and root that is not null 
    // the method errors. 
    @Test
    public void test_parseresults_array_error() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        String jsonStringArray = String.format("[%s,%s]", jsonString, jsonString2);
        Object jsonval = JSONValue.parse(jsonStringArray);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonval, "$");
        } catch (BridgeError e) {
            error = e;
        }

        assertNotNull(error);        
    }
    
    // This tests checks that a JSON object with no root returns a JSON array with
    // the object in it. 
    @Test
    public void test_parseresults_object_wo_root() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        Object jsonval = JSONValue.parse(jsonString);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonval, null);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);   
        assertTrue(jsonArray.get(0).equals(((JSONObject)jsonval)));
    }
    
    // This tests checks that a JSON object with a root returns a JSON array with
    // the object in it. 
    @Test
    public void test_parseresults_object_w_root() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        String jsonStringObject = String.format("{widget:[%s,%s]}", jsonString, jsonString2);
        Object jsonval = JSONValue.parse(jsonStringObject);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonval, "$.widget");
        } catch (BridgeError e) {
            error = e;
        }
        
        // Set up control
        String jsonStringArray = String.format("[%s,%s]", jsonString, jsonString2);
        Object control = JSONValue.parse(jsonStringArray);
        
        assertNull(error);   
        assertTrue(jsonArray.equals(((JSONArray)control)));
    }
    
    // This tests checks that a JSON object with a root returns a JSON array with
    // the object in it. 
    @Test
    public void test_parseresults_object_error() throws Exception{
        GenericRestAdapter helper = new GenericRestAdapter();
        
        String jsonStringObject = String.format("{widget:[%s,%s]}", jsonString, jsonString2);
        Object jsonval = JSONValue.parse(jsonStringObject);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonval, "$.widgetx");
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNotNull(error);   
    }
}
