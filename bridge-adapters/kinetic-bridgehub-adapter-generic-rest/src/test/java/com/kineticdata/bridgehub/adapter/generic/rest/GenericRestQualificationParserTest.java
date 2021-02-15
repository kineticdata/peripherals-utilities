package com.kineticdata.bridgehub.adapter.generic.rest;

import com.kineticdata.bridgehub.adapter.BridgeError;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class GenericRestQualificationParserTest {
    
    @Test
    public void test_getparameters_sigle_params() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        Map<String, String> parameterMap = new HashMap<>();;
        
        Map<String, String> testParameters = new HashMap<String, String>() {{
            put("fizz", "buzz");
        }};
        
        // Test one parameter
        parameterMap = helper.getParameters("fizz=buzz");
        assertTrue(parameterMap.equals(testParameters));        
    }
    

    
    @Test
    public void test_getparameters_mutiple_params() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        Map<String, String> parameterMap = new HashMap<>();;
        
        Map<String, String> testParameters = new HashMap<String, String>() {{
            put("fizz", "buzz");
            put("foo", "bar");
        }};
        
        // Test two parameters
        parameterMap = helper.getParameters("fizz=buzz&foo=bar");
        assertTrue(parameterMap.equals(testParameters));
    }

    @Test
    public void test_getparameters_empty_query() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        Map<String, String> parameterMap = new HashMap<>();;

        // Test empty query string returns empty map.
        parameterMap = helper.getParameters("");
        assertTrue(parameterMap.equals(new HashMap<String, String>()));
    }
     
    @Test
    public void test_getparameters_flag_param() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        Map<String, String> parameterMap = new HashMap<>();;
        
        Map<String, String> testParameters = new HashMap<String, String>() {{
            put("fizz", null);
        }};
                
        parameterMap = helper.getParameters("fizz");
        assertTrue(parameterMap.equals(testParameters));          
    }
    
    @Test
    public void test_getroot() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        String root; 
        root = helper.getRoot(":$.widget");
        assertTrue(root.equals("$.widget"));          
    }
    
    @Test
    public void test_getroot_param() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        String root; 
        root = helper.getRoot("foo=var:$.widget");
        assertTrue(root.equals("$.widget"));            
    }
    
    @Test
    public void test_getroot_no_root() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        String root; 
        root = helper.getRoot("foo=var");
        assertTrue(root.equals(""));   
        
        root = helper.getRoot("foo=var:");
        assertTrue(root.equals("")); 
    }
    
    @Test
    public void test_getroot_path() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        String root; 
        root = helper.getRoot("/widget_id?foo=var:$.widget");
        assertTrue(root.equals("$.widget"));     
    }
    
    @Test
    public void test_removeroot_path() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        String root; 
        root = helper.removeRoot("/widget_id?foo=var:$.widget");
        assertTrue(root.equals("/widget_id?foo=var"));     
    }
    
    @Test
    public void test_getpath() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        String root; 
        root = helper.getPath("/widget_id?foo=var:$.widget");
        assertTrue(root.equals("/widget_id"));     
    }
    
    @Test
    public void test_removepath() {
        GenericRestQualificationParser helper = new GenericRestQualificationParser();
        
        // Test one parameter with path
        String query = helper.removePath("/bazz?fizz=buzz");
        assertTrue(query.equals("fizz=buzz"));
    }
}
