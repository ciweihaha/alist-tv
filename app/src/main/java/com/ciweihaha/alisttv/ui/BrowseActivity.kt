package com.ciweihaha.alisttv.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.ciweihaha.alisttv.R
import com.ciweihaha.alisttv.util.Prefs

/**
 * 启动器入口（LEANBACK_LAUNCHER）。
 * 没登录过先跳登录页；返回键交给 BrowseFragment 回上级目录；
 * 菜单键（部分遥控器有）直接打开设置。
 */
class BrowseActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
        if (!Prefs(this).isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.browse_container, BrowseFragment())
                .commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.browse_container) as? BrowseFragment
            if (fragment != null && fragment.goBack()) return true
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
