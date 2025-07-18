package com.vandenbreemen.grucd.builder

import com.strumenta.kotlinmultiplatform.Type
import com.vandenbreemen.grucd.builder.SourceCodeExtractor
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SourceCodeExtractorTest {

    val dynamicFilePath = "src/test/resources/kotlin/localupdate"
    val dynamicFileName = "LocalUpdate.kt"

    @AfterEach
    fun tearDown() {
        //  Remove the temporary file
        val file = java.io.File("$dynamicFilePath/$dynamicFileName")
        if (file.exists()) {
            file.delete()
        }
    }

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

    @Test
    fun `should provide a way to give user the ability to filter types`() {
        val extractor = SourceCodeExtractor().filterEachType { type ->
            type.annotations.any { a -> a.typeName == "MyAnnotation" }
        }
        val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/kotlin/")
        val model = extractor.buildModelWithFiles(fileNames)

        model.types.filter { t->t.name == "KotlinClass" }.shouldBeEmpty()
        model.types.filter { t->t.name == "ClassWithAnnotation" }.shouldNotBeEmpty()
    }

    @Test
    fun `should be able to find all classes referencing a specified class name`() {

        val extractor = SourceCodeExtractor()
        val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/main/java")
        val model = extractor.buildModelWithFiles(fileNames)

        val typesMatching = model.typesWithName("Type")
        typesMatching.shouldNotBeEmpty()

        val typesAtDeg2 = mutableListOf<Type>()
        typesMatching.forEach { type->
            println(model.getTypesReferencingOrReferencedBy(type, 2))
        }

    }

    @Test
    fun `should provide a way to cache unchanged files and detect changes in other files`() {
        val extractor = SourceCodeExtractor()
            //  Turn on the feature to detect file deltas
            .detectFileDeltas()

        //  Set up fake file
        writeFakeCodeToTemporaryFile()

        val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/kotlin/")

        val startTime = System.nanoTime()
        val model = extractor.buildModelWithFiles(fileNames)
        val preCacheDuration = System.nanoTime() - startTime

        val fakeClass = model.typesWithName("LocalUpdate")[0]
        fakeClass.fields.firstOrNull() { f->f.name == "newField" }.shouldBeNull()

        //  Write an update to the fake file
        writeFakeCodeToTemporaryFile( newField = "newField" )

        //  Now trigger a model update

        val startTime2 = System.nanoTime()
        val newModel = extractor.updateModelWithFileChanges(
            inputDir = "src/test/resources/kotlin/")
        val postCacheDuration = System.nanoTime() - startTime2

        val updatedFakeClass = newModel.typesWithName("LocalUpdate")[0]

        updatedFakeClass.fields.firstOrNull { f->f.name == "newField" }.shouldNotBeNull()

        //  Compare the durations
        println("Pre-cache duration: ${preCacheDuration / 1_000_000} ms")
        println("Post-cache duration: ${postCacheDuration / 1_000_000} ms")

        //  This has been observed to be on the order of magnitudes faster so should always pass
        postCacheDuration shouldBeLessThan preCacheDuration


    }

    private fun writeFakeCodeToTemporaryFile(newField: String? = null){


        //  Write some fake code to a fake class in here:
        val fakeCode = """
            package kotlin
            
            class LocalUpdate {
            
                //  This will trigger system to detect a file change
                val someRandomLong = ${System.nanoTime()}L
                
                //  Fake a new field if provided
                ${if(newField != null) "val newField: String = \"$newField\"" else ""}
            
                fun doSomething() {
                    println("Doing something")
                }
            }
        """.trimIndent()
        val file = java.io.File("$dynamicFilePath/$dynamicFileName")
        file.writeText(fakeCode)
    }

}