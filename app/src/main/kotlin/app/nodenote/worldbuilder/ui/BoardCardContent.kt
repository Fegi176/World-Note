package app.nodenote.worldbuilder.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.compose.runtime.*
import app.nodenote.core.*
import app.nodenote.worldbuilder.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Placement presentation only; linked author content is never copied into the placement. */
data class CardContent(val image: Boolean, val notes: Boolean, val details: Boolean) {
    fun applyTo(p: Record): Record =
        p.copy(
            fields =
                p.fields +
                    mapOf(
                        "show image" to image.toString(),
                        "show notes" to notes.toString(),
                        "show details" to details.toString(),
                        "height" to
                            maxOf(
                                    p.n("height", 112.0),
                                    72.0 +
                                        (if (image) 112 else 0) +
                                        (if (notes) 66 else 0) +
                                        (if (details) 40 else 0),
                                )
                                .toString(),
                    )
        )

    companion object {
        fun from(p: Record) =
            CardContent(
                p.f("show image") == "true",
                p.f("show notes") == "true",
                p.f("show details") == "true",
            )
    }
}

fun boardCovers(records: List<Record>): Map<String, Record> {
    val attachments =
        records.filter { it.kind == Kind.ATTACHMENT && !it.trashed }.associateBy { it.id }
    return records
        .filter { it.kind == Kind.MEDIA && !it.trashed }
        .groupBy { it.ref("owner") }
        .mapNotNull { (owner, media) ->
            val chosen =
                media
                    .sortedWith(
                        compareByDescending<Record> { it.f("cover") == "true" }.thenBy { it.id }
                    )
                    .firstNotNullOfOrNull { attachments[it.ref("attachment")] }
            if (owner != null && chosen != null) owner to chosen else null
        }
        .toMap()
}

/** Viewport-bounded decode, off the UI thread. Cache and each bitmap have fixed limits. */
@Composable
fun boardThumbnails(vm: AppModel, requested: List<Record>): Map<String, Bitmap?> {
    val cache = remember {
        object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
        }
    }
    val keys = requested.take(64).map { it.id + it.f("sha256") }
    val bitmaps by
        produceState<Map<String, Bitmap?>>(emptyMap(), keys) {
            value =
                withContext(Dispatchers.IO) {
                    requested.take(64).associate { attachment ->
                        ensureActive()
                        val key = attachment.id + attachment.f("sha256")
                        val bitmap =
                            cache.get(key)
                                ?: runCatching {
                                    val file = vm.files.thumbnail(attachment)
                                    val bounds =
                                        BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                    BitmapFactory.decodeFile(file.path, bounds)
                                    val options =
                                        BitmapFactory.Options().apply {
                                            inSampleSize = 1
                                            while (
                                                maxOf(bounds.outWidth, bounds.outHeight) /
                                                    inSampleSize > 256
                                            ) inSampleSize *= 2
                                            inPreferredConfig = Bitmap.Config.RGB_565
                                        }
                                    BitmapFactory.decodeFile(file.path, options)?.also {
                                        cache.put(key, it)
                                    }
                                }
                                    .getOrNull()
                        attachment.id to bitmap
                    }
                }
        }
    return bitmaps
}

/**
 * Native text layout wraps Unicode and clips every item to its card. Images fit without cropping.
 */
fun drawBoardCardContent(
    canvas: Canvas,
    p: Record,
    entry: Record?,
    bitmap: Bitmap?,
    hasImage: Boolean,
    bounds: RectF,
    scale: Float,
) {
    val content = CardContent.from(p)
    val pad = 10f * scale
    val checkpoint = canvas.save()
    canvas.clipRect(
        bounds.left + pad / 2,
        bounds.top + pad / 2,
        bounds.right - pad / 2,
        bounds.bottom - pad / 2,
    )
    var y = bounds.top + pad
    val width = (bounds.width() - 2 * pad).toInt().coerceAtLeast(1)
    fun text(value: String, sp: Float, lines: Int, color: Int) {
        if (value.isBlank() || y >= bounds.bottom - pad) return
        val paint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sp * scale
                this.color = color
            }
        val layout =
            StaticLayout.Builder.obtain(
                    value.take(1200),
                    0,
                    minOf(value.length, 1200),
                    paint,
                    width,
                )
                .setMaxLines(lines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build()
        val save = canvas.save()
        canvas.translate(bounds.left + pad, y)
        layout.draw(canvas)
        canvas.restoreToCount(save)
        y += layout.height + 6 * scale
    }
    text(
        if (entry?.trashed == true) "[Trashed] ${entry.title}" else entry?.title ?: p.title,
        14f,
        2,
        Color.WHITE,
    )
    if (content.image) {
        val area = RectF(bounds.left + pad, y, bounds.right - pad, y + 104 * scale)
        if (bitmap != null) {
            val ratio = minOf(area.width() / bitmap.width, area.height() / bitmap.height)
            val w = bitmap.width * ratio
            val h = bitmap.height * ratio
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    area.centerX() - w / 2,
                    area.centerY() - h / 2,
                    area.centerX() + w / 2,
                    area.centerY() + h / 2,
                ),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
        } else {
            text(
                if (hasImage) "Image preview unavailable"
                else "Add an image in the entry’s Media tab",
                11f,
                2,
                Color.LTGRAY,
            )
        }
        y = area.bottom + 8 * scale
    }
    if (content.notes)
        text(
            (entry?.summary?.ifBlank { entry.body } ?: p.body).ifBlank { "No notes yet" },
            12f,
            3,
            Color.rgb(216, 230, 224),
        )
    if (content.details && entry != null) {
        text(
            listOf(entry.canon, entry.writing).filter { it.isNotBlank() }.joinToString(" · "),
            10f,
            1,
            Color.rgb(169, 191, 255),
        )
        text(entry.f("tags"), 10f, 1, Color.LTGRAY)
    }
    text(entry?.kind?.label ?: "Board sticky", 10f, 1, Color.LTGRAY)
    canvas.restoreToCount(checkpoint)
}
