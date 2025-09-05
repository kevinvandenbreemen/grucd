package com.vandenbreemen.grucd.cache.model

import com.vandenbreemen.grucd.model.Type
import org.dizitart.no2.objects.Id

class ParsedTypeDocument() {
    @Id
    var filename: String = ""
    var types: List<Type> = emptyList()
    var md5: String = ""

    constructor(filename: String, types: List<Type>, md5: String) : this() {
        this.filename = filename
        this.types = types
        this.md5 = md5
    }
}