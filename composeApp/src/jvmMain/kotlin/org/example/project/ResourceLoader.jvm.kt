package org.example.project

actual open class ResourceLoader {
    actual open suspend fun loadCertificateResource(fileName: String): ByteArray {
        TODO("Not yet implemented")
    }
}