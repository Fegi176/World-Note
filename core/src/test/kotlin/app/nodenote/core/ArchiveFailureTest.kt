package app.nodenote.core

import java.io.*
import java.util.zip.*
import org.junit.Assert.*
import org.junit.Test

class ArchiveFailureTest {
    @Test
    fun compressedExpansionLimitRejectsAndRemovesStaging() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val zip = File(root, "expansion.zip")
            ZipOutputStream(zip.outputStream()).use { out ->
                out.putNextEntry(ZipEntry("oversized.json"))
                val chunk = ByteArray(65536)
                repeat((Archives.MAX_FILE / chunk.size).toInt()) { out.write(chunk) }
                out.write(0)
                out.closeEntry()
            }
            assertTrue(zip.length() < 100000)
            val stage = File(root, "stage")
            val failure =
                assertThrows(IllegalArgumentException::class.java) { Archives.inspect(zip, stage) }
            assertTrue(failure.message!!.contains("expansion limit"))
            assertFalse(stage.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun truncatedArchiveCorpusNeverProducesAPartialWorkspace() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val complete =
                ByteArrayOutputStream()
                    .also {
                        Archives.write(
                            Workspace(listOf(Demo.create())),
                            it,
                            assets = { error("No assets") },
                        )
                    }
                    .toByteArray()
            val random = kotlin.random.Random(1903)
            repeat(24) { i ->
                val file = File(root, "cut-$i.zip")
                file.writeBytes(complete.copyOf(random.nextInt(0, complete.size - 1)))
                val stage = File(root, "stage-$i")
                assertTrue(runCatching { Archives.inspect(file, stage) }.isFailure)
                assertFalse(stage.exists())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun jsonDepthAndTruncationRejectedWhileBracesInProseAreSafe() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedJson("[".repeat(129) + "0" + "]".repeat(129))
        }
        assertThrows(IllegalArgumentException::class.java) { boundedJson("{\"body\":\"unfinished") }
        assertEquals("{\"body\":\"{{{[[[\"}", boundedJson("{\"body\":\"{{{[[[\"}"))
    }

    @Test
    fun unixSymlinkAttributesRejected() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val zip = File(root, "link.zip")
            ZipOutputStream(zip.outputStream()).use { z ->
                z.putNextEntry(ZipEntry("link"))
                z.write("target".toByteArray())
                z.closeEntry()
            }
            val bytes = zip.readBytes()
            val pos =
                (0..bytes.size - 46).first { i ->
                    bytes[i] == 0x50.toByte() &&
                        bytes[i + 1] == 0x4b.toByte() &&
                        bytes[i + 2] == 1.toByte() &&
                        bytes[i + 3] == 2.toByte()
                }
            bytes[pos + 40] = 0xff.toByte()
            bytes[pos + 41] = 0xa1.toByte()
            zip.writeBytes(bytes)
            assertThrows(IllegalArgumentException::class.java) {
                Archives.inspect(zip, File(root, "stage"))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateCaseFoldedPathsAreRejected() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val zip = File(root, "bad.zip")
            ZipOutputStream(zip.outputStream()).use { z ->
                listOf("A.json", "a.json").forEach {
                    z.putNextEntry(ZipEntry(it))
                    z.write("x".toByteArray())
                    z.closeEntry()
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                Archives.inspect(zip, File(root, "stage"))
            }
            assertFalse(File(root, "stage").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun futureManifestFailsBeforeMutation() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val zip = File(root, "future.zip")
            ZipOutputStream(zip.outputStream()).use { z ->
                z.putNextEntry(ZipEntry("manifest.json"))
                z.write(
                    """{"format":"nodenote-worldbuilder","version":99,"worlds":[],"files":[]}"""
                        .toByteArray()
                )
                z.closeEntry()
            }
            val ex =
                assertThrows(IllegalArgumentException::class.java) {
                    Archives.inspect(zip, File(root, "stage"))
                }
            assertTrue(ex.message!!.contains("Unsupported"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun outputDiskFullFailureIsNotSuccess() {
        val b = Demo.create()
        val out =
            object : OutputStream() {
                var bytes = 0

                override fun write(value: Int) {
                    if (++bytes > 200) throw IOException("Simulated ENOSPC")
                }
            }
        assertThrows(IOException::class.java) {
            Archives.write(Workspace(listOf(b)), out, { error("none") })
        }
    }

    @Test
    fun allUnsafeNamesRejected() {
        listOf("../x", "/absolute", "C:/drive", "a\\b", "a/../b", "a//b", "a/./b", ".", "a\u0000b")
            .forEach { name ->
                assertThrows(IllegalArgumentException::class.java) { Archives.safePath(name) }
            }
    }

    @Test
    fun checksumMismatchRejected() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val zip = File(root, "hash.zip")
            ZipOutputStream(zip.outputStream()).use { z ->
                z.putNextEntry(ZipEntry("manifest.json"))
                z.write(
                    """{"worlds":["x"],"files":[{"path":"payload.json","bytes":1,"sha256":"bad"}]}"""
                        .toByteArray()
                )
                z.closeEntry()
                z.putNextEntry(ZipEntry("payload.json"))
                z.write("x".toByteArray())
                z.closeEntry()
            }
            val error =
                assertThrows(IllegalArgumentException::class.java) {
                    Archives.inspect(zip, File(root, "stage"))
                }
            assertTrue(error.message!!.contains("Checksum"))
        } finally {
            root.deleteRecursively()
        }
    }
}
