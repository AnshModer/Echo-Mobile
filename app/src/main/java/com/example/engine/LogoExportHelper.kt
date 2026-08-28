package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object LogoExportHelper {

    fun generateLogoBitmap(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = size / 2.0f
        val cornerRadius = size * 0.225f

        // 1. Dark Background Squircle
        val bgRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                center, center,
                size * 0.75f,
                intArrayOf(
                    Color.rgb(15, 23, 42),
                    Color.rgb(7, 11, 20),
                    Color.rgb(2, 4, 8)
                ),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Subtle Outer Border Glow
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.006f
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.argb(120, 0, 240, 255),
                Color.argb(120, 139, 92, 246),
                Shader.TileMode.CLAMP
            )
        }
        val innerBorderRect = RectF(size * 0.005f, size * 0.005f, size * 0.995f, size * 0.995f)
        canvas.drawRoundRect(innerBorderRect, cornerRadius * 0.98f, cornerRadius * 0.98f, borderPaint)

        // 3. Ambient Radial Nebula Glow
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                center, center,
                size * 0.46f,
                intArrayOf(
                    Color.argb(90, 0, 240, 255),
                    Color.argb(50, 16, 185, 129),
                    Color.argb(30, 124, 77, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.0f, 0.35f, 0.7f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, size * 0.46f, auraPaint)

        // 4. Outer Sonic Orbit Ring with dash
        val ring1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.007f
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.argb(160, 0, 240, 255),
                Color.argb(140, 16, 185, 129),
                Shader.TileMode.CLAMP
            )
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(size * 0.035f, size * 0.02f), 0f)
        }
        canvas.drawCircle(center, center, size * 0.34f, ring1Paint)

        // 5. Middle Glowing Sonic Ring
        val ring2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.011f
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                size.toFloat(), 0f, 0f, size.toFloat(),
                Color.argb(220, 236, 72, 153),
                Color.argb(220, 0, 240, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, size * 0.27f, ring2Paint)

        // 6. Inner Energy Halo
        val ring3Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.005f
            color = Color.argb(170, 0, 240, 255)
        }
        canvas.drawCircle(center, center, size * 0.215f, ring3Paint)

        // 7. Glowing Holographic AI Orb Core (3D Sphere Lighting)
        val orbRadius = size * 0.175f
        val orbCenterX = center - orbRadius * 0.25f
        val orbCenterY = center - orbRadius * 0.28f
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                orbCenterX, orbCenterY,
                orbRadius * 1.5f,
                intArrayOf(
                    Color.rgb(255, 255, 255),
                    Color.rgb(103, 232, 249),
                    Color.rgb(6, 182, 212),
                    Color.rgb(99, 102, 241),
                    Color.rgb(59, 7, 100)
                ),
                floatArrayOf(0.0f, 0.25f, 0.52f, 0.78f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, orbRadius, orbPaint)

        // 8. Siri/Gemini Sine Acoustic Waveforms inside Core
        val waveW = orbRadius * 1.35f
        val startX = center - waveW / 2
        val wavePath1 = Path().apply {
            moveTo(startX, center)
            cubicTo(
                center - waveW * 0.25f, center - orbRadius * 0.65f,
                center - waveW * 0.05f, center + orbRadius * 0.65f,
                center, center
            )
            cubicTo(
                center + waveW * 0.05f, center - orbRadius * 0.65f,
                center + waveW * 0.25f, center + orbRadius * 0.65f,
                center + waveW / 2, center
            )
        }
        val wavePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.012f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(245, 255, 255, 255)
        }
        canvas.drawPath(wavePath1, wavePaint1)

        val waveW2 = orbRadius * 1.15f
        val startX2 = center - waveW2 / 2
        val wavePath2 = Path().apply {
            moveTo(startX2, center)
            cubicTo(
                center - waveW2 * 0.22f, center + orbRadius * 0.45f,
                center - waveW2 * 0.05f, center - orbRadius * 0.45f,
                center, center
            )
            cubicTo(
                center + waveW2 * 0.05f, center + orbRadius * 0.45f,
                center + waveW2 * 0.22f, center - orbRadius * 0.45f,
                center + waveW2 / 2, center
            )
        }
        val wavePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.007f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(210, 165, 243, 252)
        }
        canvas.drawPath(wavePath2, wavePaint2)

        return bitmap
    }

    fun saveLogoToDownloads(context: Context, size: Int): String {
        return try {
            val bitmap = generateLogoBitmap(size)
            val fileName = "echo_logo_${size}x${size}.png"
            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EchoLogos")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    savedUri = uri
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val echoDir = File(picturesDir, "EchoLogos").apply { mkdirs() }
                val file = File(echoDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                savedUri = Uri.fromFile(file)
            }

            // Also save to app cache for sharing
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            "Downloaded ${size}x${size} logo to Pictures/EchoLogos!"
        } catch (e: Exception) {
            e.printStackTrace()
            "Export failed: ${e.localizedMessage}"
        }
    }

    fun shareLogo(context: Context, size: Int) {
        try {
            val bitmap = generateLogoBitmap(size)
            val fileName = "echo_logo_${size}x${size}.png"
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Echo AI Assistant App Icon Logo (${size}x${size}px)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${size}x${size} Logo"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share logo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
