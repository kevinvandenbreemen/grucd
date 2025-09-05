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
    fun `store and retrieve multiple types by filename`() {
        val types = listOf("TypeA", "TypeB", "TypeC")
        val filename = "file1.kt"
        val md5 = "abc123"
        repo.store(types, filename, md5)
        val result = repo.getTypesByFilename(filename)
        assertEquals(types, result)
    }

    @Test
    fun `store and retrieve single type by filename`() {
        val types = listOf("MyType")
        val filename = "file2.kt"
        val md5 = "def456"
        repo.store(types, filename, md5)
        val result = repo.getTypesByFilename(filename)
        assertEquals(types, result)
    }

    @Test
    fun `returns null for unknown filename`() {
        val result = repo.getTypesByFilename("nonexistent.kt")
        assertNull(result)
    }
}
