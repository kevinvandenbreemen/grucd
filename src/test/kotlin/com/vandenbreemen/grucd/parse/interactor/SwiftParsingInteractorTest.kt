package com.vandenbreemen.grucd.parse.interactor

import com.vandenbreemen.grucd.model.Visibility
import com.vandenbreemen.grucd.model.TypeType
import org.amshove.kluent.should
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.apache.log4j.Logger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SwiftParsingInteractorTest {

    companion object {
        val logger = Logger.getLogger(SwiftParsingInteractorTest::class.java)
    }

    @Test
    fun `should parse a swift class`() {
        val parser = SwiftParsingInteractor()
        val rsult = parser.parse(
            "src/test/resources/swift/Car.swift"
        )

        logger.info("Result: $rsult")

        rsult.shouldNotBeEmpty()
    }

    @Test
    fun `should parse fields in a swift class`() {
        val parser = SwiftParsingInteractor()
        val result = parser.parse(
            "src/test/resources/swift/Car.swift"
        )

        logger.info("Result: $result")

        assertTrue(result.any { it.name == "Car" })
        val carType = result.first { it.name == "Car" }
        assertTrue(carType.fields.isNotEmpty())
        assertTrue(carType.fields.any { it.name == "make" && it.visibility == Visibility.Internal })
    }

    @Test    //  Now to handle grabbing protocols
    fun `should parse protocols in a swift class`() {
        val parser = SwiftParsingInteractor()
        val result = parser.parse(
            "src/test/resources/swift/Drivable.swift"
        )

        logger.info("Result: $result")

        assertTrue(result.any { it.name == "Drivable" && it.type == TypeType.Interface })
        val proto = result.first { it.name == "Drivable" }
        assertTrue(proto.methods.any { it.name == "start" })
        assertTrue(proto.methods.any { it.name == "stop" })
        assertTrue(proto.fields.any { it.name == "make" })
    }

    @Test
    fun `should parse a swift struct`() {
        val parser = SwiftParsingInteractor()
        val result = parser.parse(
            "src/test/resources/swift/StructExample/Person.swift"
        )

        logger.info("Result: $result")

        assertTrue(result.any { it.name == "Person" })
        val person = result.first { it.name == "Person" }
        assertEquals(TypeType.Struct, person.type)
        assertTrue(person.fields.any { it.name == "fullName" })
    }

    @Test
    fun `should detect class reference via field initializer`() {
        val parser = SwiftParsingInteractor()
        val result = parser.parse(
            "src/test/resources/swift/TypeReferencingExamples/DataImporter.swift"
        )

        logger.info("Result: $result")

        // Both classes should be discovered
        assertTrue(result.any { it.name == "DataImporter" })
        assertTrue(result.any { it.name == "DataManager" })

        // DataManager should have a field `importer` of type DataImporter
        val dataManagerTypes = result.filter { it.name == "DataManager" }
        assertTrue(
            dataManagerTypes.any { t -> t.fields.any { f -> f.name == "importer" && f.typeName == "DataImporter" } },
            "Expected DataManager to have a field 'importer' of type 'DataImporter'"
        )
    }

    @Test
    fun `should not parse local variables as fields in swift`() {
        val parser = SwiftParsingInteractor()
        val result = parser.parse(
            "src/test/resources/swift/FieldOfUnknownTypeExample/SwiftFileWithFieldOfUnknownType.swift"
        )

        logger.info("Result: $result")

        assertTrue(result.any { it.name == "Square" && it.type == TypeType.Struct })
        val square = result.first { it.name == "Square" }

        // Only the struct property should be present
        assertTrue(square.fields.any { it.name == "size" })
        assertTrue(square.fields.none { it.name == "line" })
        assertTrue(square.fields.none { it.name == "result" })
    }

    @Test
    fun `should ignore typealias declarations in swift`() {
        val parser = SwiftParsingInteractor()
        val result = parser.parse(
            "src/test/resources/swift/TypeAliasExamples/TypeAliases.swift"
        )

        logger.info("Result: $result")

        // Should not include the typealias as a type
        assertFalse(result.any { it.name == "FetchUserCompletion" })

        // Should still parse the class that uses the typealias
        assertTrue(result.any { it.name == "SomethingWithAliasesStrategy" })

        //  Validate the fields in the class
        result.first { it.name == "SomethingWithAliasesStrategy" }.run {
            fields.size shouldBeEqualTo 1
            fields[0].name shouldBeEqualTo "someField"
            fields[0].typeName shouldBeEqualTo "SomeType"
        }
    }

    @Test
    fun `should merge protocol extension members into protocol`() {
        val parser = SwiftParsingInteractor()
        val files = listOf(
            "src/test/resources/swift/TypeAndExtension/DrawableExtension.swift",
            "src/test/resources/swift/TypeAndExtension/CircleExtension.swift",
            "src/test/resources/swift/TypeAndExtension/SwiftTypeToBeExtended.swift"
        )

        val result = parser.parse(files)
        logger.info("Result: $result")

        // Drawable should be an interface and include methods from the protocol and its extension
        assertTrue(result.any { it.name == "Drawable" && it.type == TypeType.Interface })
        result.filter { it.name == "Drawable" }.size shouldBeEqualTo 1
        val drawable = result.first { it.name == "Drawable" }
        assertTrue(drawable.methods.any { it.name == "draw" }, "Expected protocol method 'draw'")
        assertTrue(drawable.methods.any { it.name == "describe" }, "Expected extension method 'describe'")
    }

    @Test
    fun `should merge struct extension members into struct`() {
        val parser = SwiftParsingInteractor()
        val files = listOf(
            "src/test/resources/swift/TypeAndExtension/CircleExtension.swift",
            "src/test/resources/swift/TypeAndExtension/DrawableExtension.swift",
            "src/test/resources/swift/TypeAndExtension/SwiftTypeToBeExtended.swift"
        )

        val result = parser.parse(files)
        logger.info("Result: $result")

        // Circle should be a struct and include fields/methods from the struct and its extension
        assertTrue(result.filter { it.name == "Circle" && it.type == TypeType.Struct }.size == 1)
        val circle = result.first { it.name == "Circle" }

        // Original fields
        assertTrue(circle.fields.any { it.name == "color" })
        assertTrue(circle.fields.any { it.name == "radius" })

        // Extension computed property
        assertTrue(circle.fields.any { it.name == "diameter" && it.typeName.contains("Double") })

        // Extension methods
        assertTrue(circle.methods.any { it.name == "area" })
        assertTrue(circle.methods.any { it.name == "scale" })
    }

}