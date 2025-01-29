package org.example.project

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.readBytes
import io.matthewnelson.kmp.file.readUtf8
import kotlin.test.Test
import kotlin.test.assertTrue

class FilePrintoutTest {

    @Test
    fun testPrintFileContents() {
        // Specify the relative path to the file from the test file's location
        val filePath = "../test.txt"

        // Read the file
        val file = File(filePath)

        // Check if the file exists
        assertTrue(file.exists(), "File does not exist at path: $filePath")

        // Print the contents of the file
        file.readBytes()
        val utf8: String = file.readUtf8()
        println(utf8)
    }
}