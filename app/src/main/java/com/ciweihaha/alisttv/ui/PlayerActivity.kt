package com.ciweihaha.alisttv.ui

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ciweihaha.alisttv.R
import com.ciweihaha.alisttv.api.AlistRepository
import com.ciweihaha.alisttv.api.AuthException
import kotlinx.coroutines.launch

/**
 * 视频全屏播放：先调 /api/fs/get 拿 raw_url，再用 Media3 ExoPlayer 播放。
 * 遥控器：确认键播放/暂停，左/右快退/快进 10 秒，返回键退出。
 * 播放器控制条显示时，方向键先交给控制条（选按钮）；控制条隐藏时才直接快进快退。
 */
class PlayerActivity : FragmentActivity() {

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private var path: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.player_view)
        playerView.controllerShowTimeoutMs = 3000
        path = intent.getStringExtra(EXTRA_PATH) ?: ""
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initPlayer() {
        if (path.isBlank()) {
            finish()
            return
        }
        val exo = ExoPlayer.Builder(this).build()
        player = exo
        playerView.player = exo
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.play_failed, error.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        })
        lifecycleScope.launch {
            try {
                val url = AlistRepository(this@PlayerActivity).getRawUrl(path)
                // 取直链期间用户可能已退出：播放器已被释放就不再继续。
                if (player !== exo) return@launch
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.play()
            } catch (e: AuthException) {
                Toast.makeText(
                    this@PlayerActivity,
                    R.string.login_expired,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.cannot_play, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        playerView.player = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val exo = player
        if (exo != null && !playerView.isControllerFullyVisible) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    if (exo.isPlaying) exo.pause() else exo.play()
                    playerView.showController()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                    // duration 未知（直播流等）时为负：旧代码 coerce 到 0，
                    // 按"右"反而跳回片头；此时不设上界，正常向前。
                    val duration = exo.duration
                    val target = if (duration > 0) {
                        (exo.currentPosition + SEEK_MS).coerceAtMost(duration)
                    } else {
                        exo.currentPosition + SEEK_MS
                    }
                    exo.seekTo(target)
                    playerView.showController()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                    val target = (exo.currentPosition - SEEK_MS).coerceAtLeast(0)
                    exo.seekTo(target)
                    playerView.showController()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_TITLE = "extra_title"
        private const val SEEK_MS = 10_000L
    }
}
