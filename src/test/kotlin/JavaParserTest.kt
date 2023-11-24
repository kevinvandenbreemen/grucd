import com.vandenbreemen.grucd.builder.ModelBuilder
import com.vandenbreemen.grucd.model.RelationType
import com.vandenbreemen.grucd.model.Visibility
import com.vandenbreemen.grucd.parse.ParseJava
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class JavaParserTest {

    @Test
    fun `should parse java class`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")

        types.shouldNotBeEmpty()
        types.size shouldBeEqualTo 1
        types[0].name shouldBeEqualTo "TestJava"
    }

    @Test
    fun `should parse public java field`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")
        val type = types[0]
        val fields = type.fields

        fields.shouldNotBeNull()

        fields.size shouldBeEqualTo 2
        fields[0].name shouldBeEqualTo "publicInt"
        fields[0].typeName shouldBeEqualTo "int"
        fields[0].visibility shouldBeEqualTo Visibility.Public
    }

    @Test
    fun `should parse private java fields`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")
        val type = types[0]
        val fields = type.fields

        fields.size shouldBeEqualTo 2
        fields[1].name shouldBeEqualTo "privateInt"
        fields[1].visibility shouldBeEqualTo Visibility.Private

    }

    @Test
    fun `should be able to determine the package of a new class`() {
        val types = ParseJava().parse("src/test/resources/pkg/test/PackageTest.java")
        types[0].pkg shouldBeEqualTo "pkg.test"
    }

    @Test
    fun `should include methods on the class`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")
        val type = types[0]
        val methods = type.methods

        methods.shouldNotBeNull()

        methods.forEach { m->println(m.name) }

        methods.size shouldBeEqualTo 3

        methods[0].name shouldBeEqualTo "getPrivateInt"
        methods[0].returnType shouldBeEqualTo "int"

        methods[1].name shouldBeEqualTo "engagePrimaryInterlock"
        methods[1].returnType shouldBeEqualTo "void"
    }

    @Test
    fun `should include method arguments in methods`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")
        val type = types[0]
        val methods = type.methods

        methods[1].name shouldBeEqualTo "engagePrimaryInterlock"
        methods[1].returnType shouldBeEqualTo "void"
        methods[1].parameters.shouldNotBeNull()
        methods[1].parameters[0].name shouldBeEqualTo "parameter"
        methods[1].parameters[1].name shouldBeEqualTo "arguments"
        methods[1].parameters[0].typeName shouldBeEqualTo "String"
        methods[1].parameters[1].typeName shouldBeEqualTo "int[]"
    }

    @Test
    fun `should not include private methods`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")
        val type = types[0]
        val methods = type.methods

        methods.size shouldBeEqualTo 3
    }

    @Test
    fun `should recognized encapsulated object`() {
        val types = ParseJava().parse("src/test/resources/encapsulation.test/Encapsulation.java")
        types.size shouldBeEqualTo 2

        val builder = ModelBuilder()
        val model = builder.build(types)

        println(model.relations[0])

        model.relations.shouldNotBeEmpty()
        model.relations[0].from shouldBeEqualTo model.types[0]
        model.relations[0].to shouldBeEqualTo model.types[1]
        model.relations[0].type shouldBeEqualTo RelationType.encapsulates
    }

    @Test
    fun `should recognized encapsulated list of objects`() {
        val types = ParseJava().parse("src/test/resources/encapsulation.test/ListEncapsulationTest.java")
        types.size shouldBeEqualTo 2

        types[0].fields[0].typeArguments.shouldNotBeEmpty()

        val builder = ModelBuilder()
        val model = builder.build(types)

        println(model.relations[0])

        model.relations.shouldNotBeEmpty()
        model.relations[0].from shouldBeEqualTo model.types[0]
        model.relations[0].to shouldBeEqualTo model.types[1]
        model.relations[0].type shouldBeEqualTo RelationType.encapsulates
    }

    @Test
    fun `should parse enumerations`() {
        val types = ParseJava().parse("src/test/resources/enum/Enum.java")
        types.size shouldBeEqualTo 2
    }

    @Test
    fun `should parse nested class`() {
        val types = ParseJava().parse("src/test/resources/encapsulation.test/NestedClass.java")
        types.size shouldBeEqualTo 2

        types[0].name shouldBeEqualTo "MainClass"
        types[1].name shouldBeEqualTo "NestedClass"

        types[0].methods.size shouldBeEqualTo 2
        types[1].methods.size shouldBeEqualTo 1

        val builder = ModelBuilder()
        val model = builder.build(types)

        model.relations.shouldNotBeEmpty()
        model.relations.size shouldBeEqualTo 1

        val relation = model.relations[0]
        relation.from.name shouldBeEqualTo "MainClass"
        relation.to.name shouldBeEqualTo "NestedClass"
        relation.type shouldBeEqualTo RelationType.nested
    }

    @Test
    fun `should parse subclasses`() {
        val types = ParseJava().parse("src/test/resources/extending/Subclass.java")
        types.size shouldBeEqualTo 2

        val builder = ModelBuilder()
        val model = builder.build(types)

        model.relations.size shouldBeEqualTo 1
        val relation = model.relations[0]
        relation.type shouldBeEqualTo RelationType.subclass
        relation.from.name shouldBeEqualTo "Subclass"
        relation.to.name shouldBeEqualTo "Superclass"
    }

    @Test
    fun `should parse interface implementations`() {
        val types = ParseJava().parse("src/test/resources/extending/InterfaceImplementation.java")
        types.size shouldBeEqualTo 2

        val builder = ModelBuilder()
        val model = builder.build(types)

        model.relations.size shouldBeEqualTo 1
        val relation = model.relations[0]
        relation.type shouldBeEqualTo RelationType.implementation
        relation.from.name shouldBeEqualTo "Implementation"
        relation.to.name shouldBeEqualTo "Interface"
    }

    @Test
    fun `should handle multiple classes with the same name`() {
        val parser = ParseJava()
        val main = parser.parse("src/test/resources/multiname/MainThing.java")[0]
        val thing1 = parser.parse("src/test/resources/multiname/thing1/Thing.java")[0]
        val thing2 = parser.parse("src/test/resources/multiname/thing2/Thing.java")[0]

        val model = ModelBuilder().build(listOf(main, thing1, thing2))

        model.relations.size shouldBeEqualTo 1
        val relation = model.relations[0]
        relation.from shouldBeEqualTo main
        relation.to shouldBeEqualTo thing1
    }

    @Test
    fun `should read javadoc on a class`() {
        val types = ParseJava().parse("src/test/resources/TestJava.java")
        val type = types[0]

        type.classDoc shouldBeEqualTo "Java test class for unit testing"
    }

    @Test
    fun `should not mix up javadoc`() {
        val types = ParseJava().parse("src/test/resources/pkg/test/PackageTest.java")
        types[0].classDoc shouldBeEqualTo "Class javadoc\n" +
                "test a new line"
    }

    @Test
    fun `should parse javadoc on nested classes`() {
        val types = ParseJava().parse("src/test/resources/encapsulation.test/NestedClass.java")
        types[0].classDoc shouldBeEqualTo "Main Class"
        types[1].classDoc shouldBeEqualTo "Nested Class"
    }

    @Test
    fun `should parse javadoc on two classes in same file`() {
        val types = ParseJava().parse("src/test/resources/encapsulation.test/Encapsulation.java")
        types[0].classDoc shouldBeEqualTo "Containing Class"
        types[1].classDoc shouldBeEqualTo "Encapsulated Class"
    }

    @Test
    fun `should detect unused type`() {
        val types = ParseJava().parse("src/test/resources/unused/UnusedClassExample.java");

        val model = ModelBuilder().build(types)

        model.unusedTypes.shouldNotBeEmpty()
        model.unusedTypes.size shouldBeEqualTo 1
        model.unusedTypes[0].name shouldBeEqualTo "UnusedClass"
    }

    @Test
    fun `should detect unused types in the form of unimplemented interfaces`() {
        val types = ParseJava().parse("src/test/resources/unused/UnusedInterfaces.java");

        val model = ModelBuilder().build(types)

        model.unusedTypes.shouldNotBeEmpty()
        model.unusedTypes.size shouldBeEqualTo 1
        model.unusedTypes[0].name shouldBeEqualTo "UnusedInterface"
    }

    @Test
    fun `should detect unused type in the form of un-extended classes`() {
        val types = ParseJava().parse("src/test/resources/unused/UnusedSuperExample.java");

        val model = ModelBuilder().build(types)

        model.unusedTypes.shouldNotBeEmpty()
        model.unusedTypes.size shouldBeEqualTo 1
        model.unusedTypes[0].name shouldBeEqualTo "StandsAlone"
    }

    @Test
    fun `should not crash when parsing class with javadoc on package declaration`() {
        val types = ParseJava().parse("src/test/resources/PackageComment.java");

        val model = ModelBuilder().build(types)

        model.unusedTypes.shouldNotBeEmpty()
        model.unusedTypes.size shouldBeEqualTo 1
    }

    @Test
    fun `should parse annotations`() {
        val types = ParseJava().parse("src/test/resources/annotations/JavaAnnotation.java")
        val model = ModelBuilder().build(types)

        types.size shouldBeEqualTo 1

        println(model.types)
    }

}