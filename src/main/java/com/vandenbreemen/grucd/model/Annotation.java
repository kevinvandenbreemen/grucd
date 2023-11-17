package com.vandenbreemen.grucd.model;

import kotlin.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return this.argumentsAndValues.entrySet().stream().collect(Collectors.toList());
    }

    public String getArgument(String argName) {
        return this.argumentsAndValues.get(argName);
    }

    @Override
    public String toString() {
        StringBuilder r =  new StringBuilder("@Annotation(");
        getArguments().stream().forEach((argVal)->{
            r.append(argVal.getKey()).append(" = ").append(argVal.getValue()).append(", ");
        });
        return r.append(")").toString();

    }
}
