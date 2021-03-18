package com.darwin.viola.age.sample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.darwin.viola.age.AgeClassificationListener
import com.darwin.viola.age.AgeRecognition
import com.darwin.viola.age.ViolaAgeClassifier
import com.darwin.viola.still.FaceDetectionListener
import com.darwin.viola.still.Viola
import com.darwin.viola.still.model.CropAlgorithm
import com.darwin.viola.still.model.FaceDetectionError
import com.darwin.viola.still.model.FaceOptions
import com.darwin.viola.still.model.Result
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viola: Viola
    private lateinit var violaAgeClassifier: ViolaAgeClassifier
    private var bitmap: Bitmap? = null

    private lateinit var permissionHelper: PermissionHelper

    private val imagePickerIntentId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionHelper = PermissionHelper(this)

        initializeUI()
        setEventListeners()

        viola = Viola(faceDetectionListener)
        violaAgeClassifier = ViolaAgeClassifier(ageClassifierListener)
        violaAgeClassifier.initialize(this)
    }

    override fun onResume() {
        super.onResume()
        permissionHelper.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionHelper.onDestroy()
    }

    private fun initializeUI() {
        val options = BitmapFactory.Options()
        options.inScaled = false
        bitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.face, options
        )
        ivInputImage.setImageBitmap(bitmap)
    }

    private fun setEventListeners() {
        btChooseImage.setOnClickListener {
            requestStoragePermission()
        }
        btFindAge.setOnClickListener {
            crop()
        }
    }

    private fun requestStoragePermission() {
        permissionHelper.setListener(permissionsListener)
        val requiredPermissions =
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionHelper.requestPermission(requiredPermissions, 100)
    }

    private fun pickImageFromGallery() {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.type = "image/*"

        val chooserIntent = Intent.createChooser(getIntent, "Select Image")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        startActivityForResult(chooserIntent, imagePickerIntentId)
    }


    private fun crop() {
        val faceOption =
            FaceOptions.Builder()
                .cropAlgorithm(CropAlgorithm.THREE_BY_FOUR)
                .setMinimumFaceSize(6)
                .enableProminentFaceDetection()
                .build()
        viola.detectFace(bitmap!!, faceOption)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickerIntentId && resultCode == Activity.RESULT_OK) {
            val pickedImage: Uri = data?.data!!
            val imagePath = Util.getPath(this, pickedImage)
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            bitmap = BitmapFactory.decodeFile(imagePath, options)
            bitmap = Util.modifyOrientation(bitmap!!, imagePath!!)
            ivInputImage.setImageBitmap(bitmap)
            ivFaceImage.setImageBitmap(null)
            tvAge.text = ""
        }
    }

    private val faceDetectionListener: FaceDetectionListener = object : FaceDetectionListener {

        override fun onFaceDetected(result: Result) {
            tvError.visibility = View.GONE
            val faceBitmap = result.facePortraits[0].face
            ivFaceImage.setImageBitmap(faceBitmap)
            violaAgeClassifier.findAgeAsync(faceBitmap)
        }

        override fun onFaceDetectionFailed(error: FaceDetectionError, message: String) {
            tvError.text = message
            tvError.visibility = View.VISIBLE
            ivFaceImage.setImageBitmap(null)
        }
    }

    private val ageClassifierListener = object : AgeClassificationListener {
        override fun onAgeClassificationResult(result: List<AgeRecognition>) {
            val builder = StringBuilder()
            result.forEach {
                builder.append("Range: ")
                builder.append(it.range)
                builder.append(", Confidence: ")
                builder.append(it.confidence)
                builder.append("\n")
            }

            tvAge.text = builder.toString()
        }

        override fun onAgeClassificationError(error: String) {

        }
    }


    private val permissionsListener: PermissionHelper.PermissionsListener = object :
        PermissionHelper.PermissionsListener {
        override fun onPermissionGranted(request_code: Int) {
            tvError.visibility = View.GONE
            pickImageFromGallery()
        }

        override fun onPermissionRejectedManyTimes(
            rejectedPerms: List<String>,
            request_code: Int,
            neverAsk: Boolean
        ) {
            tvError.text = "Permission for storage access denied."
            tvError.visibility = View.VISIBLE
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}