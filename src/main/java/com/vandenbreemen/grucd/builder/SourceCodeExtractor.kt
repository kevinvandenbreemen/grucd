package com.vandenbreemen.grucd.builder

import com.vandenbreemen.grucd.model.Model
import com.vandenbreemen.grucd.model.Type
import com.vandenbreemen.grucd.parse.ParseJava
import com.vandenbreemen.grucd.parse.ParseKotlin
import org.apache.log4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory

/**
 * Scours a file or directory for files to parse
 */
class SourceCodeExtractor {

    companion object {

        private val logger = Logger.getLogger(SourceCodeExtractor::class.java)

    }

    /**
     * General filtering logic
     */
    private val filters = mutableListOf<(Type)->Boolean>()

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
        val java = ParseJava()
        val kotlin = ParseKotlin()

        val allTypes: MutableList<Type> = ArrayList<Type>()

        filesToVisit.forEach{ file ->
            if (file.endsWith(".java")) {
                java.parse(file) ?.let { parseResult->
                    allTypes.addAll(parseResult)
                }

            } else if (file.endsWith(".kt")) {
                allTypes.addAll(kotlin.parse(file))
            }
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


}