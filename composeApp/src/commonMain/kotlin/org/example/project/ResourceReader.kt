package org.example.project

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class ResourceReader() {
    private val fileSystem = FileSystem.SYSTEM
    fun readResource(path: String): String {
        try {
            fileSystem.read(path.toPath()) {
                // If this succeeds, the file exists and is readable
                return fileSystem.read(path.toPath()) {
                    readUtf8()
                }
            }
        } catch (e: FileNotFoundException) {
            println("File not found at: $path")
        }
        return "Operation failed"
    }
}