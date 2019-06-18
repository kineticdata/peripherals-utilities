package com.kineticdata.bridgehub.adapter.counter;

import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CounterAdapter implements BridgeAdapter {
    
    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/
    
    /** Defines the adapter display name. */
    public static final String NAME = "Counter Bridge";
    
    /** Defines the logger */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CounterAdapter.class);

    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties.load(CounterAdapter.class.getResourceAsStream("/"+CounterAdapter.class.getName()+".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            logger.warn("Unable to load "+CounterAdapter.class.getName()+" version properties.", e);
            VERSION = "Unknown";
        }
    }
    
    Integer counter;
    Integer chunkSize;
    
    /** Defines the collection of property names for the adapter. */
    public static class Properties {
        // Specify the property name constants
        public static final String FileName = "File Name";
    }
    
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
            new ConfigurableProperty(Properties.FileName).setIsRequired(true).setValue("counter-config")
                .setDescription("A .txt file. The extension will be automatically added by the bridge.")
    );
    
    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
       return  VERSION;
    }
    
    @Override
    public ConfigurablePropertyMap getProperties() {
        return properties;
    }
    
    @Override
    public void setProperties(Map<String,String> parameters) {
        properties.setValues(parameters);
    }
     
    @Override
    public void initialize() throws BridgeError {
        try {
            configNextChunk(true);
        } catch (IOException ioe) {
            throw new BridgeError("Error setting up Config file",ioe);
        }
        this.chunkSize = 100;
    }
    
    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/
    
    @Override
    public Count count(BridgeRequest request) throws BridgeError {
        // Count shows you what the last value was without incrementing it
        return new Count(counter);
    }
    
    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        counter += 1;
        
        if (request.getFieldString() == null) {
            List<String> fields = new ArrayList<String>();
            fields.add("count");
            request.setFields(fields);
        }
        
        if (!request.getFieldString().trim().equals("count")) {
            throw new BridgeError(request.getFieldString() + " contains an invalid field. The only valid field is currently 'count'");
        }
        
        if ((counter%chunkSize) == chunkSize-10) {
            try {
                configNextChunk(false);
            } catch (IOException ioe) {
                throw new BridgeError("Error setting up Config file",ioe);
            }
        }
        
        Map<String,Object> counterObject = new LinkedHashMap<String,Object>();
        counterObject.put("count", counter.toString());
        
        return new Record(counterObject);
    }
    
    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        List<Record> records = new ArrayList<Record>();
        records.add(retrieve(request));

        List<String> fields = new ArrayList<String>(records.get(0).getFieldNames());
        
        return new RecordList(fields,records);
    }
    
    /*---------------------------------------------------------------------------------------------
     * HELPER METHODS
     *-------------------------------------------------------------------------------------------*/
    
    // If a config file has not been created for this bridge instance, it creates it. 
    private void configNextChunk(Boolean startup) throws BridgeError,IOException {
        String configName = getProperties().getValue(Properties.FileName);
        String counterConfig = System.getProperty("com.kineticdata.bridgehub.dataDirectory") + "/" + configName + ".txt";
        Integer chunkSize = 100;

        File file = new File(counterConfig);
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException ioe) {
            throw new BridgeError("Error attempting to create counter config file at '"+counterConfig+"'");
        }

//        List<String> lines = Files.readAllLines(Paths.get(counterConfig), Charset.defaultCharset());
        // Read the file in java 6
        List<String> lines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while((line = br.readLine()) != null) {
             lines.add(line);
        }
        
        Integer newCount;
        if (lines.isEmpty()) {
            newCount = chunkSize;
        } else {
            String currentCount = lines.get(0);
            newCount = Integer.valueOf(currentCount) + chunkSize;
        }
        
        if (startup || newCount.intValue() != getClosestChunk(counter,chunkSize).intValue()) {
            counter = newCount;
        }

        PrintWriter writer = new PrintWriter(file);
        writer.write(newCount.toString());
        writer.close();
    }
    
    private Integer getClosestChunk(Integer count, Integer chunkSize) {
        Integer tempCount = count;
        while (tempCount % chunkSize != 0) {
            tempCount += 1;
        }
        return tempCount;
    }
}
