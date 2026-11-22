package com.example.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.DevotionalEntity
import com.example.data.model.StatusCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object StatusCardGenerator {

    suspend fun generateStatusCardUri(
        context: Context,
        entity: DevotionalEntity,
        theme: StatusCardTheme
    ): Uri = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background illustration scaled to cover 1080x1920
        val bgRaw = BitmapFactory.decodeResource(context.resources, theme.drawableRes)
        if (bgRaw != null) {
            val scale = maxOf(
                width.toFloat() / bgRaw.width.toFloat(),
                height.toFloat() / bgRaw.height.toFloat()
            )
            val scaledW = (bgRaw.width * scale).toInt()
            val scaledH = (bgRaw.height * scale).toInt()
            val left = (width - scaledW) / 2
            val top = (height - scaledH) / 2
            val destRect = Rect(left, top, left + scaledW, top + scaledH)
            canvas.drawBitmap(bgRaw, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            bgRaw.recycle()
        } else {
            canvas.drawColor(Color.parseColor("#3B2418"))
        }

        // 2. Warm devotional gradient scrim for crisp legibility
        val scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                intArrayOf(
                    Color.argb(140, 26, 18, 12),
                    Color.argb(185, 24, 17, 12),
                    Color.argb(230, 20, 14, 10)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        // 3. Soft gold border frame
        val borderPaint = Paint().apply {
            color = Color.argb(115, 232, 188, 110)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(56f, 72f, width - 56f, height - 72f, 44f, 44f, borderPaint)

        val serifTypeface = ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.SERIF
        val sansTypeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF

        // 4. Mood / Time Pill Header
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2C97D")
            textSize = 34f
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            letterSpacing = 0.14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            entity.moodTag.uppercase(),
            width / 2f,
            230f,
            headerPaint
        )

        // 5. Quote Mark
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 242, 201, 125)
            textSize = 140f
            typeface = serifTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“", width / 2f, 405f, quotePaint)

        // 6. Bible Verse Text (Centered StaticLayout)
        val versePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8EE")
            textSize = 60f
            typeface = Typeface.create(serifTypeface, Typeface.BOLD)
        }
        val verseWidth = width - 192
        val verseLayout = StaticLayout.Builder
            .obtain(entity.verseText, 0, entity.verseText.length, versePaint, verseWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(14f, 1.1f)
            .build()

        canvas.save()
        canvas.translate(96f, 450f)
        verseLayout.draw(canvas)
        canvas.restore()

        val afterVerseY = 450f + verseLayout.height + 64f

        // 7. Verse Reference
        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2C97D")
            textSize = 42f
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— ${entity.verseReference} —", width / 2f, afterVerseY, refPaint)

        // 8. Divider Line
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 242, 201, 125)
            strokeWidth = 2.5f
        }
        canvas.drawLine(width / 2f - 120f, afterVerseY + 56f, width / 2f + 120f, afterVerseY + 56f, dividerPaint)

        // 9. Short Reflection Excerpt
        val reflectionSnippet = if (entity.reflection.length > 220) {
            entity.reflection.take(217).trimEnd() + "…"
        } else {
            entity.reflection
        }
        val reflectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E6DDD0")
            textSize = 36f
            typeface = sansTypeface
        }
        val reflectionLayout = StaticLayout.Builder
            .obtain(reflectionSnippet, 0, reflectionSnippet.length, reflectionPaint, verseWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(10f, 1.1f)
            .build()

        canvas.save()
        canvas.translate(96f, afterVerseY + 115f)
        reflectionLayout.draw(canvas)
        canvas.restore()

        // 10. Subtle Footer Watermark
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 248, 238)
            textSize = 32f
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Daily Vachan • Peace in Every Word", width / 2f, height - 140f, footerPaint)

        // 11. Save PNG into cacheDir/shared_cards/
        val cacheFolder = File(context.cacheDir, "shared_cards").apply { if (!exists()) mkdirs() }
        val file = File(cacheFolder, "vachan_status_${System.currentTimeMillis()}.png")
        // Keep cache tidy by removing older temporary cards
        cacheFolder.listFiles()?.forEach { existing ->
            if (existing.name != file.name) existing.delete()
        }
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareStatusImage(context: Context, imageUri: Uri, entity: DevotionalEntity, preferWhatsApp: Boolean) {
        val caption = "\"${entity.verseText}\" — ${entity.verseReference}\n\nShared via Daily Vachan"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (preferWhatsApp) {
            val whatsappPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                try {
                    context.packageManager.getPackageInfo(pkg, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            }
            if (whatsappPackage != null) {
                shareIntent.setPackage(whatsappPackage)
            }
        }

        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_status_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
