package com.ciweihaha.alisttv.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import com.ciweihaha.alisttv.R
import com.ciweihaha.alisttv.api.AlistRepository
import com.ciweihaha.alisttv.api.AuthException
import com.ciweihaha.alisttv.api.FsItem
import com.ciweihaha.alisttv.util.FileUtils
import kotlinx.coroutines.launch

/**
 * 目录浏览：Leanback 垂直网格，文件夹在前、按名字排序。
 * 遥控器方向键移动焦点，确认键进文件夹或播放/查看，返回键回上级目录。
 * 顶部标题栏显示当前路径（面包屑），根目录就是各网盘挂载点。
 */
class BrowseFragment : VerticalGridSupportFragment() {

    private lateinit var repository: AlistRepository
    private val browseAdapter = ArrayObjectAdapter(BrowseItemPresenter())
    private val pathStack = ArrayDeque<String>()
    private var currentPath = "/"
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AlistRepository(requireContext())
        gridPresenter = VerticalGridPresenter().apply { numberOfColumns = 4 }
        adapter = browseAdapter
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            onItemClicked(item)
        }
        loadPath("/")
    }

    /** 返回键处理：能回上级返回 true，否则 false（由 Activity 退出）。 */
    fun goBack(): Boolean {
        if (pathStack.isEmpty()) return false
        loadPath(pathStack.removeLast())
        return true
    }

    private fun onItemClicked(item: Any?) {
        if (loading) return
        when (val browseItem = item as? BrowseItem ?: return) {
            is BrowseItem.Settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
            is BrowseItem.File -> {
                val fs = browseItem.fs
                if (fs.isDir) {
                    pathStack.addLast(currentPath)
                    loadPath(FileUtils.joinPath(currentPath, fs.name))
                } else if (FileUtils.isVideo(fs.name)) {
                    val intent = Intent(requireContext(), PlayerActivity::class.java)
                    intent.putExtra(PlayerActivity.EXTRA_PATH, FileUtils.joinPath(currentPath, fs.name))
                    intent.putExtra(PlayerActivity.EXTRA_TITLE, fs.name)
                    startActivity(intent)
                } else if (FileUtils.isImage(fs.name)) {
                    openImage(fs)
                } else {
                    toast(getString(R.string.unsupported_file), Toast.LENGTH_SHORT)
                }
            }
        }
    }

    /** 打开图片查看器，并把当前目录里的全部图片路径传过去，方便左右翻页。 */
    private fun openImage(fs: FsItem) {
        val paths = currentItems()
            .filterIsInstance<BrowseItem.File>()
            .map { it.fs }
            .filter { !it.isDir && FileUtils.isImage(it.name) }
            .map { FileUtils.joinPath(currentPath, it.name) }
        val index = paths
            .indexOf(FileUtils.joinPath(currentPath, fs.name))
            .coerceAtLeast(0)
        val intent = Intent(requireContext(), ImageViewerActivity::class.java)
        intent.putStringArrayListExtra(ImageViewerActivity.EXTRA_PATHS, ArrayList(paths))
        intent.putExtra(ImageViewerActivity.EXTRA_INDEX, index)
        startActivity(intent)
    }

    private fun currentItems(): List<BrowseItem> {
        val list = mutableListOf<BrowseItem>()
        for (i in 0 until browseAdapter.size()) {
            (browseAdapter[i] as? BrowseItem)?.let { list.add(it) }
        }
        return list
    }

    private fun loadPath(path: String) {
        currentPath = path
        title = if (path == "/") getString(R.string.all_drives) else path
        loading = true
        browseAdapter.clear()
        lifecycleScope.launch {
            try {
                val files = repository.listFiles(path)
                val sorted = files.sortedWith(
                    compareByDescending<FsItem> { it.isDir }
                        .thenBy { it.name.lowercase() }
                )
                browseAdapter.add(BrowseItem.Settings)
                sorted.forEach { browseAdapter.add(BrowseItem.File(it)) }
                if (sorted.isEmpty()) {
                    title = "$path ${getString(R.string.empty_dir)}"
                }
            } catch (e: AuthException) {
                toast(getString(R.string.login_expired), Toast.LENGTH_LONG)
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            } catch (e: Exception) {
                toast(getString(R.string.load_failed, e.message ?: ""), Toast.LENGTH_LONG)
            } finally {
                loading = false
            }
        }
    }

    private fun toast(message: String, duration: Int) {
        context?.let { Toast.makeText(it, message, duration).show() }
    }
}
