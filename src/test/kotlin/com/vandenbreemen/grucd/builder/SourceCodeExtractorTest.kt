package com.vandenbreemen.grucd.builder

import com.strumenta.kotlinmultiplatform.Type
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

class SourceCodeExtractorTest {

    val dynamicFilePath = "./src/test/resources/kotlin/localupdate"
    val dynamicFileName = "LocalUpdate.kt"

    @AfterEach
    fun tearDown() {
        //  Delete the type file if it exists
        File("model-previously-parsed.db").delete()
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

    @Test
    fun `should include Swift files when building model`() {
        SourceCodeExtractor().use { extractor ->
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = "src/test/resources/swift")

            // Ensure .swift files are detected
            fileNames.shouldNotBeEmpty()
            assert(fileNames.any { it.endsWith(".swift") })

            val model = extractor.buildModelWithFiles(fileNames)

            // Ensure Swift types are parsed into the model
            model.typesWithName("Car").shouldNotBeEmpty()
            model.typesWithName("Drivable").shouldNotBeEmpty()
            model.typesWithName("Person").shouldNotBeEmpty()
        }
    }

    @Test
    fun `should not register the same type twice when parsing multiple files`() {
        SourceCodeExtractor().use { extractor ->
            val directory = "src/test/resources/swift"
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = directory)
            val model = extractor.buildModelWithFiles(fileNames)
            //  There should only be one Car type
            model.types.filter { t->t.name == "Drawable" }.size shouldBeEqualTo 1
        }
    }

    @Test
    fun `should get consistent results when dealing with types with extensions`() {

        val model1 = SourceCodeExtractor().use { extractor ->
            val directory = "src/test/resources/swift"
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = directory)
            extractor.buildModelWithFiles(fileNames)
        }

        val model2 = SourceCodeExtractor().use { extractor ->
            val directory = "src/test/resources/swift"
            val fileNames = extractor.getFilenamesToVisit(inputFile = null, inputDir = directory)
            extractor.buildModelWithFiles(fileNames)
        }

        //  Validate drawable looks the same
        val drawable1 = model1.typesWithName("Drawable").first()
        val drawable2 = model2.typesWithName("Drawable").first()
        //  Compare fields
        drawable1.fields.size shouldBeEqualTo drawable2.fields.size
        drawable1.fields.forEach { field1 ->
            val field2 = drawable2.fields.firstOrNull { f ->
                f.name == field1
                    .name
            }
            field2.shouldNotBeNull()
            field1.typeName shouldBeEqualTo field2!!.typeName
        }

        //  Compare methods
        drawable1.methods.size shouldBeEqualTo drawable2.methods.size
        drawable1.methods.forEach { method1 ->
            val method2 = drawable2.methods.firstOrNull { m -> m.name == method1.name }
            method2.shouldNotBeNull()
            method1.returnType shouldBeEqualTo method2!!.returnType
            method1.parameters.size shouldBeEqualTo method2.parameters.size
            method1.parameters.forEachIndexed { idx, param1 ->
                val param2 = method2.parameters[idx]
                param1.name shouldBeEqualTo param2.name
                param1.typeName shouldBeEqualTo param2.typeName
            }
        }
    }

}