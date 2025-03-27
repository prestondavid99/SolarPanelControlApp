package org.example.project

expect open class ResourceLoader() {
    open suspend fun loadCertificateResource(fileName: String): ByteArray
}
