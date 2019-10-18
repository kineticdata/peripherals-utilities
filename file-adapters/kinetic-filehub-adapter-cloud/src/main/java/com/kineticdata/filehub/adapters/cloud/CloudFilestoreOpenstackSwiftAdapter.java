package com.kineticdata.filehub.adapters.cloud;

import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.util.Map;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;

public class CloudFilestoreOpenstackSwiftAdapter extends CloudFilestoreAdapter {
    
    /** Defines the adapter display name. */
    public static final String NAME = "Openstack Swift";
    
    /** Defines the collection of property names for the adapter. */
    public static class Properties {
        public static final String CONTAINER = "Container";
        public static final String CREDENTIAL = "Credential";
        public static final String IDENTITY = "Identity";
    }
    
    /** 
     * Specifies the configurable properties for the adapter.  These are populated as part of object
     * construction so that the collection of properties (default values, menu options, etc) are 
     * available before the adapter is configured.  These initial properties can be used to 
     * dynamically generate the list of configurable properties, such as when the Kinetic Filehub
     * application prepares the new Filestore display.
     */
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.IDENTITY)
            .setIsRequired(true),
        new ConfigurableProperty(Properties.CREDENTIAL)
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
        return ContextBuilder.newBuilder("openstack-swift")
            .credentials(
                properties.getValue(Properties.IDENTITY), 
                properties.getValue(Properties.CREDENTIAL))
            .buildView(BlobStoreContext.class);
    }

    @Override
    protected String getContainer() {
        return properties.getValue(Properties.CONTAINER);
    }

    @Override
    protected boolean supportsUploadMultipart() {
        return true;
    }

    @Override
    protected boolean supportsUploadStream() {
        return true; // unverified
    }
    
}
