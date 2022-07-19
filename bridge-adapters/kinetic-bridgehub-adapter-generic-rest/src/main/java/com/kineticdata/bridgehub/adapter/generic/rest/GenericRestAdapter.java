package com.kineticdata.bridgehub.adapter.generic.rest;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.BridgeUtils;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericRestAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    public GenericRestAdapter () {
        // Parse the query and exchange out any parameters with their parameter 
        // values. ie. change the query username=<%=parameter["Username"]%> to
        // username=test.user where parameter["Username"]=test.user
        this.parser = new GenericRestQualificationParser();
    }

    
    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/

    /** Defines the adapter display name */
    public static final String NAME = "Generic Rest Bridge";

    /** Defines the logger */
    protected static final Logger LOGGER = LoggerFactory.getLogger(GenericRestAdapter.class);
    
    /** Adapter version constant. */
    public static String VERSION = "";
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties.load(GenericRestAdapter.class.getResourceAsStream("/"+GenericRestAdapter.class.getName()+".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            LOGGER.warn("Unable to load "+GenericRestAdapter.class.getName()+" version properties.", e);
            VERSION = "Unknown";
        }
    }

    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String PROPERTY_USERNAME = "Username";
        public static final String PROPERTY_PASSWORD = "Password";
        public static final String PROPERTY_ORIGIN = "URL Origin";
        public static final String PROPERTY_CONTENT_TYPE = "Content Type";

    }

    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.PROPERTY_USERNAME),
        new ConfigurableProperty(Properties.PROPERTY_PASSWORD).setIsSensitive(true),
        new ConfigurableProperty(Properties.PROPERTY_ORIGIN).setIsRequired(true)
            .setDescription("The scheme://hostname:port"),
        new ConfigurableProperty(Properties.PROPERTY_CONTENT_TYPE)
            .setPossibleValues("JSON", "XML")
            .setValue("JSON")
            .setDescription("The content type of the response data")
    );

    // Local variables to store the property values in
    private String username;
    private String password;
    private String origin;
    private String contentType;
    private GenericRestQualificationParser parser;
    private GenericRestApiHelper apiHelper;
    
    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public void initialize() throws BridgeError {
        // Initializing the variables with the property values that were passed
        // when creating the bridge so that they are easier to use
        username = properties.getValue(Properties.PROPERTY_USERNAME);
        password = properties.getValue(Properties.PROPERTY_PASSWORD);
        origin = properties.getValue(Properties.PROPERTY_ORIGIN);
        contentType = properties.getValue(Properties.PROPERTY_CONTENT_TYPE);
        
        apiHelper = new GenericRestApiHelper(origin, username, password, contentType);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
       return VERSION;
    }

    @Override
    public void setProperties(Map<String,String> parameters) {
        // This should always be the same unless there are special circumstances
        // for changing it
        properties.setValues(parameters);
    }

    @Override
    public ConfigurablePropertyMap getProperties() {
        // This should always be the same unless there are special circumstances
        // for changing it
        return properties;
    }

    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public Count count(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Counting records");
        LOGGER.trace("  Structure: " + request.getStructure());
        LOGGER.trace("  Query: " + request.getQuery());
        
        // Call common function for all BridgeAdpater methods. The method manages:
        // parameter replacment for qualification, removing the json root, splitting
        // the partial path from query, building partial url and calling the api
        // helper.
        JSONArray jsonArray = getResultsArray(request);

        // Create and return a count object that contains the count
        return new Count(jsonArray.size());
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Retrieving Generic Rest Record");
        LOGGER.trace("  Structure: " + request.getStructure());
        LOGGER.trace("  Query: " + request.getQuery());
        LOGGER.trace("  Fields: " + request.getFieldString());

        // Call common function for all BridgeAdpater methods. The method manages:
        // parameter replacment for qualification, removing the json root, splitting
        // the partial path from query, building partial url and calling the api
        // helper.
        JSONArray jsonArray = getResultsArray(request);
        
        // Build record from response object
        Record record = new Record();
        if (jsonArray != null && jsonArray.size() > 0) {
            // Throw error if multiple results found.
            if (jsonArray.size() > 1) {
                throw new BridgeError ("Retrieve must return a single result."
                    + " Multiple results found.");
            }
            
            // Get fields to be used when building the record
            JSONObject obj = (JSONObject)jsonArray.get(0);  
            List<String> fields = getFields(request.getFields() == null ? 
                new ArrayList() : request.getFields(), obj);
            
            // buildRecord will use JSON Path to get nested values if a field
            // provided a path to the nested value.
            record = buildRecord(fields, obj); 
        }

        // Return the created Record object
        return record;
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Searching Records");
        LOGGER.trace("  Structure: " + request.getStructure());
        LOGGER.trace("  Query: " + request.getQuery());
        LOGGER.trace("  Fields: " + request.getFieldString());

        // Call common function for all BridgeAdpater methods. The method manages:
        // parameter replacment for qualification, removing the json root, splitting
        // the partial path from query, building partial url and calling the api
        // helper.
        JSONArray jsonArray = getResultsArray(request);
        
        // Build list of records from resonse to the source system
        List<Record> recordList = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        if(jsonArray.isEmpty() != true){

            // Use first object in response array to get fields to be used when 
            // building the record.
            JSONObject obj = (JSONObject)jsonArray.get(0);  
            fields = getFields(request.getFields() == null ? 
                new ArrayList() : request.getFields(), obj);
            
            // Iterate through the response objects and make a new Record for each.
            for (Object o : jsonArray) {
                obj = (JSONObject)o;
                
                Record record = new Record();
                if (obj != null) {
                    record = buildRecord(fields, obj);
                }
                
                // Add the created record to the list of records
                recordList.add(record);
            } 
        }
        
        // Sort records
        LinkedHashMap<String,String> sortOrderItems = null; 
        // Adapter side sorting requires an order be set by request
        if (request.getMetadata("order") != null) {
            sortOrderItems = getSortOrderItems(
                BridgeUtils.parseOrder(request.getMetadata("order")));
            
            GenericRestComparator comparator =
                new GenericRestComparator(sortOrderItems);
            Collections.sort(recordList, comparator);
        }
        
        
        
        // Return the RecordList object
        return new RecordList(fields, recordList);
    }

    /*----------------------------------------------------------------------------------------------
     * HELPER METHODS
     *--------------------------------------------------------------------------------------------*/
    protected JSONArray getResultsArray(BridgeRequest request) throws BridgeError{
        // This will be a partial path.
        String path = request.getStructure().trim();
        
        // The qualification can contain a pratial path and JSON root.
        // Replace parameter placeholders with their values.
        String qualification = parser.parse(request.getQuery(),request.getParameters());
        String root = parser.getRoot(qualification);
        // The JSON root isn't required to be part of the qualification anymore
        qualification = parser.removeRoot(qualification);
        
        // If qualification has a partial path it will start with '/'
        if (!qualification.isEmpty() && qualification.charAt(0) == '/'){
            path += parser.getPath(qualification);
            qualification = parser.removePath(qualification);
        }
        
        // Convert parameters from the request into a map of parameter names to 
        // values.
        Map<String, String> parameters = parser.getParameters(qualification);
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        Object object = apiHelper.executeRequest(getUrl(path, parameterMap));

        return parseResults(object, root);
    }
    
    /**
     * Return a list of fields property names. If no fields were provided in the  
     * bridge request then return a list of all properties on the response object.
     * 
     * @param fields
     * @param jsonobj
     * @return fields
     */
    protected List<String> getFields(List<String> fields, JSONObject jsonobj) {
        if(fields.isEmpty()){
            fields.addAll(jsonobj.keySet());
        }
        return fields;
    }
    
    /**
     * Build a Record.If no fields are provided all fields will be returned.
     * 
     * @param fields
     * @param jsonobj
     * @return record
     * @throws com.kineticdata.bridgehub.adapter.BridgeError
     */
    protected Record buildRecord (List<String> fields, JSONObject jsonobj) 
        throws JsonPathException {
        
        JSONObject obj = new JSONObject();
        DocumentContext jsonContext = JsonPath.parse(jsonobj); 
        
        fields.stream().forEach(field -> {
            // either use JsonPath or just add the field value.  We're assuming
            // all JsonPath usages will begin with $[ or $.. 
            if (field.startsWith("$.") || field.startsWith("$[")) {
                try {
                    obj.put(field, jsonContext.read(field));
                } catch (JsonPathException e) {
                    
                    throw new JsonPathException(String.format("There was an issue"
                        + " reading the JSON Path for field '%s'", field), e);
                }
            } else {
                obj.put(field, jsonobj.get(field));
            }
        });
        
        Record record = new Record(obj, fields);
        return record;
    }

    /**
     * returns a Map of keys that are the parameter name as a key and a NameValuePair
     * for a value.
     * 
     * @param parameters
     * @return 
     */
    protected Map<String, NameValuePair> buildNameValuePairMap(Map<String, String> parameters) {
        Map<String, NameValuePair> parameterMap = new HashMap<>();

        parameters.forEach((key, value) -> {
            parameterMap.put(key, new BasicNameValuePair(key, value));
        });

        return parameterMap;
    }
    
    /**
     * Returns the url as a string.  The url is just the path and query, the base
     * url is attached in the request.
     * 
     * @param path
     * @param parameters
     * @return 
     */
    protected String getUrl (String path, Map<String, NameValuePair> parameters) {
        return String.format("%s?%s", path, 
            URLEncodedUtils.format(parameters.values(), Charset.forName("UTF-8")));
    }
    
    /**
     * The results of parseOrder does not allow for a structure that 
     * guarantees order.  The method is required to preserver order.
     * 
     * @param uncastSortOrderItems
     * @return
     * @throws IllegalArgumentException 
     */
    private LinkedHashMap<String, String> 
        getSortOrderItems (Map<String, String> uncastSortOrderItems)
        throws IllegalArgumentException{

        if (!(uncastSortOrderItems instanceof LinkedHashMap)) {
            throw new IllegalArgumentException("An unexpected error occured");
        }
        
        return (LinkedHashMap)uncastSortOrderItems;
    }
    
    /**
     * This method takes a Object that is inherently a JSONValue that could be
     * type JSONObject or JSONArray.  This method always returns an array of 
     * JSONArray to normalize the retrieve and search methods behaviors.
     * 
     * @param result
     * @param root
     * @return
     * @throws BridgeError 
     */    
     // The JSON path root that was provided in the qualification mapping is applied
     // in this method.
    protected JSONArray parseResults(Object result, String root) throws BridgeError{
        JSONArray jsonArray = new JSONArray();
        
        if (result instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject)result;
            
            if (root != null) { 
                Object jsonRootObj;
                try {
                    DocumentContext jsonContext = JsonPath.parse(jsonObj);
                    jsonRootObj = jsonContext.read(root);
                } catch (JsonPathException e) {
                    // If contentType is XML, do additional checks since empty XML structures will break JSONPath
                    if ("XML".equals(contentType)) {
                        // Get root without last leaf and parse again to check if parent of root exists
                        String subRoot = root.substring(0, root.lastIndexOf("."));
                        try {
                            DocumentContext jsonContext = JsonPath.parse(jsonObj);
                            jsonRootObj = jsonContext.read(subRoot);
                            
                            // If subRoot is an object, null, or an empty string, return an empty array
                            // because XML might be missing the last leaf when it's empty
                            if (
                                jsonRootObj instanceof JSONObject 
                                || jsonRootObj == null 
                                || (jsonRootObj instanceof String && ((String)jsonRootObj).isEmpty())
                            ) {
                                jsonRootObj = new JSONArray();
                            } else {
                                // Otherwise throw error
                                throw new BridgeError("An issue occured when applying JSON path to the parsed XML."
                                    + " Please check the root path in the qualification mapping.", e);
                            }
                        } catch (JsonPathException e2) {
                            throw new BridgeError("An issue occured when applying JSON path to the parsed XML."
                                + " Please check the root path in the qualification mapping.", e);
                        }
                    } else {
                        throw new BridgeError("An issue occured when applying JSON path."
                            + " Please check the root path in the qualification mapping.", e);
                    }
                }
                jsonArray = parseResults(jsonRootObj, null);
            } else {
                jsonArray.add(jsonObj);
            }
        } else if (result instanceof JSONArray) {
            if (root != null) {
                throw new BridgeError("The Generic Rest adapter does not support" +
                    " a root accessor when an array type has been returned");
            }
            jsonArray = (JSONArray)result;
        } else {
            throw new BridgeError("The Generic Rest adapter was expecting" +
                " a return type of JSON object or array.");
        }

        return jsonArray;
    }
}
