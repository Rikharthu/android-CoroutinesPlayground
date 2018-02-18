package com.example.coroutinesplayground

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Looper
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    private var blurImageJob: Job? = null

    private lateinit var service: GithubService

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
                blurImageJob = launch(UI) {
                    val start = System.currentTimeMillis()
                    val blurryImage = blurImageAsync().await()
                    val end = System.currentTimeMillis()
                    Log.d("MainActivity", "Time to blur: ${end - start}ms")
                    blurryImageView.setImageBitmap(blurryImage)
                }
            } else {
                if (blurImageJob != null) {
                    blurImageJob?.cancel()
                    blurImageJob = null
                }
                unblurImage()
            }
        }

        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        service = retrofit.create<GithubService>(GithubService::class.java)

        // Specify UI context, thus launch can operate on UI thread
        // TODO why does it run on ui thread, but it isnt blocked because of .await()
        // because .await() is not block. It returns and continues to execute when it's finished
        launch(UI) {
            val runningOnUiThread = Looper.getMainLooper().thread == Thread.currentThread() // true
            Log.d("MainActivity", "Running on UI thread? $runningOnUiThread")
            val repos = getReposAsync("google").await()
            processRepos(repos)
            blurSwitch.text = "${blurSwitch.text}+"
        }
    }

    private fun processRepos(repos: List<Repo>) {
        for (repo in repos) {
            Log.d("MainActivity", repo.toString())
        }
    }

    // Wrap retrofit call inside a suspend function
    // TODO often this wrapping is provided by extension functions
    private fun callRepos(user: String): List<Repo> {
        try {
            Thread.sleep(10000)
            val response = service.listRepos(user).execute()
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", t.message)
        }
        return emptyList()
    }

    private fun getReposAsync(user: String) = async {
        callRepos("google")
    }

    private suspend fun getRepos(user: String): List<Repo> {
        val repos = getReposAsync(user).await()
        return repos
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
