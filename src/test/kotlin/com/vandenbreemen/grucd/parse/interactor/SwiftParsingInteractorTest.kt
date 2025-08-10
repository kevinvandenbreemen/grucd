package com.vandenbreemen.grucd.parse.interactor

import com.vandenbreemen.grucd.model.Visibility
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
        assertTrue(carType.fields.any { it.name == "make" && it.visibility == Visibility.Public })
    }


}