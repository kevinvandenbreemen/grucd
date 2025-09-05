package com.vandenbreemen.grucd.cache.repository

import com.vandenbreemen.grucd.cache.model.ParsedTypeDocument
import org.dizitart.no2.Nitrite
import org.dizitart.no2.objects.ObjectRepository
import org.dizitart.no2.objects.filters.ObjectFilters

class ModelPreviouslyParsedRepository {
    private val db: Nitrite = Nitrite.builder()
        .filePath("model-previously-parsed.db")
        .openOrCreate()
    private val repository: ObjectRepository<ParsedTypeDocument> = db.getRepository(ParsedTypeDocument::class.java)

    fun store(types: List<String>, filename: String, md5: String) {
        val doc = ParsedTypeDocument(filename, types, md5)
        repository.update(doc, true) // upsert
    }

    fun getTypesByFilename(filename: String): List<String>? {
        val doc = repository.find(ObjectFilters.eq("filename", filename)).firstOrNull()
        return doc?.types
    }

    fun getDocumentByFilename(filename: String): ParsedTypeDocument? {
        return repository.find(ObjectFilters.eq("filename", filename)).firstOrNull()
    }

    fun deleteByFilename(filename: String) {
        repository.remove(ObjectFilters.eq("filename", filename))
    }

    fun close() {
        db.close()
    }
}