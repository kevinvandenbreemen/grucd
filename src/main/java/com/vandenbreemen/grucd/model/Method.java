package com.vandenbreemen.grucd.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Method implements Serializable  {

    private String name;
    private String returnType;
    private List<Parameter> parameters;

    public Method(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = new ArrayList<>();
    }

    public Method() {
        this.parameters = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public List<Parameter> getParameters() {
        return parameters;
    }
}
