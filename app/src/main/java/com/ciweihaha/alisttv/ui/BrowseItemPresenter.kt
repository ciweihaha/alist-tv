package com.ciweihaha.alisttv.ui

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.ciweihaha.alisttv.R
import com.ciweihaha.alisttv.api.FsItem
import com.ciweihaha.alisttv.util.FileUtils

/** 网格里的一项：文件/文件夹，或排在第一位的“设置”卡片。 */
sealed class BrowseItem {
    data class File(val fs: FsItem) : BrowseItem()
    object Settings : BrowseItem()
}

/** 把 BrowseItem 画成 Leanback 的 ImageCardView 卡片。 */
class BrowseItemPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.cardType = ImageCardView.CARD_TYPE_INFO_UNDER
        cardView.setInfoVisibility(ImageCardView.CARD_REGION_VISIBLE_ALWAYS)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        when (val browseItem = item as BrowseItem) {
            is BrowseItem.Settings -> {
                cardView.titleText = cardView.context.getString(R.string.settings_card)
                cardView.contentText = cardView.context.getString(R.string.settings_card_desc)
                cardView.mainImage =
                    ContextCompat.getDrawable(cardView.context, R.drawable.ic_settings)
            }
            is BrowseItem.File -> {
                val fs = browseItem.fs
                cardView.titleText = fs.name
                cardView.contentText =
                    if (fs.isDir) cardView.context.getString(R.string.folder)
                    else FileUtils.formatSize(fs.size)
                val iconRes = when {
                    fs.isDir -> R.drawable.ic_folder
                    FileUtils.isVideo(fs.name) -> R.drawable.ic_video
                    FileUtils.isImage(fs.name) -> R.drawable.ic_image
                    FileUtils.isAudio(fs.name) -> R.drawable.ic_music
                    else -> R.drawable.ic_file
                }
                cardView.mainImage = ContextCompat.getDrawable(cardView.context, iconRes)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
        cardView.badgeImage = null
    }

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}
