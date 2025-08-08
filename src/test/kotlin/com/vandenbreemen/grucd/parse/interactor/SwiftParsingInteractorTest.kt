package com.vandenbreemen.grucd.parse.interactor

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


}