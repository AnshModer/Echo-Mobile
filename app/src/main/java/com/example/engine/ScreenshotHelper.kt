package com.example.engine

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScreenshotItem(
    val file: File,
    val uri: Uri,
    val name: String,
    val timestamp: Long,
    val sizeBytes: Long
)

object ScreenshotHelper {

    private val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private var lastCapturedUri: Uri? = null
    private var lastCapturedFile: File? = null

    fun getLastScreenshotUri(): Uri? = lastCapturedUri
    fun getLastScreenshotFile(): File? = lastCapturedFile

    /**
     * Captures a screenshot of the provided Activity's current window or view.
     */
    fun captureActivityScreenshot(
        activity: Activity,
        onComplete: (Boolean, String, Uri?) -> Unit
    ) {
        val window = activity.window ?: run {
            onComplete(false, "Cannot capture screen: Window unavailable.", null)
            return
        }

        val rootView = window.decorView.rootView ?: run {
            onComplete(false, "Cannot capture screen: Root view unavailable.", null)
            return
        }

        val width = rootView.width
        val height = rootView.height

        if (width <= 0 || height <= 0) {
            onComplete(false, "Cannot capture screen: View has zero dimensions.", null)
            return
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val location = IntArray(2)
            rootView.getLocationInWindow(location)
            val rect = Rect(location[0], location[1], location[0] + width, location[1] + height)

            try {
                PixelCopy.request(
                    window,
                    rect,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            val uri = saveBitmapToStorage(activity, bitmap)
                            if (uri != null) {
                                lastCapturedUri = uri
                                onComplete(true, "Screenshot captured and saved to Gallery.", uri)
                            } else {
                                onComplete(false, "Screenshot captured but failed to save file.", null)
                            }
                        } else {
                            // Fallback to Canvas draw
                            captureViaCanvas(activity, rootView, onComplete)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                e.printStackTrace()
                captureViaCanvas(activity, rootView, onComplete)
            }
        } else {
            captureViaCanvas(activity, rootView, onComplete)
        }
    }

    private fun captureViaCanvas(
        context: Context,
        view: View,
        onComplete: (Boolean, String, Uri?) -> Unit
    ) {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val uri = saveBitmapToStorage(context, bitmap)
            if (uri != null) {
                lastCapturedUri = uri
                onComplete(true, "Screenshot captured and saved to Gallery.", uri)
            } else {
                onComplete(false, "Screenshot captured but failed to save file.", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, "Failed to capture screenshot: ${e.localizedMessage}", null)
        }
    }

    /**
     * Saves bitmap to MediaStore / app cache storage so it is both persistent and shareable via FileProvider.
     */
    fun saveBitmapToStorage(context: Context, bitmap: Bitmap): Uri? {
        val fileName = "Echo_Screenshot_${timeFormat.format(Date())}.png"

        // 1. Save to local app storage for direct FileProvider sharing
        val screenshotsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Screenshots").apply {
            if (!exists()) mkdirs()
        }
        val file = File(screenshotsDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            lastCapturedFile = file
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Also register in MediaStore for visibility in Gallery / Photos app
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EchoScreenshots")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                val stream: OutputStream? = resolver.openOutputStream(imageUri)
                stream?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return FileProvider Uri for instant sharing / previewing
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Uri.fromFile(file)
        }
    }

    /**
     * Share screenshot image via standard Android Share sheet
     */
    fun shareScreenshot(context: Context, uri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Echo Screenshot")
                putExtra(Intent.EXTRA_TEXT, "Captured with Echo Voice Assistant")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Screenshot via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share screenshot: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * View screenshot in Gallery / Photo viewer
     */
    fun viewScreenshot(context: Context, uri: Uri) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            shareScreenshot(context, uri)
        }
    }

    /**
     * Retrieves all screenshots captured by Echo ordered from newest to oldest
     */
    fun getRecentScreenshots(context: Context): List<ScreenshotItem> {
        val list = mutableListOf<ScreenshotItem>()
        try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Screenshots")
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { f -> f.extension.equals("png", ignoreCase = true) || f.extension.equals("jpg", ignoreCase = true) }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

                for (f in files) {
                    val uri = try {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                    } catch (e: Exception) {
                        Uri.fromFile(f)
                    }
                    list.add(
                        ScreenshotItem(
                            file = f,
                            uri = uri,
                            name = f.name,
                            timestamp = f.lastModified(),
                            sizeBytes = f.length()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
