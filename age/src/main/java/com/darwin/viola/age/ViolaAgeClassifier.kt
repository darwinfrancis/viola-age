package com.darwin.viola.age

import android.content.Context
import android.graphics.Bitmap
import android.media.FaceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.IllegalArgumentException


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

    init {
        Util.debug = true
    }

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

    fun findAgeAsync(faceBitmap: Bitmap) {
        if (isInitialized) {
            Util.printLog("Processing face bitmap for age classification.")
            coroutineScope.launch {
                if (verifyFacePresence(faceBitmap)) {
                    val results: List<AgeRecognition> =
                        classifier.recognizeImage(faceBitmap, 0)
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


    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun findAgeSynchronized(faceBitmap: Bitmap): List<AgeRecognition> {
        if (isInitialized) {
            Util.printLog("Processing face bitmap in synchronized manner for age classification.")
            if (!verifyFacePresence(faceBitmap)) {
                throw IllegalArgumentException("There is no face portraits in the given image.")
            }
            return classifier.recognizeImage(faceBitmap, 0)
        } else {
            Util.printLog("Viola age classifier is not initialized. Throwing exception.")
            throw IllegalStateException("Viola age classifier is not initialized.")
        }
    }

    private fun verifyFacePresence(bitmap: Bitmap): Boolean {
        val pBitmap: Bitmap = bitmap.copy(Bitmap.Config.RGB_565, true)
        val faceDetector = FaceDetector(pBitmap.width, pBitmap.height, 1)
        val faceArray = arrayOfNulls<FaceDetector.Face>(1)
        val faceCount = faceDetector.findFaces(pBitmap, faceArray)
        return faceCount != 0
    }
}