package com.vandenbreemen.grucd.model;

import kotlin.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract representation of any annotation
 */
public class Annotation {

    private String typeName;

    private Map<String, String> argumentsAndValues;

    public Annotation() {
        this.argumentsAndValues = new HashMap<>();
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void addArgument(String argName, String value) {
        this.argumentsAndValues.put(argName, value);
    }

    public List<Map.Entry<String, String>> getArguments() {
        return this.argumentsAndValues.entrySet().stream().toList();
    }
}
