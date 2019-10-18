package com.kineticdata.filehub.adapters.cloud;

import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.util.Map;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;

public class CloudFilestoreMicrosoftAzureAdapter extends CloudFilestoreAdapter {
    
    /** Defines the adapter display name. */
    public static final String NAME = "Microsoft Azure";
    
    /** Defines the collection of property names for the adapter. */
    public static class Properties {
        public static final String CONTAINER = "Container";
        public static final String STORAGE_ACCOUNT_KEY = "Storage Account Key";
        public static final String STORAGE_ACCOUNT_NAME = "Storage Account Name";
    }
    
    /** 
     * Specifies the configurable properties for the adapter.  These are populated as part of object
     * construction so that the collection of properties (default values, menu options, etc) are 
     * available before the adapter is configured.  These initial properties can be used to 
     * dynamically generate the list of configurable properties, such as when the Kinetic Filehub
     * application prepares the new Filestore display.
     */
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.STORAGE_ACCOUNT_NAME)
            .setIsRequired(true),
        new ConfigurableProperty(Properties.STORAGE_ACCOUNT_KEY)
            .setIsRequired(true)
            .setIsSensitive(true),
        new ConfigurableProperty(Properties.CONTAINER)
            .setIsRequired(true)
    );
    
    
    /*----------------------------------------------------------------------------------------------
     * CONFIGURATION
     *--------------------------------------------------------------------------------------------*/
    
    /**
     * Initializes the filestore adapter.  This method will be called when the properties are first
     * specified, and when the properties are updated.
     * 
     * @param propertyValues 
     */
    @Override
    public void initialize(Map<String, String> propertyValues) {
        // Set the configurable properties
        properties.setValues(propertyValues);
    }

    /**
     * Returns the display name for the adapter.
     * 
     * @return 
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Returns the collection of configurable properties for the adapter.
     * 
     * @return 
     */
    @Override
    public ConfigurablePropertyMap getProperties() {
        return properties;
    }
    
    
    /*----------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *--------------------------------------------------------------------------------------------*/

    @Override
    protected BlobStoreContext buildBlobStoreContext() {
        return ContextBuilder.newBuilder("azureblob")
            .credentials(
                properties.getValue(Properties.STORAGE_ACCOUNT_NAME), 
                properties.getValue(Properties.STORAGE_ACCOUNT_KEY))
            .buildView(BlobStoreContext.class);
    }

    @Override
    protected String getContainer() {
        return properties.getValue(Properties.CONTAINER);
    }

    @Override
    protected boolean supportsUploadMultipart() {
        return true; // unverified
    }

    @Override
    protected boolean supportsUploadStream() {
        return true; // unverified
    }

}
