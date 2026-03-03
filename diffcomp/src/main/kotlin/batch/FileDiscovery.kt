package batch

import java.io.File

/**
 * Discovers .sml files within a directory tree.
 */
object FileDiscovery {
    /**
     * Recursively walks [directory] and returns all .sml files found.
     *
     * Returns an empty list if the directory is empty or contains no .sml files. All returned [File] instances have
     * absolute paths.
     */
    fun findSmlFiles(directory: File): List<File> =
        directory.absoluteFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "sml" }
            .toList()
}
