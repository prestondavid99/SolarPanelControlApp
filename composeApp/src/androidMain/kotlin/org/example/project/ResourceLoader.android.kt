package org.example.project

import org.jetbrains.compose.resources.ExperimentalResourceApi
import solarpanelcontrolapp.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
actual open class ResourceLoader {
    actual open suspend fun loadCertificateResource(fileName: String): ByteArray {
        return Res.readBytes("files/certs/$fileName")
    }
}