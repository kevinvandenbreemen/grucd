package com.vandenbreemen.grucd.parse

import com.vandenbreemen.grucd.builder.ModelBuilder
import com.vandenbreemen.grucd.model.RelationType
import com.vandenbreemen.grucd.model.Visibility
import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.ast.DefaultAstTerminal
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.common.klass.KlassIdentifier
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNullOrEmpty
import org.apache.log4j.Logger
import org.apache.log4j.NDC
import org.junit.jupiter.api.Test

internal class KotlinParserTest {

    companion object {
        val logger: Logger = Logger.getLogger(KotlinParserTest::class.java)
    }

    @Test
    fun `should parse a kotlin class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        types.shouldNotBeEmpty()
        types.size shouldBeEqualTo 1
        types[0].name shouldBeEqualTo "KotlinClass"
    }

    @Test
    fun `should parse a kotlin enum`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/EnumExample.kt")
        types.shouldNotBeEmpty()
        types.size shouldBeEqualTo 1
        types[0].name shouldBeEqualTo "EnumExample"
        types[0].fields.size shouldBeEqualTo 2
    }

    @Test
    fun `should parse multiple enums in same file`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/MultiEnum.kt")
        types.shouldNotBeEmpty()
        types.size shouldBeEqualTo 2
    }

    @Test
    fun `should parse class with parametrized field`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithListOfEncapsulated.kt")
        types.size shouldBeEqualTo 2

        val type = types[0]
        type.fields.size shouldBeEqualTo 1

        val model = ModelBuilder().build(types)
        model.relations.shouldNotBeEmpty()
        model.relations[0].from shouldBeEqualTo model.types[0]
        model.relations[0].to shouldBeEqualTo model.types[1]
        model.relations[0].type shouldBeEqualTo RelationType.encapsulates
    }

    @Test
    fun `should parse class with parametrized field declaration`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithListOfEncapsulatedDeclared.kt")
        types.size shouldBeEqualTo 2

        val type = types[0]
        type.fields.size shouldBeEqualTo 1

        val model = ModelBuilder().build(types)
        model.relations.shouldNotBeEmpty()
        model.relations[0].from shouldBeEqualTo model.types[0]
        model.relations[0].to shouldBeEqualTo model.types[1]
        model.relations[0].type shouldBeEqualTo RelationType.encapsulates
    }

    @Test
    fun `should parse class with parametrized parameter`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithParameterWithListOfEncapsulated.kt")
        types.size shouldBeEqualTo 2

        val type = types[0]
        type.fields.size shouldBeEqualTo 1

        val model = ModelBuilder().build(types)
        model.relations.shouldNotBeEmpty()
        model.relations[0].from shouldBeEqualTo model.types[0]
        model.relations[0].to shouldBeEqualTo model.types[1]
        model.relations[0].type shouldBeEqualTo RelationType.encapsulates
    }

    @Test
    fun `should parse class with parametrized parameter - var`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithParameterWithVarListOfEncapsulated.kt")
        types.size shouldBeEqualTo 2

        val type = types[0]
        type.fields.size shouldBeEqualTo 1

        val model = ModelBuilder().build(types)
        model.relations.shouldNotBeEmpty()
        model.relations[0].from shouldBeEqualTo model.types[0]
        model.relations[0].to shouldBeEqualTo model.types[1]
        model.relations[0].type shouldBeEqualTo RelationType.encapsulates
    }

    @Test
    fun `should determine the package name of a Kotlin class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        types[0].pkg shouldBeEqualTo "kotlin"
    }

    @Test
    fun `should parse class attributes`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        val fields = types[0].fields
        fields.shouldNotBeEmpty()

        fields[0].name shouldBeEqualTo "name"
        fields[0].typeName shouldBeEqualTo "String"
        fields[0].visibility shouldBeEqualTo Visibility.Public
    }

    @Test
    fun `should parse private field`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinWithPrivates.kt")
        val fields = types[0].fields
        fields.shouldNotBeEmpty()

        fields[0].name shouldBeEqualTo "privateName"
        fields[0].typeName shouldBeEqualTo "String"
        fields[0].visibility shouldBeEqualTo Visibility.Private
    }

    @Test
    fun `should parse declared fields inside a class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClassWithAFieldInsideIt.kt")
        val fields = types[0].fields
        fields.shouldNotBeEmpty()

        fields[0].name shouldBeEqualTo "hiddenName"
        fields[0].typeName shouldBeEqualTo "String"
        fields[0].visibility shouldBeEqualTo Visibility.Private

        fields[1].name shouldBeEqualTo "hiddenVariableName"
        fields[1].typeName shouldBeEqualTo "String"
        fields[1].visibility shouldBeEqualTo Visibility.Private

    }

    @Test
    fun `should parse imports to a type`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/Importer.kt")
        types[0].imports.shouldNotBeEmpty()
        types[0].imports[0] shouldBeEqualTo "java.util.ArrayList"
    }

    @Test
    fun `should parse method on a class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        val type = types[0]

        type.methods.shouldNotBeEmpty()
        val method = type.methods[0]
        method.name shouldBeEqualTo "requireParams"
        method.returnType shouldBeEqualTo ""
    }

    @Test
    fun `should parse method with a return type on a class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        val type = types[0]

        type.methods.shouldNotBeEmpty()
        val method = type.methods[1]
        method.name shouldBeEqualTo "getSystemData"
        method.returnType shouldBeEqualTo "List<String>"
    }

    @Test
    fun `should parse parameter types of a method`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        val type = types[0]

        type.methods.shouldNotBeEmpty()
        val method = type.methods[1]
        method.parameters.size shouldBeEqualTo 2

        method.parameters[0].name shouldBeEqualTo "param1"
        method.parameters[0].typeName shouldBeEqualTo "String"

        method.parameters[1].name shouldBeEqualTo "matrix"
        method.parameters[1].typeName shouldBeEqualTo "Int"
    }

    @Test
    fun `should not parse private methods`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        val type = types[0]

        type.methods.size shouldBeEqualTo 2
    }

    @Test
    fun `should parse nested classes`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithANestedClass.kt")
        types.size shouldBeEqualTo 2

        val model = ModelBuilder().build(types)
        model.relations.size shouldBeEqualTo 1
        val relation = model.relations[0]
        relation.from.name shouldBeEqualTo "ClassWithANestedClass"
        relation.to.name shouldBeEqualTo "NestedClass"

        types[1].name shouldBeEqualTo "ClassWithANestedClass"
        types[0].name shouldBeEqualTo "NestedClass"

        types[1].methods.size shouldBeEqualTo 1
        types[0].methods.size shouldBeEqualTo 2
    }

    @Test
    fun `should parse functions with function parameters`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithCallbacks.kt")
        types.size shouldBeEqualTo 1
        val t = types[0]
        t.methods.count() shouldBeEqualTo 1

        val method = t.methods[0]
        method.parameters.count() shouldBeEqualTo 1
        val parameter = method.parameters[0]

        parameter.typeName.shouldNotBeNullOrEmpty()
    }

    @Test
    fun `should parse subclass`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/Superclass.kt")
        types.size shouldBeEqualTo 2

        val model = ModelBuilder().build(types)
        model.relations.size shouldBeEqualTo 1
        val relation = model.relations[0]
        relation.type shouldBeEqualTo RelationType.subclass
        relation.from.name shouldBeEqualTo "Subclass"
        relation.to.name shouldBeEqualTo "Superclass"
    }

    @Test
    fun `should parse interface implementation`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/InterfaceImplementation.kt")
        types.size shouldBeEqualTo 2

        val model = ModelBuilder().build(types)
        model.relations.size shouldBeEqualTo 1
        val relation = model.relations[0]
        relation.type shouldBeEqualTo RelationType.implementation
        relation.from.name shouldBeEqualTo "InterfaceImplementation"
        relation.to.name shouldBeEqualTo "Interface"
    }

    @Test
    fun `should parse javadoc on a kotlin class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/KotlinClass.kt")
        types[0].classDoc shouldBeEqualTo "Unit test Kotlin Class"
    }

    @Test
    fun `should parse javadoc on nested classes`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithANestedClass.kt")
        types.size shouldBeEqualTo 2

        println(types[1].name)
        println(types[0].name)

        types[1].classDoc shouldBeEqualTo "Parent class"
        types[0].classDoc shouldBeEqualTo "Nested class"
    }

    @Test
    fun `learning test to parse Kotlin with kotlinx dot ast`() {


        val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(AstSource.File("src/test/resources/kotlin/KotlinClass.kt"))
        println(kotlinFile)

        kotlinFile.summary(false).onSuccess { astList ->
            astList.forEach { item->
                run {
                    NDC.push(item.javaClass.name)
                    logger.info(item.description)
                    evaluate(item)
                    NDC.pop()
                }
            }
        }.onFailure { errors ->
            errors.forEach(::println)
        }

    }

    @Test
    fun `should detect annotation on a class`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithAnnotation.kt")
        types.size shouldBeEqualTo 2

        val type = types[1]
        println(type)

        type.annotations.shouldNotBeEmpty()
        type.annotations.size shouldBeEqualTo 1

        val annotation = type.annotations[0]
        annotation.typeName shouldBeEqualTo "MyAnnotation"
        annotation.arguments.size shouldBeEqualTo 1

        val arg = annotation.arguments[0]
        arg.key shouldBeEqualTo "name"
        arg.value shouldBeEqualTo "Test Annotation"

    }

    @Test
    fun `should detect annotations with enumerated values`() {
        val types = ParseKotlin().parse("src/test/resources/kotlin/ClassWithEnumeratedAnnotation.kt")
        types.size shouldBeEqualTo 3

        val type = types[2]
        println(type)

        type.annotations.shouldNotBeEmpty()
        type.annotations.size shouldBeEqualTo 1

        val annotation = type.annotations[0]

        println(annotation)

        annotation.typeName shouldBeEqualTo "SpecialTypedAnnotation"
        annotation.arguments.size shouldBeEqualTo 1

        val arg = annotation.arguments[0]
        arg.key shouldBeEqualTo "type"
        arg.value shouldBeEqualTo "Type2"
    }

    private fun evaluate(ast: Ast) {

        logger.info("evaluating a ${ast.javaClass}")

        (ast as? KlassDeclaration)?.let { klassDeclaration ->
            NDC.push("Clz ${klassDeclaration.identifier?.identifier}")
            klassDeclaration.children.forEach { child->
                evaluate(child)
            }

            logger.info("AST info=${klassDeclaration.info}")

            klassDeclaration.attributes.forEach {
                logger.info("Attribute ${it.description}")
                evaluate(it)
            }

            NDC.pop()
        }
        (ast as? DefaultAstNode)?.let { defaultAstNode ->
            NDC.push("defaultASTNode")
            defaultAstNode.children.forEach {
                logger.info("defAstNodeChild=$it")
            }
            NDC.pop()
        }
        (ast as? DefaultAstTerminal)?.let { terminal->
            logger.info("terminal - $terminal")
        }
        (ast as? KlassIdentifier)?.let {
            logger.info("identifier - ${it.identifier}: ${it.rawName}")
        }
    }
}