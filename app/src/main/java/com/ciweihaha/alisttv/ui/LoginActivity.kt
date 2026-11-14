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
 * 首次启动的登录页：GuidedStep，让用户填服务器地址、用户名、密码。
 * 服务器地址默认 https://openlist.ciweihaha.win。
 * 登录成功后保存 token 和地址，之后启动自动登录。
 */
class LoginActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guided)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.add(
                supportFragmentManager, LoginStepFragment(), R.id.guided_container
            )
        }
    }

    class LoginStepFragment : GuidedStepSupportFragment() {
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
                getString(R.string.login_title),
                getString(R.string.login_desc),
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
                    .id(ACTION_LOGIN)
                    .title(getString(R.string.login))
                    .build()
            )
        }

        override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
            return GuidedAction.ACTION_ID_NEXT
        }

        override fun onGuidedActionClicked(action: GuidedAction) {
            if (action.id != ACTION_LOGIN || busy) return
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
                    startActivity(Intent(requireContext(), BrowseActivity::class.java))
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

        companion object {
            private const val ACTION_SERVER = 1L
            private const val ACTION_USERNAME = 2L
            private const val ACTION_PASSWORD = 3L
            private const val ACTION_LOGIN = 4L
        }
    }
}
