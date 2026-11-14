package com.ciweihaha.alisttv.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ciweihaha.alisttv.R
import com.ciweihaha.alisttv.api.AlistRepository
import kotlinx.coroutines.launch

/**
 * 图片全屏查看：调 /api/fs/get 拿 raw_url，下载后解码显示。
 * 遥控器左/右在当前目录的图片之间翻页，返回键退出。
 */
class ImageViewerActivity : FragmentActivity() {

    private lateinit var imageView: ImageView
    private lateinit var hintView: TextView
    private lateinit var repository: AlistRepository
    private var paths: List<String> = emptyList()
    private var index: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        imageView = findViewById(R.id.image_view)
        hintView = findViewById(R.id.image_hint)
        repository = AlistRepository(this)
        paths = intent.getStringArrayListExtra(EXTRA_PATHS) ?: emptyList()
        index = intent.getIntExtra(EXTRA_INDEX, 0)
            .coerceIn(0, (paths.size - 1).coerceAtLeast(0))
        showImage()
    }

    private fun showImage() {
        if (paths.isEmpty()) {
            finish()
            return
        }
        val path = paths[index]
        hintView.text = "${index + 1} / ${paths.size}  $path"
        lifecycleScope.launch {
            try {
                val url = repository.getRawUrl(path)
                val bytes = repository.fetchImageBytes(url)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(
                        this@ImageViewerActivity,
                        R.string.image_decode_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImageViewerActivity,
                    getString(R.string.image_load_failed, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (index > 0) {
                    index--
                    showImage()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (index < paths.size - 1) {
                    index++
                    showImage()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_PATHS = "extra_paths"
        const val EXTRA_INDEX = "extra_index"
    }
}
