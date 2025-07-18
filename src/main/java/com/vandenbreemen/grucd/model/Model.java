package com.vandenbreemen.grucd.model;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Software system model
 */
public class Model {

    private List<Type> types;
    private List<TypeRelation> relations;

    private List<Type> unusedTypes;

    public Model(List<Type> types) {
        this.types = types;
        this.relations = new ArrayList<>();
        this.unusedTypes = new ArrayList<>();
    }

    public void addRelation(TypeRelation relation) {
        this.relations.add(relation);
    }

    public List<Type> getTypes() {
        return types;
    }

    public List<TypeRelation> getRelations() {
        return relations;
    }

    public List<Type> getUnusedTypes() {
        return unusedTypes;
    }

    public void setUnusedTypes(List<Type> unusedTypes) {
        this.unusedTypes = unusedTypes;
    }

    public Model getTypesReferencingOrReferencedBy(Type type, int numLevels) {

        HashSet<Type> relatedTypes = new HashSet<>();
        relatedTypes.add(type); // Include the original type

        // Get all related types
        relatedTypes.addAll(getRelatedTypesRecursive(type, numLevels, relatedTypes));

        // Create list of types for the new model
        List<Type> typesList = new ArrayList<>(relatedTypes);

        // Find all relations between these types
        List<TypeRelation> relevantRelations = new ArrayList<>();
        for (TypeRelation relation : relations) {
            if (relatedTypes.contains(relation.getFrom()) && relatedTypes.contains(relation.getTo())) {
                relevantRelations.add(relation);
            }
        }

        // Create new model with the filtered types
        Model filteredModel = new Model(typesList);

        // Add all relevant relations
        for (TypeRelation relation : relevantRelations) {
            filteredModel.addRelation(relation);
        }

        return filteredModel;
    }

    private List<Type> getRelatedTypesRecursive(Type type, int numLevels, HashSet<Type> result) {
        if(numLevels == 0) {
            return Collections.emptyList();
        }

        for (TypeRelation relation : relations) {
            if(relation.getFrom() == type) {
                if(result.add(relation.getTo())) {
                    result.addAll(
                            getRelatedTypesRecursive(relation.getTo(), numLevels - 1, result) // Recursively find types referencing this type
                    );
                }
            } else if(relation.getTo() == type) {
                if(result.add(relation.getFrom())) {
                    result.addAll(
                            getRelatedTypesRecursive(relation.getFrom(), numLevels - 1, result) // Recursively find types referenced by this type
                    );
                }
            }
        }
        return result.stream().toList();
    }

    public List<Type> typesWithName(@NotNull String typeName) {
        List<Type> result = new ArrayList<>();
        for (Type type : types) {
            if(type.getName().equals(typeName)) {
                result.add(type);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Model {\n");
        for (Type type : types) {
            sb.append(" ").append(type.getName()).append(",\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
