package com.vandenbreemen.grucd.builder;

import com.vandenbreemen.grucd.model.Model;
import com.vandenbreemen.grucd.model.RelationType;
import com.vandenbreemen.grucd.model.Type;
import com.vandenbreemen.grucd.model.TypeRelation;

import java.util.*;

/**
 * Once the raw model types have been generated this object assembles the relations between them into a single unified model
 */
public class ModelBuilder {

    public Model build(List<Type> types) {
        Model model = new Model(types);

        Map<Type, AbstractSet<Type>> encapsulations = new HashMap<>();
        types.forEach(type -> {
            types.forEach(targetType->{
                if(type != targetType) {

                    type.getFields().forEach(field -> {
                        if(field.getTypeName().equals(targetType.getName())) {
                            AbstractSet<Type> targets = encapsulations.computeIfAbsent(type, type1 -> new HashSet<>());
                            targets.add(targetType);
                        }
                        field.getTypeArguments().forEach(arg->{
                            if(arg.equals(targetType.getName())) {
                                AbstractSet<Type> targets = encapsulations.computeIfAbsent(type, type1 -> new HashSet<>());
                                targets.add(targetType);
                            }
                        });
                    });

                }
            });
        });

        encapsulations.entrySet().forEach(relationSet->{
            relationSet.getValue().forEach(target->{
                model.addRelation(new TypeRelation(relationSet.getKey(), target, RelationType.encapsulates));
            });
        });

        return model;

    }

}