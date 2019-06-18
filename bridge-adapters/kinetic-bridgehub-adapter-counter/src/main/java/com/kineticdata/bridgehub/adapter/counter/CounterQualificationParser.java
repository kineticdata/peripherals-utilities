package com.kineticdata.bridgehub.adapter.counter;

import com.kineticdata.bridgehub.adapter.QualificationParser;

/**
 *
 */
public class CounterQualificationParser extends QualificationParser {
    public String encodeParameter(String name, String value) {
        return value;
    }
}
