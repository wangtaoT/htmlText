package com.wt.htmltext.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.wt.htmltext.HtmlImageLoader
import com.wt.htmltext.HtmlText
import com.wt.htmltext.OnTagClickListener
import com.wt.htmltext.demo.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setDescText()
    }

    private fun setDescText() {
        HtmlText.from(getSample())
            .setImageLoader(object : HtmlImageLoader {
                override fun loadImage(url: String?, callback: HtmlImageLoader.Callback) {
                    Glide.with(this@MainActivity)
                        .load<Any>(url)
                        .asBitmap()
                        .into(object : SimpleTarget<Bitmap?>() {
                            override fun onResourceReady(
                                resource: Bitmap?,
                                glideAnimation: GlideAnimation<in Bitmap?>?
                            ) {
                                callback.onLoadComplete(resource)
                            }

                            override fun onLoadFailed(e: Exception?, errorDrawable: Drawable?) {
                                callback.onLoadFailed()
                            }
                        })
                }

                override val defaultDrawable: Drawable
                    get() = ContextCompat.getDrawable(
                        this@MainActivity,
                        R.mipmap.image_placeholder_loading
                    )!!
                override val errorDrawable: Drawable
                    get() = ContextCompat.getDrawable(
                        this@MainActivity,
                        R.mipmap.image_placeholder_fail
                    )!!
                override val maxWidth: Int
                    get() = getTextWidth()

                override fun fitWidth(): Boolean {
                    return false
                }
            })
            .setOnTagClickListener(object : OnTagClickListener {
                override fun onImageClick(
                    context: Context?,
                    imageUrlList: List<String?>,
                    position: Int
                ) {
                    //TODO 点击图片
                }

                override fun onLinkClick(context: Context?, url: String) {
                    //TODO 点击链接
                }
            })
            .into(binding.tvContent)
    }

    private fun getSample(): String? {
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.sample)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
            return sb.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getTextWidth(): Int {
        val dm = resources.displayMetrics
        return dm.widthPixels - binding.tvContent.paddingLeft - binding.tvContent.paddingRight
    }
}