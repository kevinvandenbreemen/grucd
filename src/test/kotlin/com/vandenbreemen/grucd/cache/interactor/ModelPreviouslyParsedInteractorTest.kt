package com.vandenbreemen.grucd.cache.interactor

import com.vandenbreemen.grucd.cache.repository.ModelPreviouslyParsedRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest

class ModelPreviouslyParsedInteractorTest {
    private lateinit var repo: ModelPreviouslyParsedRepository
    private lateinit var interactor: ModelPreviouslyParsedInteractor
    private val testFile = File("testfile.txt")

    @BeforeEach
    fun setUp() {
        File("model-previously-parsed.db").delete()
        repo = ModelPreviouslyParsedRepository()
        interactor = ModelPreviouslyParsedInteractor(repo)
        testFile.writeText("original content")
    }

    @AfterEach
    fun tearDown() {
        repo.close()
        File("model-previously-parsed.db").delete()
        testFile.delete()
    }

    private fun md5Of(file: File): String {
        return file.inputStream().use { input ->
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    }

    @Test
    fun `returns cached type if md5 matches`() {
        val type = "MyType"
        val md5 = md5Of(testFile)
        repo.store(type, testFile.path, md5)
        val result = interactor.getValidCachedTypeForFile(testFile.path)
        assertEquals(type, result)
    }

    @Test
    fun `returns null and deletes cache if md5 does not match`() {
        val type = "MyType"
        val md5 = md5Of(testFile)
        repo.store(type, testFile.path, md5)
        testFile.writeText("changed content")
        val result = interactor.getValidCachedTypeForFile(testFile.path)
        assertNull(result)
        assertNull(repo.getDocumentByFilename(testFile.path))
    }

    @Test
    fun `returns null and deletes cache if file does not exist`() {
        val type = "MyType"
        val md5 = md5Of(testFile)
        repo.store(type, testFile.path, md5)
        testFile.delete()
        val result = interactor.getValidCachedTypeForFile(testFile.path)
        assertNull(result)
        assertNull(repo.getDocumentByFilename(testFile.path))
    }
}

