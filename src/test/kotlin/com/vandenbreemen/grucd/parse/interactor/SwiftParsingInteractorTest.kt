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


}