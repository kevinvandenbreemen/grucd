package com.vandenbreemen.grucd.parse;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.vandenbreemen.grucd.model.Parameter;
import com.vandenbreemen.grucd.model.*;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParseJava {

    private static final Logger logger = Logger.getLogger(ParseJava.class);

    private class VisitorContext {
        Type parentType;

        public VisitorContext(Type parentType) {
            this.parentType = parentType;
        }
    }

    public List<Type> parse(String filePath) {

        try {
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> unit = parser.parse(new File(filePath));

            List<Type> result = new ArrayList<>();


            unit.getResult().ifPresent(new Consumer<CompilationUnit>() {

                ArrayList<String> imports = new ArrayList<>();

                @Override
                public void accept(CompilationUnit unit) {
                    unit.accept(new VoidVisitorAdapter<VisitorContext>() {

                        @Override
                        public void visit(ImportDeclaration importDeclaration, VisitorContext arg) {
                            imports.add(importDeclaration.getNameAsString());
                        }

                        @Override
                        public void visit(JavadocComment n, VisitorContext classContext) {
                            logger.trace("Found javadoc comment:\n"+n.parse().toText());
                            if(classContext != null) {   //  Don't attempt this if it's a javadoc comment on a package declaration
                                classContext.parentType.setClassDoc(n.parse().toText().trim());
                            }
                            super.visit(n, classContext);
                        }

                        @Override
                        public void visit(ClassOrInterfaceDeclaration n, VisitorContext visitorContext) {

                            String name = n.getNameAsString();
                            final String[] packageName = {""};
                            n.getFullyQualifiedName().ifPresent(new Consumer<String>() {
                                @Override
                                public void accept(String fullName) {
                                    if(fullName.length() > name.length()) {
                                        packageName[0] = fullName.substring(0, fullName.length() - (name.length() + 1));
                                    }
                                }
                            });

                            Type currentType = new Type(n.getNameAsString(), packageName[0], n.isInterface() ? TypeType.Interface : TypeType.Class);
                            currentType.setImports(imports);

                            if(visitorContext != null) {
                                currentType.setParentType(visitorContext.parentType);
                            }

                            result.add(currentType);

                            NDC.push(currentType.getName());
                            try {
                                n.getExtendedTypes().forEach(classOrInterfaceType -> {
                                    logger.trace("Recognized parent type "+classOrInterfaceType.getNameAsString());
                                    currentType.addSuperType(classOrInterfaceType.getNameAsString());
                                });
                                n.getImplementedTypes().forEach(classOrInterfaceType -> {
                                    logger.trace("Recognized implemented interface " + classOrInterfaceType.getNameAsString());
                                    currentType.addInterface(classOrInterfaceType.getNameAsString());
                                });
                                super.visit(n, new VisitorContext(currentType));
                            } finally {
                                NDC.pop();
                            }

                        }

                        @Override
                        public void visit(EnumDeclaration n, VisitorContext arg) {

                            String name = n.getNameAsString();
                            final String[] packageName = {""};
                            n.getFullyQualifiedName().ifPresent(new Consumer<String>() {
                                @Override
                                public void accept(String fullName) {
                                    if(fullName.length() > name.length()) {
                                        packageName[0] = fullName.substring(0, fullName.length() - (name.length() + 1));
                                    }
                                }
                            });

                            Type currentType = new Type(n.getNameAsString(), packageName[0], TypeType.Enum);
                            result.add(currentType);

                            NDC.push(currentType.getName());
                            VisitorContext context = new VisitorContext(currentType);
                            logger.trace("Visiting enum and fields");
                            try {
                                super.visit(n, context);
                            } finally {
                                NDC.pop();
                            }
                        }

                        @Override
                        public void visit(EnumConstantDeclaration n, VisitorContext visitorContext) {

                            Type currentType = visitorContext.parentType;
                            currentType.addField(new Field(n.getNameAsString(), currentType.getName(), Visibility.Public));

                            super.visit(n, visitorContext);
                        }

                        @Override
                        public void visit(FieldDeclaration n, VisitorContext visitorContext) {

                            Type currentType = visitorContext.parentType;

                            for (VariableDeclarator dec : n.getVariables()) {
                                Visibility visibility;
                                if(n.hasModifier(Modifier.publicModifier().getKeyword())){
                                    visibility = Visibility.Public;
                                } else {
                                    visibility = Visibility.Private;
                                }
                                Field field = new Field(dec.getName().asString(), dec.getTypeAsString(), visibility);

                                if(dec.getType() instanceof ClassOrInterfaceType) {
                                    ((ClassOrInterfaceType) dec.getType()).getTypeArguments().ifPresent(arguments->{
                                        arguments.forEach(type -> {
                                            field.addTypeArgument(type.getElementType().asString());
                                        });
                                    });
                                }

                                currentType.addField(field);
                            }
                        }

                        @Override
                        public void visit(MethodDeclaration n, VisitorContext visitorContext) {
                            super.visit(n, visitorContext);

                            Type currentType = visitorContext.parentType;

                            if(!n.hasModifier(Modifier.publicModifier().getKeyword())) {
                                return;
                            }

                            Method method = new Method(n.getNameAsString(), n.getTypeAsString());

                            logger.trace("mthd:  "+method.getName()+ ": "+method.getReturnType());

                            n.getParameters().forEach(p->{
                                logger.trace("parm "+p.getNameAsString());
                                method.addParameter(new Parameter(p.getNameAsString(), p.getTypeAsString()));
                            });

                            currentType.addMethod(method);
                        }
                    }, null);
                }
            });

            return result;

        } catch (FileNotFoundException fex) {
            logger.error("Failed to parse", fex);
        } finally {
            NDC.pop();
        }

        return null;
    }

}
