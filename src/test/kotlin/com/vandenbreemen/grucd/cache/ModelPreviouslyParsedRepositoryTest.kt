package com.vandenbreemen.grucd.cache

import com.vandenbreemen.grucd.cache.repository.ModelPreviouslyParsedRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ModelPreviouslyParsedRepositoryTest {
    private lateinit var repo: ModelPreviouslyParsedRepository

    @BeforeEach
    fun setUp() {
        // Remove any existing db file for clean test
        File("model-previously-parsed.db").delete()
        repo = ModelPreviouslyParsedRepository()
    }

    @AfterEach
    fun tearDown() {
        repo.close()
        File("model-previously-parsed.db").delete()
    }

    @Test
    fun `store and retrieve type by filename`() {
        val type = "MyType"
        val filename = "file1.kt"
        val md5 = "abc123"
        repo.store(type, filename, md5)
        val result = repo.getTypeByFilename(filename)
        assertEquals(type, result)
    }

    @Test
    fun `returns null for unknown filename`() {
        val result = repo.getTypeByFilename("nonexistent.kt")
        assertNull(result)
    }
}

