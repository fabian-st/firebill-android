package com.firebill.android.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.firebill.android.R
import com.firebill.android.data.repository.BillRepository
import com.firebill.android.databinding.ActivityCaptureBinding
import com.firebill.android.worker.UploadWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaptureBinding
    private val repository by lazy { BillRepository(this) }

    private var photoUri: Uri? = null
    private var photoFile: File? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Snackbar.make(binding.root, R.string.camera_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { showPreview(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoUri = it
            photoFile = null
            showPreview(it)
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) galleryLauncher.launch("image/*")
        else Snackbar.make(binding.root, R.string.storage_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.buttonCamera.setOnClickListener { checkCameraPermission() }
        binding.buttonGallery.setOnClickListener { checkStoragePermission() }
        binding.buttonUpload.setOnClickListener { uploadPhoto() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> galleryLauncher.launch("image/*")
            else -> storagePermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        val imageFile = createImageFile()
        photoFile = imageFile
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
        photoUri = uri
        cameraLauncher.launch(uri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("BILL_${timestamp}_", ".jpg", storageDir)
    }

    private fun showPreview(uri: Uri) {
        binding.imagePreview.setImageURI(uri)
        binding.imagePreview.visibility = View.VISIBLE
        binding.textNoImage.visibility = View.GONE
        binding.buttonUpload.isEnabled = true
    }

    private fun uploadPhoto() {
        val uri = photoUri ?: return
        binding.progressUpload.visibility = View.VISIBLE
        binding.buttonUpload.isEnabled = false

        if (isOnline()) {
            uploadNow(uri)
        } else {
            queueUpload(uri)
        }
    }

    private fun uploadNow(uri: Uri) {
        lifecycleScope.launch {
            val file = getFileFromUri(uri)
            if (file == null) {
                showError(getString(R.string.error_reading_file))
                return@launch
            }
            repository.uploadBill(file)
                .onSuccess {
                    Snackbar.make(binding.root, R.string.upload_success, Snackbar.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    showError(it.message ?: getString(R.string.upload_failed))
                }
            binding.progressUpload.visibility = View.GONE
            binding.buttonUpload.isEnabled = true
        }
    }

    private fun queueUpload(uri: Uri) {
        lifecycleScope.launch {
            val file = getFileFromUri(uri)
            if (file == null) {
                showError(getString(R.string.error_reading_file))
                return@launch
            }
            repository.queueUpload(file.absolutePath, file.name)

            // Schedule WorkManager task to upload when online
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(workRequest)

            binding.progressUpload.visibility = View.GONE
            binding.buttonUpload.isEnabled = true
            Snackbar.make(binding.root, R.string.queued_for_upload, Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        // If we have a direct file reference from camera, use it
        photoFile?.let { if (it.exists()) return it }

        // Otherwise, copy from URI to a temp file
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cacheFile = File(cacheDir, "bill_${timestamp}.jpg")
            cacheFile.outputStream().use { out -> inputStream.copyTo(out) }
            inputStream.close()
            cacheFile
        } catch (e: Exception) {
            null
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        binding.progressUpload.visibility = View.GONE
        binding.buttonUpload.isEnabled = true
    }
}
