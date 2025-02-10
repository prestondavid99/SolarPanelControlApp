package org.example.project

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.io.IOException
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual suspend fun loadCertificateResource(fileName: String): ByteArray {
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex <= 0 || dotIndex == fileName.length - 1) {
        throw IllegalArgumentException("Invalid file name: $fileName. The file name must include an extension.")
    }
    val name = fileName.substring(0, dotIndex)
    val ext = fileName.substring(dotIndex + 1)
    println("Attempting to load: name=$name, ext=$ext")

    // Get the path for the full filename (name + extension)
    val bundlePath = NSBundle.mainBundle.pathForResource(name, ext)
        ?: throw IOException("Certificate $fileName not found")

    val data = NSData.dataWithContentsOfFile(bundlePath)
        ?: throw IOException("Failed to load data from $bundlePath")

    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            data.getBytes(pinned.addressOf(0), data.length)
        }
    }
}