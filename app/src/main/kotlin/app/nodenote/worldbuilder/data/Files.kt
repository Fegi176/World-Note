package app.nodenote.worldbuilder.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import app.nodenote.core.*
import java.io.File
import kotlinx.coroutines.*

class ManagedFiles(private val context: Context, private val repository: Repository) {
    val recovery = File(context.filesDir, "recovery").also { it.mkdirs() }
    private val thumbs = File(context.cacheDir, "thumbs").also { it.mkdirs() }

    fun safetyCopy(workspace: Workspace, reason: String): File {
        require(reason in setOf("restore", "delete", "erase"))
        val file = File(recovery, "pre-$reason-${newId()}.nnbackup")
        val atomic = android.util.AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            // ZipOutputStream owns its wrapper; AtomicFile owns the underlying descriptor.
            val wrapper =
                object : java.io.FilterOutputStream(stream) {
                    override fun close() {
                        flush()
                    }

                    override fun write(bytes: ByteArray, off: Int, len: Int) {
                        out.write(bytes, off, len)
                    }
                }
            Archives.write(workspace, wrapper, repository::asset)
            atomic.finishWrite(stream)
            return file
        } catch (e: Exception) {
            atomic.failWrite(stream)
            throw e
        }
    }

    fun removeThumbnail(id: String) {
        require(Regex("[a-fA-F0-9-]{36}").matches(id))
        File(thumbs, "$id.jpg").delete()
    }

    fun thumbnailBytes(): Long = thumbs.listFiles().orEmpty().sumOf { it.length() }

    fun clearThumbnails() {
        thumbs.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    }

    fun recoveryArchives(): List<File> =
        recovery
            .listFiles { f -> f.isFile && f.name.startsWith("pre-") && f.extension == "nnbackup" }
            .orEmpty()
            .sortedByDescending { it.lastModified() }

    fun removeRecoveryArchive(file: File) {
        require(
            file.canonicalFile.parentFile == recovery.canonicalFile &&
                file.name.startsWith("pre-") &&
                file.extension == "nnbackup"
        ) {
            "Not a managed recovery archive"
        }
        require(file.delete()) { "Could not remove recovery archive" }
    }

    fun sampleImages(bundle: WorldBundle): WorldBundle {
        val added = mutableListOf<Record>()
        bundle.records
            .filter { it.kind in setOf(Kind.CHARACTER, Kind.PLACE, Kind.EPOCH) }
            .take(3)
            .forEachIndexed { index, owner ->
                val id = newId()
                val file = File(repository.assetRoot, id)
                val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.rgb(27, 28, 42))
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                paint.color = android.graphics.Color.rgb(110 + index * 20, 90 + index * 15, 160)
                canvas.drawCircle(320f, 160f, 110f, paint)
                paint.color = android.graphics.Color.rgb(27, 28, 42)
                canvas.drawCircle(340f, 140f, 85f, paint)
                paint.color = android.graphics.Color.rgb(210, 195, 155)
                paint.strokeWidth = 3f
                canvas.drawLine(80f, 270f, 560f, 270f, paint)
                paint.textSize = 23f
                canvas.drawText("THE ASHEN MERIDIAN · SAMPLE", 100f, 320f, paint)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                val asset =
                    Record(
                        id = id,
                        world = bundle.world.id,
                        kind = Kind.ATTACHMENT,
                        title = "Original geometric sample ${index+1}",
                        fields =
                            mapOf(
                                "path" to "assets/$id",
                                "mime" to "image/png",
                                "bytes" to file.length().toString(),
                                "sha256" to sha256(file),
                                "width" to "640",
                                "height" to "360",
                            ),
                    )
                added += asset
                added +=
                    Record(
                        world = bundle.world.id,
                        kind = Kind.MEDIA,
                        title = "Meridian study ${index+1}",
                        refs = mapOf("owner" to listOf(owner.id), "attachment" to listOf(asset.id)),
                        fields =
                            mapOf(
                                "caption" to "Geometric placeholder, generated locally.",
                                "credit" to "NodeNote sample artwork",
                                "cover" to "true",
                            ),
                    )
            }
        return bundle.copy(records = bundle.records + added)
    }

    suspend fun image(uri: Uri, owner: Record): Pair<Record, Record> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            var name = "Imported image"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0)
            }
            val id = newId()
            val original = File(repository.assetRoot, id)
            try {
                resolver.openInputStream(uri)?.use { input ->
                    original.outputStream().use { out ->
                        val bytes = ByteArray(65536)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(bytes)
                            if (n < 0) break
                            total += n
                            require(total <= 32L * 1024 * 1024) { "Image limit: 32 MiB" }
                            out.write(bytes, 0, n)
                        }
                    }
                } ?: error("Provider did not open image")
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(original.path, options)
                require(
                    options.outWidth > 0 &&
                        options.outHeight > 0 &&
                        options.outWidth.toLong() * options.outHeight <= 80_000_000
                ) {
                    "Malformed image or exceeds 80 megapixels"
                }
                require(
                    options.outMimeType in
                        listOf("image/jpeg", "image/png", "image/webp", "image/gif")
                ) {
                    "Unsupported image type"
                }
                val record =
                    Record(
                        id = id,
                        world = owner.world,
                        kind = Kind.ATTACHMENT,
                        title = name,
                        fields =
                            mapOf(
                                "path" to "assets/$id",
                                "mime" to options.outMimeType,
                                "width" to options.outWidth.toString(),
                                "height" to options.outHeight.toString(),
                                "bytes" to original.length().toString(),
                                "sha256" to sha256(original),
                            ),
                    )
                thumbnail(record)
                record to
                    Record(
                        world = owner.world,
                        kind = Kind.MEDIA,
                        title = name,
                        refs = mapOf("owner" to listOf(owner.id), "attachment" to listOf(id)),
                        fields = mapOf("caption" to "", "source URL" to ""),
                    )
            } catch (e: Exception) {
                original.delete()
                throw e
            }
        }

    suspend fun placeImage(uri: Uri, board: Record, position: Point): Record {
        require(board.kind == Kind.BOARD && !board.trashed)
        val owner = Record(world = board.world, kind = Kind.NOTE, title = "Image")
        val (asset, media) = image(uri, owner)
        val note = owner.copy(title = asset.title.substringBeforeLast('.').ifBlank { "Image" })
        val placement =
            Record(
                world = board.world,
                kind = Kind.PLACEMENT,
                title = note.title,
                refs = mapOf("board" to listOf(board.id), "entry" to listOf(note.id)),
                fields =
                    mapOf(
                        "x" to position.x.toString(),
                        "y" to position.y.toString(),
                        "width" to "240",
                        "height" to "220",
                        "show image" to "true",
                    ),
            )
        var committed = false
        try {
            withContext(NonCancellable) {
                repository.batch(
                    board.world,
                    listOf(note, asset, media.withField("cover", "true"), placement),
                )
                committed = true
            }
        } catch (e: Exception) {
            if (!committed) {
                repository.asset(asset).delete()
                File(thumbs, "${asset.id}.jpg").delete()
            }
            throw e
        }
        return placement
    }

    @Synchronized
    fun thumbnail(attachment: Record): File {
        val target = File(thumbs, "${attachment.id}.jpg")
        if (target.exists()) {
            val cached = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, cached)
            if (cached.outWidth in 1..1600 && cached.outHeight in 1..1600) return target
            target.delete()
        }
        val source = repository.asset(attachment)
        require(source.isFile) { "Missing attachment: ${attachment.title}" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        require(
            source.length() <= 32L * 1024 * 1024 &&
                bounds.outWidth > 0 &&
                bounds.outHeight > 0 &&
                bounds.outWidth.toLong() * bounds.outHeight <= 80_000_000
        ) {
            "Invalid image or image exceeds 32 MiB / 80 megapixels"
        }
        val options =
            BitmapFactory.Options().apply {
                var factor = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / factor > 800) factor *= 2
                inSampleSize = factor
            }
        val bitmap = BitmapFactory.decodeFile(source.path, options) ?: error("Corrupt image")
        val exif = runCatching { ExifInterface(source) }.getOrNull()
        val matrix = Matrix()
        if (exif != null) {
            if (exif.isFlipped) matrix.postScale(-1f, 1f)
            matrix.postRotate(exif.rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val atomic = android.util.AtomicFile(target)
        val output = atomic.startWrite()
        try {
            require(rotated.compress(Bitmap.CompressFormat.JPEG, 85, output))
            atomic.finishWrite(output)
        } catch (e: Exception) {
            atomic.failWrite(output)
            throw e
        }
        if (rotated !== bitmap) rotated.recycle()
        bitmap.recycle()
        return target
    }

    suspend fun copyInput(uri: Uri): File =
        withContext(Dispatchers.IO) {
            val file = File(recovery, "source-${newId()}")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        val b = ByteArray(65536)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(b)
                            if (n < 0) break
                            total += n
                            require(total <= Archives.MAX_BYTES)
                            output.write(b, 0, n)
                        }
                    }
                } ?: error("Provider did not open input")
                file
            } catch (e: Exception) {
                file.delete()
                throw e
            }
        }
}
