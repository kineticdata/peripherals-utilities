package com.kineticdata.bridgehub.adapter.generic.rest;

import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.Record;
import java.util.ArrayList;
import java.util.List;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author chadrehm
 */
public class GenericRest_HelperMethodTest_XML {
    private String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:atom=\"http://www.w3.org/2005/Atom\">"
    + "<channel>"
    + "<item><guid>123</guid><title>Title One</title><link>/link/one.html</link></item>"
    + "<item><guid>456</guid><title>Title Two</title><link>/link/two.html</link></item>"
    + "</channel>"
    + "</rss>";
    
    // This tests checks that an XML response is correctly parsed into a JSON object
    @Test
    public void test_parseresponse_xml() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        
        Object jsonXML = null;
        Exception error = null;
        try {
            jsonXML = apiHelper.parseResponse(xmlString);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);   
        assertTrue(jsonXML instanceof JSONObject);
        if (jsonXML != null) {
            assertTrue(((JSONObject)jsonXML).get("rss") != null);
        }
    }
    
    // This tests checks that a JSON object with no root returns a JSON array with
    // the object in it. 
    @Test
    public void test_parseresults_object_wo_root() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        GenericRestAdapter helper = new GenericRestAdapter();
        
        Object jsonXML = apiHelper.parseResponse(xmlString);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonXML, null);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);   
        assertTrue(jsonArray.get(0).equals(((JSONObject)jsonXML)));
    }
    
    @Test
    public void test_parseresults_object_w_root() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        GenericRestAdapter helper = new GenericRestAdapter();
        
        Object jsonXML = apiHelper.parseResponse(xmlString);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonXML, "$.rss");
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);   
        assertTrue(jsonArray.size() == 1);
        assertTrue(((JSONObject)jsonArray.get(0)).get("channel") != null);
    }
    
    @Test
    public void test_parseresults_object_w_nested_root() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        GenericRestAdapter helper = new GenericRestAdapter();
        
        Object jsonXML = apiHelper.parseResponse(xmlString);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonXML, "$.rss.channel.item");
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);   
        assertTrue(jsonArray.size() > 1);
        assertTrue(((JSONObject)jsonArray.get(0)).get("guid") != null);
    }
        
    // This tests checks that a JSON object with a root returns a JSON array with
    // the object in it. 
    @Test
    public void test_parseresults_object_error() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        GenericRestAdapter helper = new GenericRestAdapter();
        
        Object jsonXML = apiHelper.parseResponse(xmlString);
        
        BridgeError error = null;
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = helper.parseResults(jsonXML, "$.nonexistant");
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNotNull(error);   
    }

    @Test
    public void test_build_record() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        GenericRestAdapter helper = new GenericRestAdapter();
        
        List<String> fields = new ArrayList();
        fields.add("guid");
        fields.add("title");

        Object jsonXML = apiHelper.parseResponse(xmlString);
        JSONArray jsonArray = helper.parseResults(jsonXML, "$.rss.channel.item");
        
        JsonPathException error = null;
        Record record = new Record();
        try {
            record = helper.buildRecord(fields, (JSONObject)jsonArray.get(0));
        } catch (JsonPathException e) {
            error = e;
        }
        
        // Set up control object for comparison of equality.
        JSONObject recordControl = new JSONObject();
        recordControl.put("guid", 123);
        recordControl.put("title", "Title One");
        
        assertNull(error);        
        assertTrue(record.getRecord().equals(recordControl));
    }
    
    @Test
    public void test_build_record_error() throws Exception{
        GenericRestApiHelper apiHelper = new GenericRestApiHelper("", "", "", "XML");
        GenericRestAdapter helper = new GenericRestAdapter();
        
        List<String> fields = new ArrayList();
        fields.add("guid");
        fields.add("$.description");

        Object jsonXML = apiHelper.parseResponse(xmlString);
        JSONArray jsonArray = helper.parseResults(jsonXML, "$.rss.channel.item");
        
        JsonPathException error = null;
        Record record = new Record();
        try {
            record = helper.buildRecord(fields, (JSONObject)jsonArray.get(0));
        } catch (JsonPathException e) {
            error = e;
        }
        
        assertNotNull(error);
    }
}