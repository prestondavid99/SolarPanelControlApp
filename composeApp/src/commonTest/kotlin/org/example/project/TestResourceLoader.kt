package org.example.project

import com.goncalossilva.resources.Resource
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.parentPath

class TestResourceLoader : ResourceLoader() {
    override suspend fun loadCertificateResource(fileName: String): ByteArray {
        val filePath = "src/commonTest/resources/$fileName"
        return Resource(filePath).readBytes()
    }
}