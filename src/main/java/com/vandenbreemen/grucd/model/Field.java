package com.vandenbreemen.grucd.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Field implements Serializable  {

    private String name;
    private String typeName;

    private boolean show = true;

    private Visibility visibility;

    /**
     * For generics
     */
    private List<String> typeArguments;

    public Field(String name, String typeName, Visibility visibility) {
        this.name = name;
        this.typeName = typeName;
        this.visibility = visibility;
        this.typeArguments = new ArrayList<>();
    }

    public Field() {
        this.typeArguments = new ArrayList<>();
        this.show = true;
        this.visibility = Visibility.Public;
    }

    public void addTypeArgument(String type) {
        this.typeArguments.add(type);
    }

    public List<String> getTypeArguments() {
        return typeArguments;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * Hide this field
     */
    public void hide() {
        this.show = false;
    }

    public boolean shouldShow() {
        return show;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public String toString() {
        return name + " : " + typeName;
    }
}
