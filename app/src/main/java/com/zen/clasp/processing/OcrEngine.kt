package com.zen.clasp.processing

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

interface OcrEngine {
    suspend fun recognize(file: File): String
}

class MlKitOcrEngine(private val context: Context) : OcrEngine {
    override suspend fun recognize(file: File): String {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(image).await().text.trim()
        } finally {
            recognizer.close()
        }
    }
}
