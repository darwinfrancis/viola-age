package com.darwin.viola.age

import android.content.Context
import android.graphics.Bitmap
import android.media.FaceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


/**
 * The class ViolaAgeClassifier
 *
 * @author Darwin Francis
 * @version 1.0
 * @since 16 Mar 2021
 */
class ViolaAgeClassifier(private val listener: AgeClassificationListener) {

    private lateinit var classifier: Classifier
    var isInitialized = false
        private set
    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    fun initialize(context: Context) {
        if (!isInitialized) {
            Util.printLog("Initializing Viola age classifier.")
            val model = Classifier.Model.QUANTIZED_MOBILE_NET
            val device = Classifier.Device.CPU
            try {
                classifier = Classifier.create(context, model, device, 1)
                isInitialized = true
            } catch (e: IOException) {
                val error =
                    "Failed to create age classifier: ${e.javaClass.canonicalName}(${e.message})"
                Util.printLog(error)
                listener.onAgeClassificationError(error)
            }
        }
    }


    fun dispose() {
        Util.printLog("Disposing age classifier and its resources.")
        isInitialized = false
        classifier.close()
    }

    @JvmOverloads
    fun findAgeAsync(faceBitmap: Bitmap, options: AgeOptions = getDefaultAgeOptions()) {
        Util.debug = options.debug
        if (isInitialized) {
            Util.printLog("Processing face bitmap for age classification.")
            coroutineScope.launch {
                val resizedBitmap = resize(faceBitmap)
                val fixedBitmap = Util.forceEvenBitmapSize(resizedBitmap)!!
                if (!options.preValidateFace || verifyFacePresence(fixedBitmap)) {
                    val results: List<AgeRecognition> =
                        classifier.recognizeImage(resizedBitmap, 0)
                    Util.printLog("Age classification completed, sending back the result.")
                    withContext(Dispatchers.Main) { listener.onAgeClassificationResult(results) }
                } else {
                    withContext(Dispatchers.Main) { listener.onAgeClassificationError("There is no face portraits in the given image.") }
                }
            }
        } else {
            Util.printLog("Viola age classifier is not initialized.")
            listener.onAgeClassificationError("Viola age classifier is not initialized.")
        }
    }


    @JvmOverloads
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun findAgeSynchronized(
        faceBitmap: Bitmap,
        options: AgeOptions = getDefaultAgeOptions()
    ): List<AgeRecognition> {
        Util.debug = options.debug
        if (isInitialized) {
            Util.printLog("Processing face bitmap in synchronized manner for age classification.")
            val resizedBitmap = resize(faceBitmap)
            val fixedBitmap = Util.forceEvenBitmapSize(resizedBitmap)!!
            if (options.preValidateFace && !verifyFacePresence(fixedBitmap)) {
                throw IllegalArgumentException("There is no face portraits in the given image.")
            }
            return classifier.recognizeImage(resizedBitmap, 0)
        } else {
            Util.printLog("Viola age classifier is not initialized. Throwing exception.")
            throw IllegalStateException("Viola age classifier is not initialized.")
        }
    }

    private fun getDefaultAgeOptions(): AgeOptions {
        return AgeOptions.Builder()
            .build()
    }

    private fun verifyFacePresence(bitmap: Bitmap): Boolean {
        val pBitmap: Bitmap = bitmap.copy(Bitmap.Config.RGB_565, true)
        val faceDetector = FaceDetector(pBitmap.width, pBitmap.height, 1)
        val faceArray = arrayOfNulls<FaceDetector.Face>(1)
        val faceCount = faceDetector.findFaces(pBitmap, faceArray)
        return faceCount != 0
    }

    private fun resize(image: Bitmap): Bitmap {
        Util.printLog("Re-scaling input bitmap for fast image processing.")
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
}