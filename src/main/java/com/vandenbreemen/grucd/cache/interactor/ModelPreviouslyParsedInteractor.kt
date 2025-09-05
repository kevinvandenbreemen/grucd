package com.vandenbreemen.grucd.cache.interactor

import com.vandenbreemen.grucd.cache.repository.ModelPreviouslyParsedRepository
import com.vandenbreemen.grucd.model.Type
import java.io.File
import java.security.MessageDigest

class ModelPreviouslyParsedInteractor(private val repository: ModelPreviouslyParsedRepository) {
    /**
     * Returns the cached type for the given file path if the cache is valid (md5 matches).
     * If the cache is invalid, deletes the cache entry and returns null.
     */
    fun getValidCachedTypeForFile(filePath: String): List<Type>? {
        val doc = repository.getDocumentByFilename(filePath) ?: return null
        val file = File(filePath)
        if (!file.exists()) {
            repository.deleteByFilename(filePath)
            return null
        }
        val currentMd5 = file.inputStream().use { input ->
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
        return if (currentMd5 == doc.md5) {
            doc.types
        } else {
            repository.deleteByFilename(filePath)
            null
        }
    }

    /**
     * Stores the given filename and its associated types, along with the current MD5 checksum of the file.
     */
    fun storeFileTypesWithChecksum(filePath: String, types: List<Type>) {
        val file = File(filePath)
        if (!file.exists()) return
        val md5 = file.inputStream().use { input ->
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
        repository.store(types, filePath, md5)
    }

    fun close() {
        repository.close()
    }
}