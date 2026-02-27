package batch

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileDiscoveryTest {

    @Test
    fun `finds sml files in directory`() {
        val tempDir = createTempDirectory("file-discovery-test").toFile()
        try {
            tempDir.resolve("a.sml").writeText("val a = 1")
            tempDir.resolve("b.sml").writeText("val b = 2")
            tempDir.resolve("c.txt").writeText("not sml")

            val result = FileDiscovery.findSmlFiles(tempDir)

            assertEquals(2, result.size, "Should find exactly 2 .sml files")
            assertTrue(result.all { it.extension == "sml" }, "All results should be .sml files")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `finds sml files recursively`() {
        val tempDir = createTempDirectory("file-discovery-recursive-test").toFile()
        try {
            tempDir.resolve("top.sml").writeText("val top = 1")
            val subDir = tempDir.resolve("sub").also { it.mkdir() }
            subDir.resolve("nested.sml").writeText("val nested = 2")
            subDir.resolve("deep.sml").writeText("val deep = 3")

            val result = FileDiscovery.findSmlFiles(tempDir)

            assertEquals(3, result.size, "Should find all 3 .sml files including subdirectory")
            assertTrue(result.all { it.extension == "sml" }, "All results should be .sml files")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `returns empty list for directory with no sml files`() {
        val tempDir = createTempDirectory("file-discovery-no-sml-test").toFile()
        try {
            tempDir.resolve("a.txt").writeText("text file")
            tempDir.resolve("b.kt").writeText("kotlin file")

            val result = FileDiscovery.findSmlFiles(tempDir)

            assertEquals(0, result.size, "Should return empty list when no .sml files exist")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `returns empty list for empty directory`() {
        val tempDir = createTempDirectory("file-discovery-empty-test").toFile()
        try {
            val result = FileDiscovery.findSmlFiles(tempDir)

            assertEquals(0, result.size, "Should return empty list for empty directory")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `returns absolute paths`() {
        val tempDir = createTempDirectory("file-discovery-absolute-test").toFile()
        try {
            tempDir.resolve("example.sml").writeText("val x = 1")

            val result = FileDiscovery.findSmlFiles(tempDir)

            assertEquals(1, result.size, "Should find exactly 1 .sml file")
            assertTrue(result.all { it.isAbsolute }, "All returned paths should be absolute")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
