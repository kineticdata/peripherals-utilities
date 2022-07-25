package com.kineticdata.bridgehub.adapter.generic.rest;

import com.kineticdata.bridgehub.adapter.QualificationParser;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

public class GenericRestQualificationParser extends QualificationParser {
    /** Defines the logger */
    protected static final org.slf4j.Logger logger 
        = LoggerFactory.getLogger(GenericRestAdapter.class);
    
    protected String getRoot (String queryString) {
        String root = null;

        String[] parts = queryString.split("[:]",2);
        if (parts.length > 1) {
            root = parts[1];
        }
        
        return root;
    }
    
    protected String removeRoot (String queryString) {
        String[] parts = queryString.split("[:]",2);
        return parts[0];
    }
    
    /**
     * This method will return a map of parameters from the qualification.
     * 
     * @param queryString
     * @return parameters
     */
    protected Map<String, String> getParameters (String queryString) {
      
        Map<String, String> parameters = new HashMap<>();

        // Return empyt map if no query was provided from reqeust.
        if (!queryString.isEmpty()) {
            // Split into individual key/values parameters by splitting on the  
            // & between each distinct query
            String[] queries = queryString.split("&(?=[^&]*?=)");
            
            
            for (String query : queries) {
                // Split the query on the = to determine the field/value key-pair. 
                // Anything before the first = is considered to be the field and 
                // anything after (including more = signs if there are any) is 
                // considered to be part of the value
                String[] str_array = query.split("=",2);
                if (str_array.length == 2) {
                    // If the parameter is in the qualification multiple times
                    // concatenate the values.
                    parameters.merge(str_array[0].trim(), str_array[1].trim(), 
                        (prev, curr) -> {
                            return String.join(",", prev, curr);
                        }
                    );
                } else if (str_array.length == 1) {
                    // This is for flag parameters.
                    parameters.put(str_array[0].trim(), null);
                } else {
                    logger.debug("%s has a parameter that was unexpected.",
                        queryString);
                }
            }
        }
        return parameters;
    }
    
    /**
     * This method gets the path extensions form the qualification.
     * 
     * @param qualificationString
     * @return path
     */
    public String getPath (String qualificationString) {
        // Split the api path from the rest of the string
        String[] parts = qualificationString.split("[?]",2);
        
        // Return the path with spaces encoded to be URL safe.
        return parts[0].replace(" ", "%20");
    }
    
    protected String removePath (String qualificationString ) {
        String query = "";
        // Split the api path from the rest of the string
        String[] parts = qualificationString.split("[?]",2);
        
        // If qualification has a query.
        if (parts.length > 1) {
            query =  parts[1];
        }
        
        return query;
    }
    
    public String encodeParameter(String name, String value) {
        return value;
    }
}
