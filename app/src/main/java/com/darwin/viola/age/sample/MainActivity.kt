package com.darwin.viola.age.sample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.media.FaceDetector
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.darwin.viola.age.AgeClassificationListener
import com.darwin.viola.age.AgeOptions
import com.darwin.viola.age.AgeRecognition
import com.darwin.viola.age.ViolaAgeClassifier
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

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
            val faceBitmap = cropFaceFromBitmap(bitmap!!)
            faceBitmap?.let {
                tvError.visibility = View.GONE
                ivFaceImage.setImageBitmap(faceBitmap)
                findAge(faceBitmap)
            } ?: run {
                "Unable to crop face from given image.".also { tvError.text = it }
                tvError.visibility = View.VISIBLE
                ivFaceImage.setImageBitmap(null)
            }
        }
    }

    private fun requestStoragePermission() {
        permissionHelper.setListener(permissionsListener)
        val requiredPermissions =
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionHelper.requestPermission(requiredPermissions, 100)
    }

    private fun findAge(faceBitmap: Bitmap) {
        val ageOption = AgeOptions.Builder()
            .enableFacePreValidation() //ignore this if you are confident that input image has valid face in it.
            .build()
        violaAgeClassifier.findAgeAsync(faceBitmap, ageOption)
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


    private fun cropFaceFromBitmap(bitmap: Bitmap): Bitmap? {
        val resizedBitmap = resize(bitmap)
        val fixedBitmap = forceEvenBitmapSize(resizedBitmap)
        val pBitmap: Bitmap = fixedBitmap.copy(Bitmap.Config.RGB_565, true)
        val faceDetector = FaceDetector(pBitmap.width, pBitmap.height, 1)
        val faceArray = arrayOfNulls<FaceDetector.Face>(1)
        val faceCount = faceDetector.findFaces(pBitmap, faceArray)

        if (faceCount != 0) {
            val face: FaceDetector.Face = faceArray[0]!!

            val faceMidpoint = PointF()

            face.getMidPoint(faceMidpoint);

            val eyesDistance = face.eyesDistance()
            val xPadding = (eyesDistance * 2)
            val yPadding = (eyesDistance * 2)

            var bStartX = faceMidpoint.x - xPadding
            bStartX = bStartX.coerceAtLeast(0.0f)
            var bStartY = faceMidpoint.y - yPadding
            bStartY = bStartY.coerceAtLeast(0.0f)
            var bWidth = (eyesDistance / 0.25).toFloat()

            var bHeight = (bWidth / 0.75).toFloat()

            bWidth =
                if (bStartX + bWidth > fixedBitmap.width) fixedBitmap.width.toFloat() else bWidth
            bHeight =
                if (bStartY + bHeight > fixedBitmap.height) fixedBitmap.height.toFloat() else bHeight

            if (bStartY + bHeight > fixedBitmap.height) {
                val excessHeight: Float = bStartY + bHeight - fixedBitmap.height
                bHeight -= excessHeight
            }

            if (bStartX + bWidth > fixedBitmap.width) {
                val excessWidth: Float = bStartX + bWidth - fixedBitmap.width
                bWidth -= excessWidth
            }

            return Bitmap.createBitmap(
                fixedBitmap,
                bStartX.toInt(),
                bStartY.toInt(),
                bWidth.toInt(),
                bHeight.toInt()
            )
        } else {
            return null
        }

    }

    private fun resize(image: Bitmap): Bitmap {
        val maxWidth = 300
        val maxHeight = 400
        val width = image.width
        val height = image.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
    }

    private fun forceEvenBitmapSize(original: Bitmap): Bitmap {
        var width = original.width
        var height = original.height
        if (width % 2 == 1) {
            width++
        }
        if (height % 2 == 1) {
            height++
        }
        var fixedBitmap = original
        if (width != original.width || height != original.height) {
            fixedBitmap = Bitmap.createScaledBitmap(original, width, height, false)
        }
        return fixedBitmap
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

    private val ageClassifierListener = object : AgeClassificationListener {
        override fun onAgeClassificationResult(result: List<AgeRecognition>) {
            val builder = StringBuilder()
            result.forEach {
                builder.append("Range: ")
                builder.append(it.range)
                builder.append(", Confidence: ")
                builder.append(it.confidence.toBigDecimal().toPlainString())
                builder.append(" %")
                builder.append("\n")
            }

            tvAge.text = builder.toString()
        }

        override fun onAgeClassificationError(error: String) {
            tvError.text = error
            tvError.visibility = View.VISIBLE
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
            "Permission for storage access denied.".also { tvError.text = it }
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