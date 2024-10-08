package com.concordium.wallet.ui.onramp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.util.Log
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SwipeluxWebChromeClient(
    private val context: Context,
    private val filePickerLauncher: ActivityResultLauncher<Intent>,
    private val permissionManager: PermissionManager
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    fun getWidgetSettings() : String? {
        val widgetConfig = WidgetConfig(
            apiKey = "6515da6d-a065-4676-a214-c83e5b18f5f3",
            colors = Colors(
                main = "#48A2AE",
                background = "#182022",
                processing = "#FFA400",
                warning = "#ED0A34",
                success = "#58CB4E",
                link = "F24F21"
            )
        )

        val gson = App.appCore.gson
        val jsonString = gson.toJson(widgetConfig)
        return URLEncoder.encode(jsonString, StandardCharsets.UTF_8.toString())
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback = filePathCallback

        if (!permissionManager.arePermissionsGranted()) {
            permissionManager.requestPermissions()
            Toast.makeText(
                context,
                "Permissions required for camera and microphone",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Create camera intent to capture an image
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

            // Grant URI permission
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // File picker intent for selecting an image from storage
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }

        // Combine both intents into a chooser
        val chooserIntent = Intent.createChooser(fileIntent, "Select or capture an image")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))

        filePickerLauncher.launch(chooserIntent)
        return true
    }

    fun handleFileChooserResult(resultCode: Int, data: Intent?) {
        Log.i("File chooser result received: resultCode=$resultCode")
        val results: Array<Uri>? = when {
            resultCode != Activity.RESULT_OK -> {
                Log.e("File chooser result: Not OK")
                null
            }

            data?.data != null -> {
                Log.i("File chooser result: Data URI=${data.data}")
                arrayOf(data.data!!)
            }

            cameraImageUri != null -> {
                Log.i("File chooser result: Camera URI=${cameraImageUri}")
                arrayOf(cameraImageUri!!)
            }

            else -> {
                Log.e("File chooser result: No data or camera URI available")
                null
            }
        }

        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    private fun createImageFile(): File? {
        return try {
            // Generate a timestamp for the filename
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Get the storage directory for pictures
            val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            // Ensure the directory exists, creating it if necessary
            if (storageDir == null) {
                Log.e("Error: Storage directory is null")
                return null
            } else if (!storageDir.exists()) {
                val dirCreated = storageDir.mkdirs()
                Log.e("Directory creation result: $dirCreated at ${storageDir.absolutePath}")
            }

            // Create the file in the specified directory
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            Log.i("Image file created: ${imageFile.absolutePath}")
            imageFile
        } catch (ex: Exception) {
            Log.e("Error creating file: ${ex.message}", ex)
            null
        }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val requestedResources = request.resources
        val grantedResources = requestedResources.filter {
            when (it) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                }

                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                }

                else -> false
            }
        }.toTypedArray()

        if (grantedResources.isNotEmpty()) {
            request.grant(grantedResources)
        } else {
            request.deny()
        }
    }

    companion object {
        const val BASE_URL = "https://track.swipelux.com"
    }
}