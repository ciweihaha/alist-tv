package com.ciweihaha.alisttv.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.ciweihaha.alisttv.R
import com.ciweihaha.alisttv.api.AlistRepository
import com.ciweihaha.alisttv.util.Prefs
import kotlinx.coroutines.launch

/**
 * 设置页：改服务器地址和账号（保存后重新登录），或退出登录回到登录页。
 * 入口：浏览页第一个“设置”卡片，或遥控器菜单键。
 */
class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guided)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.add(
                supportFragmentManager, SettingsStepFragment(), R.id.guided_container
            )
        }
    }

    class SettingsStepFragment : GuidedStepSupportFragment() {
        private lateinit var repository: AlistRepository
        private lateinit var prefs: Prefs
        private var busy = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            prefs = Prefs(requireContext())
            repository = AlistRepository(requireContext())
        }

        override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
            return GuidanceStylist.Guidance(
                getString(R.string.settings_title),
                getString(R.string.settings_desc),
                "",
                null
            )
        }

        override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_SERVER)
                    .title(getString(R.string.server_address))
                    .description(prefs.server)
                    .descriptionEditable(true)
                    .descriptionEditInputType(
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                    )
                    .build()
            )
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_USERNAME)
                    .title(getString(R.string.username))
                    .description(prefs.username)
                    .descriptionEditable(true)
                    .descriptionEditInputType(InputType.TYPE_CLASS_TEXT)
                    .build()
            )
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_PASSWORD)
                    .title(getString(R.string.password))
                    .description(prefs.password)
                    .descriptionEditable(true)
                    .descriptionEditInputType(
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    )
                    .build()
            )
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_SAVE)
                    .title(getString(R.string.save_and_login))
                    .build()
            )
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_LOGOUT)
                    .title(getString(R.string.logout))
                    .build()
            )
        }

        override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
            return GuidedAction.ACTION_ID_NEXT
        }

        override fun onGuidedActionClicked(action: GuidedAction) {
            when (action.id) {
                ACTION_SAVE -> saveAndLogin()
                ACTION_LOGOUT -> logout()
            }
        }

        private fun saveAndLogin() {
            if (busy) return
            val server = (findActionById(ACTION_SERVER)?.description ?: "").toString().trim()
            val username = (findActionById(ACTION_USERNAME)?.description ?: "").toString().trim()
            val password = (findActionById(ACTION_PASSWORD)?.description ?: "").toString()
            if (server.isBlank() || username.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), R.string.login_empty, Toast.LENGTH_SHORT).show()
                return
            }
            busy = true
            lifecycleScope.launch {
                try {
                    repository.login(server, username, password)
                    val intent = Intent(requireContext(), BrowseActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.login_failed, e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    busy = false
                }
            }
        }

        private fun logout() {
            prefs.clearAll()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        companion object {
            private const val ACTION_SERVER = 1L
            private const val ACTION_USERNAME = 2L
            private const val ACTION_PASSWORD = 3L
            private const val ACTION_SAVE = 4L
            private const val ACTION_LOGOUT = 5L
        }
    }
}
