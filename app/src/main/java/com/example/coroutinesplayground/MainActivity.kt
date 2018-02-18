package com.example.coroutinesplayground

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {

    private var blurImageJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        blurSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
//                launch {
//                    val start = System.currentTimeMillis()
//                    val blurryImage = blurImage()
//                    val end = System.currentTimeMillis()
//                    Log.d("MainActivity", "Time to blur: ${end - start}ms")
//                    blurryImageView.setImageBitmap(blurryImage)
//                }
                // Or:
                blurImageJob = launch {
                    val start = System.currentTimeMillis()
                    val blurryImage = blurImageAsync().await()
                    val end = System.currentTimeMillis()
                    Log.d("MainActivity", "Time to blur: ${end - start}ms")
                    updateImage(blurryImage)
                }
            } else {
                if (blurImageJob != null) {
                    blurImageJob?.cancel()
                    blurImageJob = null
                }
                unblurImage()
            }
        }
    }

    private fun updateImage(image: Bitmap) = launch(UI) {
        blurryImageView.setImageBitmap(image)
    }

    private fun unblurImage() {
        blurryImageView.setImageResource(R.drawable.lenna)
    }

    private fun blurImage(): Bitmap {
        val inputBitmap = BitmapFactory.decodeResource(resources, R.drawable.lenna)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(this)
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4((rs)))
        val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap)
        val outputAllocation = Allocation.createFromBitmap(rs, outputBitmap)
        blurScript.setRadius(24f)
        blurScript.setInput(inputAllocation)
        for (i in 0..100) {
            blurScript.forEach(outputAllocation)
        }
        outputAllocation.copyTo(outputBitmap)
        rs.destroy()

        return outputBitmap
    }

    private fun blurImageAsync() = async {
        blurImage()
    }
}
