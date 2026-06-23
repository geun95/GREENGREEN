package com.example.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ClassificationResult(
    val label: String,
    val confidence: Float
)

class PlantClassifier(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputSize: Int

    init {
        val model = loadModelFile(context, "plant_model.tflite")
        interpreter = Interpreter(model)
        labels = context.assets.open("labels.txt").bufferedReader().readLines().filter { it.isNotBlank() }

        val inputShape = interpreter.getInputTensor(0).shape() // [1, H, W, C]
        inputSize = inputShape[1]
    }

    fun classify(bitmap: Bitmap): List<ClassificationResult> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = convertBitmapToByteBuffer(resized)

        val outputShape = interpreter.getOutputTensor(0).shape() // [1, numClasses]
        val numClasses = outputShape[1]
        val output = Array(1) { FloatArray(numClasses) }

        interpreter.run(inputBuffer, output)

        return output[0].mapIndexed { index, score ->
            ClassificationResult(
                label = labels.getOrElse(index) { "unknown" },
                confidence = score
            )
        }.sortedByDescending { it.confidence }
    }

    fun close() {
        interpreter.close()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }
        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }
}
