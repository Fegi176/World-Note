package app.nodenote.core

import java.io.*
import java.security.MessageDigest
import java.util.zip.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

fun sha256(file: File): String {
    val hash = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val b = ByteArray(65536)
        while (true) {
            val n = input.read(b)
            if (n < 0) break
            hash.update(b, 0, n)
        }
    }
    return hash.digest().joinToString("") { "%02x".format(it) }
}

@Serializable data class FileDigest(val path: String, val bytes: Long, val sha256: String)

@Serializable
data class Manifest(
    val format: String = "nodenote-worldbuilder",
    val version: Int = 1,
    val kind: String = "workspace",
    val appVersion: String = "0.2.0",
    val created: Long = System.currentTimeMillis(),
    val worlds: List<String>,
    val files: List<FileDigest>,
)

data class InspectedArchive(
    val workspace: Workspace,
    val files: Map<String, File>,
    val root: File,
    val warnings: List<String>,
)

object Archives {
    const val MAX_BYTES = 512L * 1024 * 1024
    const val MAX_FILE = 64L * 1024 * 1024
    const val MAX_FILES = 10000

    fun safePath(name: String): String {
        require(
            name.isNotBlank() &&
                name.length < 240 &&
                !name.contains('\\') &&
                !name.contains(':') &&
                !name.startsWith('/') &&
                !name.endsWith('/') &&
                name.split('/').none { it in listOf(".", "..", "") }
        ) {
            "Unsafe archive path: $name"
        }
        require(name.none { it.code < 32 }) { "Invalid archive path" }
        return name
    }

    /**
     * Writes from a coherent logical snapshot and immutable original assets. Caller commits backup
     * timestamp only after output closes.
     */
    fun write(
        workspace: Workspace,
        output: OutputStream,
        assets: (Record) -> File,
        progress: (String) -> Unit = {},
    ) {
        val blobs = linkedMapOf<String, ByteArray>()
        val files = linkedMapOf<String, File>()
        workspace.worlds.forEach { bundle ->
            Integrity.requireValid(bundle)
            blobs["worlds/${bundle.world.id}/world.json"] =
                codec.encodeToString(bundle).toByteArray()
            bundle.records
                .filter { it.kind == Kind.ATTACHMENT }
                .forEach { a ->
                    val file = assets(a)
                    require(
                        file.isFile &&
                            file.length().toString() == a.f("bytes") &&
                            sha256(file) == a.f("sha256")
                    ) {
                        "Missing/corrupt attachment: ${a.title}"
                    }
                    files["worlds/${bundle.world.id}/${a.f("path")}"] = file
                }
        }
        blobs["preferences.json"] = codec.encodeToString(workspace.preferences).toByteArray()
        val digests =
            blobs.map { FileDigest(it.key, it.value.size.toLong(), sha256(it.value)) } +
                files.map { FileDigest(it.key, it.value.length(), sha256(it.value)) }
        require(
            digests.size <= MAX_FILES &&
                digests.sumOf { it.bytes } <= MAX_BYTES &&
                digests.all { it.bytes <= MAX_FILE }
        ) {
            "Backup exceeds archive limits; split worlds"
        }
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            fun put(path: String, write: () -> Unit) {
                if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Cancelled")
                zip.putNextEntry(ZipEntry(path))
                write()
                zip.closeEntry()
                progress(path)
            }
            put("manifest.json") {
                zip.write(
                    codec
                        .encodeToString(
                            Manifest(worlds = workspace.worlds.map { it.world.id }, files = digests)
                        )
                        .toByteArray()
                )
            }
            blobs.forEach { (path, b) ->
                put(path) {
                    var offset = 0
                    while (offset < b.size) {
                        if (Thread.currentThread().isInterrupted)
                            throw InterruptedIOException("Cancelled")
                        val n = minOf(65536, b.size - offset)
                        zip.write(b, offset, n)
                        offset += n
                    }
                }
            }
            files.forEach { (path, file) ->
                put(path) {
                    file.inputStream().use { input ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            if (Thread.currentThread().isInterrupted)
                                throw InterruptedIOException("Cancelled")
                            val n = input.read(buffer)
                            if (n < 0) break
                            zip.write(buffer, 0, n)
                        }
                    }
                }
            }
        }
    }

    fun inspect(input: File, stage: File, progress: (String) -> Unit = {}): InspectedArchive {
        require(input.length() <= MAX_BYTES) { "Compressed archive exceeds 512 MiB" }
        require(stage.mkdirs() || stage.isDirectory)
        val extracted = linkedMapOf<String, File>()
        var total = 0L
        try {
            // We never materialize links or directories from ZIP attributes, only bounded regular
            // files.
            // Reject UNIX symbolic-link central-directory attributes before any extraction.
            rejectSymlinks(input)
            ZipFile(input).use { zip ->
                val entries = zip.entries().toList()
                require(entries.size <= MAX_FILES + 1) { "Too many archive members" }
                val names = mutableSetOf<String>()
                entries.forEach { entry ->
                    val name = safePath(entry.name)
                    require(names.add(name.lowercase())) { "Duplicate/conflicting archive path" }
                    val dest = File(stage, name)
                    require(dest.canonicalPath.startsWith(stage.canonicalPath + File.separator)) {
                        "Path escaped staging"
                    }
                    dest.parentFile.mkdirs()
                    var size = 0L
                    zip.getInputStream(entry).use { src ->
                        dest.outputStream().use { out ->
                            val b = ByteArray(65536)
                            while (true) {
                                val n = src.read(b)
                                if (n < 0) break
                                size += n
                                total += n
                                require(size <= MAX_FILE && total <= MAX_BYTES) {
                                    "Archive expansion limit exceeded"
                                }
                                if (Thread.currentThread().isInterrupted)
                                    throw InterruptedIOException("Cancelled")
                                out.write(b, 0, n)
                            }
                        }
                    }
                    extracted[name] = dest
                    progress(name)
                }
            }
            val manifest =
                codec.decodeFromString<Manifest>(
                    boundedJson(
                        extracted["manifest.json"]?.readText() ?: error("Missing manifest.json")
                    )
                )
            require(manifest.format == "nodenote-worldbuilder" && manifest.version == 1) {
                "Unsupported backup format/version ${manifest.version}; use a compatible app"
            }
            require(manifest.files.map { it.path }.toSet().size == manifest.files.size) {
                "Duplicate manifest path"
            }
            require(extracted.keys == manifest.files.map { it.path }.toSet() + "manifest.json") {
                "Unlisted or missing archive members"
            }
            manifest.files.forEach { digest ->
                val f = extracted.getValue(safePath(digest.path))
                require(f.length() == digest.bytes && sha256(f) == digest.sha256) {
                    "Checksum/size mismatch: ${digest.path}"
                }
            }
            require(
                manifest.worlds.distinct().size == manifest.worlds.size &&
                    manifest.worlds.isNotEmpty()
            ) {
                "Invalid world inventory"
            }
            val bundles =
                manifest.worlds.map { id ->
                    val b =
                        codec.decodeFromString<WorldBundle>(
                            boundedJson(
                                extracted["worlds/$id/world.json"]?.readText()
                                    ?: error("Missing world")
                            )
                        )
                    require(b.world.id == id)
                    Integrity.requireValid(b)
                    b
                }
            require(
                bundles.flatMap { it.records }.map { it.id }.distinct().size ==
                    bundles.sumOf { it.records.size }
            ) {
                "Record ID collision across worlds"
            }
            bundles.forEach { bundle ->
                bundle.records
                    .filter { it.kind == Kind.ATTACHMENT }
                    .forEach { a ->
                        val f =
                            extracted["worlds/${bundle.world.id}/${a.f("path")}"]
                                ?: error("Missing attachment ${a.title}")
                        require(
                            sha256(f) == a.f("sha256") && f.length().toString() == a.f("bytes")
                        ) {
                            "Attachment metadata mismatch"
                        }
                    }
            }
            val prefs =
                extracted["preferences.json"]
                    ?.let { codec.decodeFromString<Map<String, String>>(it.readText()) }
                    .orEmpty()
            return InspectedArchive(
                Workspace(bundles, prefs),
                extracted,
                stage,
                bundles.flatMap {
                    Integrity.validate(it)
                        .filter { d -> d.severity == "WARNING" }
                        .map { d -> d.message }
                },
            )
        } catch (e: Exception) {
            stage.deleteRecursively()
            throw e
        }
    }

    private fun rejectSymlinks(file: File) {
        RandomAccessFile(file, "r").use { f ->
            val tail = ByteArray(minOf(file.length(), 65557L).toInt())
            f.seek(file.length() - tail.size)
            f.readFully(tail)
            fun u16(b: ByteArray, o: Int) =
                (b[o].toInt() and 255) or ((b[o + 1].toInt() and 255) shl 8)
            fun u32(b: ByteArray, o: Int): Long =
                (0..3).sumOf { (b[o + it].toLong() and 255) shl (it * 8) }
            val end =
                (tail.size - 22 downTo 0).firstOrNull { u32(tail, it) == 0x06054b50L }
                    ?: error("Truncated ZIP")
            require(u16(tail, end + 4) == 0 && u16(tail, end + 6) == 0) {
                "Multi-disk archives unsupported"
            }
            val offset = u32(tail, end + 16)
            val count = u16(tail, end + 10)
            require(count <= MAX_FILES + 1)
            f.seek(offset)
            repeat(count) {
                val h = ByteArray(46)
                f.readFully(h)
                require(u32(h, 0) == 0x02014b50L) { "Invalid ZIP directory" }
                val mode = (u32(h, 38) shr 16).toInt()
                require(mode and 0xf000 != 0xa000) { "Symbolic links are not allowed" }
                require(u16(h, 8) and 1 == 0) { "Encrypted archives unsupported" }
                f.seek(f.filePointer + u16(h, 28) + u16(h, 30) + u16(h, 32))
            }
        }
    }
}
