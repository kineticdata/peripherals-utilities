/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kineticdata.bridgehub.adapter.generic.rest;

import com.kineticdata.bridgehub.adapter.BridgeError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.XML;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class GenericRestApiHelper {
    private static final Logger LOGGER = 
        LoggerFactory.getLogger(GenericRestApiHelper.class);
    
    private final String origin;
    private final String username;
    private final String password;
    private final String contentType;
    
    public GenericRestApiHelper(String origin, String username, String password, String contentType) {
        this.origin = origin;
        this.username = username;
        this.password = password;
        this.contentType = contentType == null || contentType.isEmpty() ? "JSON" : contentType;
    } 
    
    public Object executeRequest (String partialUrl) throws BridgeError{
        
        Object output;      
        // System time used to measure the request/response time
        long start = System.currentTimeMillis();
        
        try (
            CloseableHttpClient client = HttpClients.createDefault()
        ) {
            HttpResponse response;
            HttpGet get = new HttpGet(origin + partialUrl);

            get.setHeader("Authorization", getAuthHeader());
            if ("XML".equals(contentType)) {
                get.setHeader("Content-Type", "application/xml");
                get.setHeader("Accept", "application/xml");
            } else {
                get.setHeader("Content-Type", "application/json");
                get.setHeader("Accept", "application/json");
            }
            
            response = client.execute(get);
            LOGGER.debug("Recieved response from \"{}\" in {}ms.",
                partialUrl,
                System.currentTimeMillis()-start);

            int responseCode = response.getStatusLine().getStatusCode();
            LOGGER.trace("Request response code: " + responseCode);
            
            HttpEntity entity = response.getEntity();
            // Confirm that response is a JSON object
            output = parseResponse(EntityUtils.toString(entity));
            
            // Handle all other faild repsonses
            if (responseCode >= 400) {
                handleFailedRequest(responseCode);
            } 
        }
        catch (IOException e) {
            throw new BridgeError("Unable to make a connection to the REST"
                + " Service", e);
        }
        
        return output;
    }
    
    private void handleFailedRequest (int responseCode) throws BridgeError {
        switch (responseCode) {
            case 400:
                throw new BridgeError("400: Bad Reqeust");
            case 401:
                throw new BridgeError("401: Unauthorized");
            case 404:
                throw new BridgeError("404: Page not found");
            case 405:
                throw new BridgeError("405: Method Not Allowed");
            case 500:
                throw new BridgeError("500 Internal Server Error");
            default:
                throw new BridgeError("Unexpected response from server");
        }
    }
        
    public Object parseResponse(String output) throws BridgeError{
        Object jsonValue = null;
        
        try {
            if ("XML".equals(contentType)) {
                jsonValue = JSONValue.parseWithException(XML.toJSONObject(output).toString());
            } else {
                jsonValue = JSONValue.parseWithException(output);
            }
        } catch (JSONException | ParseException e){
            // Assume all 200 responses will match the correct JSON or XML content type.
            LOGGER.error("There was a parse exception with the response", e);
        } catch (Exception e) {
            throw new BridgeError("An unexpected error has occured ", e);
        }
        
        return jsonValue;
    }
    
    private String getAuthHeader() {
        String auth = username + ":" + password;
        byte[] encodedAuth = 
            Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth);
    }
}
