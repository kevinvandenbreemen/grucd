package com.vandenbreemen.grucd.parse.interactor

import com.vandenbreemen.grucd.model.Visibility
import com.vandenbreemen.grucd.model.TypeType
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

}