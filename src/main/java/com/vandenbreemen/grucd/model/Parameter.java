package com.vandenbreemen.grucd.model;

public class Parameter implements java.io.Serializable {

    private String name;
    private String typeName;

    public Parameter() {
        this.name = "";
        this.typeName = "";
    }

    public Parameter(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }
}
