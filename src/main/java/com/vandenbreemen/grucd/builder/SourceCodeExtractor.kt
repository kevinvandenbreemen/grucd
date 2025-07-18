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

internal data class FileAssociatedChecksumAndTypes (
    val checksum: String,
    val types: List<Type>
)

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
     * Mapping from file absolute path to its checksum.  This is used to detect changes in files
     */
    private var fileChecksums: MutableMap<String, FileAssociatedChecksumAndTypes>? = null

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

    private fun calculateMd5(filePath: String): String {
        val file = java.io.File(filePath)
        if (!file.exists()) return ""
        val md = java.security.MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Given the list of files to visit generates a [Model] by parsing these files
     */
    fun buildModelWithFiles(filesToVisit: List<String>): Model {
        val java = ParseJava()
        val kotlin = ParseKotlin()

        val allTypes: MutableList<Type> = ArrayList<Type>()

        filesToVisit.forEach{ file ->
            val parsedTypes: List<Type> = when {
                file.endsWith(".java") -> java.parse(file) ?: emptyList()
                file.endsWith(".kt") -> kotlin.parse(file)
                else -> emptyList()
            }
            allTypes.addAll(parsedTypes)

            fileChecksums?.let {
                if (it.containsKey(file)) {
                    val old = it[file]
                    it[file] = old!!.copy(types = parsedTypes)
                } else {
                    it[file] = FileAssociatedChecksumAndTypes(
                        checksum = calculateMd5(file),
                        types = parsedTypes.toMutableList()
                    )
                }
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

    fun detectFileDeltas(): SourceCodeExtractor {
        fileChecksums = mutableMapOf()
        return this
    }

    fun updateModelWithFileChanges(
        model: Model,
        inputDir: String) {
        TODO("Not yet implemented")
    }


}