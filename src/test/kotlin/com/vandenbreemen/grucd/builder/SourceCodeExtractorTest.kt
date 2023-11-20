package com.vandenbreemen.grucd.builder

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class SourceCodeExtractorTest {

    @Test
    fun `should parse a bunch of code`() {
        val extractor = SourceCodeExtractor()
        val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/")
        val model = extractor.buildModelWithFiles(fileNames)

        println(model)
    }

    @Test
    fun `should filter for annotated classes`() {
        val extractor = SourceCodeExtractor().filterForAnnotationType("MyAnnotation")
        val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/kotlin/")
        val model = extractor.buildModelWithFiles(fileNames)

        model.types.filter { t->t.name == "KotlinClass" }.shouldBeEmpty()
        model.types.filter { t->t.name == "ClassWithAnnotation" }.shouldNotBeEmpty()
    }

}