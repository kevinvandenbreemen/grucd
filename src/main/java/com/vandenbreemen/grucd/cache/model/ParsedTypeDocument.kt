package com.vandenbreemen.grucd.cache.model

import org.dizitart.no2.objects.Id

class ParsedTypeDocument() {
    @Id
    var filename: String = ""
    var types: List<String> = emptyList()
    var md5: String = ""

    constructor(filename: String, types: List<String>, md5: String) : this() {
        this.filename = filename
        this.types = types
        this.md5 = md5
    }
}