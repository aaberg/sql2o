package org.sql2o;

import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Settings class that stores all tunable parameters that are used in Sql2o.
 * Objects of this class are immutable, to change something please use {@code with*} methods
 * that will create new instances of this class
 *
 * Created by nickl on 20.01.17.
 */
public class Settings {

    private Quirks quirks;
    private Map<String, String> defaultColumnMappings;
    private boolean defaultCaseSensitive;


    public Settings(Quirks quirks, Map<String, String> defaultColumnMappings, boolean defaultCaseSensitive) {
        this.quirks = requireNonNull(quirks, "quirks can't be null");
        this.defaultColumnMappings = Collections.unmodifiableMap(requireNonNull(defaultColumnMappings, "defaultColumnMappings can't be null"));
        this.defaultCaseSensitive = defaultCaseSensitive;
    }

    public static Settings defaults = new Settings(new NoQuirks(), new HashMap<String, String>(), false);

    public Quirks getQuirks() {
        return quirks;
    }

    /**
     * Gets the default column mappings Map. column mappings added to this Map are always available when Sql2o attempts
     * to map between result sets and object instances.
     * @return  The {@link Map<String, String>} instance, which Sql2o internally uses to map column names with property
     * names.
     */
    public Map<String, String> getDefaultColumnMappings() {
        return defaultColumnMappings;
    }

    /**
     * Gets value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names.
     * @return
     */
    public boolean isDefaultCaseSensitive() {
        return defaultCaseSensitive;
    }


    /**
     * Creates a new instance of Settings class with updated {@code defaultColumnMappings} parameter
     * @param defaultColumnMappings new column mapping to use
     * @return new instance of Settings
     */
    public Settings withDefaultColumnMappings(Map<String, String> defaultColumnMappings){
       return new Settings(quirks, defaultColumnMappings, defaultCaseSensitive);
    }

    /**
     * Creates a new instance of Settings class with updated {@code defaultCaseSensitive} parameter
     * @param defaultCaseSensitive new case sensitivity to use
     * @return new instance of Settings
     */
    public Settings withDefaultCaseSensitive(boolean defaultCaseSensitive){
       return new Settings(quirks, defaultColumnMappings, defaultCaseSensitive);
    }

    /**
     * Creates a new instance of Settings class with updated {@code quirks} parameter
     * @param quirks new quirks to use
     * @return new instance of Settings
     */
    public Settings withQuirks(Quirks quirks){
       return new Settings(quirks, defaultColumnMappings, defaultCaseSensitive);
    }

}
