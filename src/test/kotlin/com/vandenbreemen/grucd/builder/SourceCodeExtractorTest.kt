package com.vandenbreemen.grucd.builder

import com.strumenta.kotlinmultiplatform.Type
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class SourceCodeExtractorTest {

    val dynamicFilePath = "./src/test/resources/kotlin/localupdate"
    val dynamicFileName = "LocalUpdate.kt"

    private var extractor: SourceCodeExtractor? = null

    @AfterEach
    fun tearDown() {
        //  Remove the temporary file
        val file = java.io.File("$dynamicFilePath/$dynamicFileName")
        if (file.exists()) {
            file.delete()
        }
        extractor?.close()
        extractor = null
    }

    @Test
    fun `should parse a bunch of code`() {
        SourceCodeExtractor().use { extractor ->
            val fileNames = extractor!!.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/")
            val model = extractor!!.buildModelWithFiles(fileNames)
            println(model)
        }



    }

    @Test
    fun `should filter for annotated classes`() {
        SourceCodeExtractor().filterForAnnotationType("MyAnnotation").use { extractor ->
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/kotlin/")
            val model = extractor.buildModelWithFiles(fileNames)

            model.types.filter { t->t.name == "KotlinClass" }.shouldBeEmpty()
            model.types.filter { t->t.name == "ClassWithAnnotation" }.shouldNotBeEmpty()
        }
    }

    @Test
    fun `should provide a way to give user the ability to filter types`() {
        SourceCodeExtractor().filterEachType { type ->
            type.annotations.any { a -> a.typeName == "MyAnnotation" }
        }.use { extractor ->
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/kotlin/")
            val model = extractor.buildModelWithFiles(fileNames)

            model.types.filter { t->t.name == "KotlinClass" }.shouldBeEmpty()
            model.types.filter { t->t.name == "ClassWithAnnotation" }.shouldNotBeEmpty()
        }
    }

    @Test
    fun `should be able to find all classes referencing a specified class name`() {
        SourceCodeExtractor().use { extractor ->
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/main/java")
            val model = extractor.buildModelWithFiles(fileNames)

            val typesMatching = model.typesWithName("Type")
            typesMatching.shouldNotBeEmpty()

            val typesAtDeg2 = mutableListOf<Type>()
            typesMatching.forEach { type->
                println(model.getTypesReferencingOrReferencedBy(type, 2))
            }
        }
    }

    @Test
    fun `should provide a way to cache unchanged files and detect changes in other files`() {
        SourceCodeExtractor().use { extractor ->
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

            //  Sanity test similar contents between models
            model.typesWithName("ClassWithListOfEncapsulatedDeclared")[0].fields.size shouldBeEqualTo 1
            newModel.typesWithName("ClassWithListOfEncapsulatedDeclared")[0].fields.size shouldBeEqualTo 1

            model.types.size shouldBeEqualTo newModel.types.size
        }
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

        //  Create the file first?
        if (!file.exists()) {
            file.parentFile.mkdirs() // Ensure the directory exists
            file.createNewFile() // Create the file
        }

        file.writeText(fakeCode)
    }

}