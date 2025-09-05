package com.vandenbreemen.grucd.cache.model

import org.dizitart.no2.objects.Id

class ParsedTypeDocument() {
    @Id
    var filename: String = ""
    var type: String = ""
    var md5: String = ""

    constructor(filename: String, type: String, md5: String) : this() {
        this.filename = filename
        this.type = type
        this.md5 = md5
    }
}