package com.vandenbreemen.grucd.builder

import com.vandenbreemen.grucd.cache.interactor.ModelPreviouslyParsedInteractor
import com.vandenbreemen.grucd.cache.repository.ModelPreviouslyParsedRepository
import com.vandenbreemen.grucd.model.Model
import com.vandenbreemen.grucd.model.Type
import com.vandenbreemen.grucd.parse.ParseJava
import com.vandenbreemen.grucd.parse.ParseKotlin
import com.vandenbreemen.grucd.parse.interactor.SwiftParsingInteractor
import org.apache.log4j.Logger
import java.io.Closeable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory

/**
 * Scours a file or directory for files to parse
 */
class SourceCodeExtractor: Closeable {

    companion object {

        private val logger = Logger.getLogger(SourceCodeExtractor::class.java)

    }

    /**
     * General filtering logic
     */
    private val filters = mutableListOf<(Type)->Boolean>()

    private val cacheInteractor = ModelPreviouslyParsedInteractor(
        ModelPreviouslyParsedRepository()
    )

    /**
     * Adds the given annotation name to the list of annotations that will be filtered for.  Note that any non-annotated
     * types will no longer be included in the generated model
     */
    fun filterForAnnotationType(annotationName: String): SourceCodeExtractor {
        filterEachType { type->
            type.annotations.any { annotation ->
                annotation.typeName == annotationName
            }
        }
        return this
    }

    /**
     * Given an input file or an input directory returns appropriate list of files to parse
     * @return  List of files to parse through
     */
    fun getFilenamesToVisit(inputFile: String?, inputDir: String): List<String> {
        val filesToVisit: MutableList<String> = ArrayList()
        if (inputFile != null) {
            logger.info("Parsing single file '$inputFile'")
            filesToVisit.add(inputFile)
        } else {
            logger.info("Parsing directory $inputDir")
            try {
                Files.walk(Paths.get(inputDir)).filter { filePath: Path ->
                    filePath.fileName.toString().endsWith(".java") && !filePath.isDirectory()
                }.forEach { path: Path ->
                    logger.debug("path (java)=" + path.toFile().absolutePath)
                    filesToVisit.add(path.toFile().absolutePath)
                }
                Files.walk(Paths.get(inputDir)).filter { filePath: Path ->
                    filePath.fileName.toString().endsWith(".kt") && !filePath.isDirectory()
                }.forEach { path: Path ->
                    logger.debug("path (kotlin)=" + path.toFile().absolutePath)
                    filesToVisit.add(path.toFile().absolutePath)
                }
                Files.walk(Paths.get(inputDir)).filter { filePath: Path ->
                    filePath.fileName.toString().endsWith(".swift") && !filePath.isDirectory()
                }.forEach { path: Path ->
                    logger.debug("path (swift)=" + path.toFile().absolutePath)
                    filesToVisit.add(path.toFile().absolutePath)
                }
            } catch (ioe: IOException) {
                logger.error("Failed to get files to parse", ioe)
            }
        }
        return filesToVisit
    }

    /**
     * Given the list of files to visit generates a [Model] by parsing these files
     */
    fun buildModelWithFiles(filesToVisit: List<String>): Model {
        return doVisitSpecificFiles(filesToVisit, false)
    }

    private fun doVisitSpecificFiles(filesToVisit: List<String>, useCachedTypes: Boolean): Model {
        val java = ParseJava()
        val kotlin = ParseKotlin()
        val swiftParser = SwiftParsingInteractor()

        val allTypes: MutableList<Type> = ArrayList<Type>()

        // Collect Swift files to parse together (to merge extensions with base types)
        val swiftFiles = mutableListOf<String>()

        filesToVisit.forEach { file ->
            val cachedTypes = cacheInteractor.getValidCachedTypeForFile(file)
            val parsedTypes: List<Type> = if (
                useCachedTypes && cachedTypes != null
            ) {
                cachedTypes
            } else {
                when {
                    file.endsWith(".java") -> java.parse(file) ?: emptyList()
                    file.endsWith(".kt") -> kotlin.parse(file)
                    file.endsWith(".swift") -> {
                        // Defer Swift parsing; just collect the file
                        swiftFiles.add(file)
                        emptyList()
                    }
                    else -> emptyList()
                }
            }
            allTypes.addAll(parsedTypes)

            cacheInteractor.storeFileTypesWithChecksum(
                file,
                parsedTypes
            )
        }


        //  Annotation filter
        val filtered = if(filters.isEmpty()) allTypes else allTypes.filter { type->
            if(filters.any { filter -> !filter(type) }) {
                return@filter false
            }

            true
        }

        val modelBuilder = ModelBuilder()
        return modelBuilder.build(filtered)
    }

    fun filterEachType(filter: (Type)->Boolean): SourceCodeExtractor {
        this.filters.add(filter)
        return this
    }

    /**
     * @return  Updated copy of the model with the changes from the files in the input directory
     */
    fun updateModelWithFileChanges(inputDir: String): Model {

        //  Get list of files to visit
        val filesToVisit = getFilenamesToVisit(inputFile = null, inputDir = inputDir)
        return doVisitSpecificFiles(filesToVisit, useCachedTypes = true)

    }

    override fun close() {
        cacheInteractor.close()
    }

}