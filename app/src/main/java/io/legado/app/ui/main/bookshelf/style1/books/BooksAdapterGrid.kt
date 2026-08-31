package io.legado.app.ui.main.bookshelf.style1.books

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemBookshelfGrid2Binding
import io.legado.app.databinding.ItemBookshelfGridBinding
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.widget.text.BadgeView
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import splitties.views.onLongClick
import kotlin.math.roundToInt

class BooksAdapterGrid(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<ViewBinding>(context) {
    private val showBookname = AppConfig.showBookname
    override fun getViewBinding(parent: ViewGroup): ViewBinding {
        return when (showBookname) {
            2 -> ItemBookshelfGrid2Binding.inflate(inflater, parent, false)
            else -> ItemBookshelfGridBinding.inflate(inflater, parent, false)
        }
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ViewBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        when (binding) {
            is ItemBookshelfGridBinding -> binding.run {
                if (payloads.isEmpty()) {
                    if (showBookname == 0) {
                        tvName.visible()
                        tvName.text = item.name
                    } else {
                        tvName.gone()
                    }
                    ivCover.load(item, false)
                    bindReadProgress(item, tvReadProgress, pbReadProgress)
                    upRefresh(binding, item)
                } else {
                    for (i in payloads.indices) {
                        val bundle = payloads[i] as Bundle
                        bundle.keySet().forEach {
                            when (it) {
                                "name" -> tvName.text = item.name
                                "cover" -> ivCover.load(
                                    item,
                                    false
                                )

                                "refresh" -> upRefresh(binding, item)
                                "progress" -> bindReadProgress(item, tvReadProgress, pbReadProgress)
                            }
                        }
                    }
                }
            }
            is ItemBookshelfGrid2Binding -> binding.run {
                if (payloads.isEmpty()) {
                    tvName.text = item.name
                    ivCover.load(item, false)
                    bindReadProgress(item, tvReadProgress, pbReadProgress)
                    upRefresh(binding, item)
                } else {
                    for (i in payloads.indices) {
                        val bundle = payloads[i] as Bundle
                        bundle.keySet().forEach {
                            when (it) {
                                "name" -> tvName.text = item.name
                                "cover" -> ivCover.load(
                                    item,
                                    false
                                )

                                "refresh" -> upRefresh(binding, item)
                                "progress" -> bindReadProgress(item, tvReadProgress, pbReadProgress)
                            }
                        }
                    }
                }
            }
        }

    }

    private fun upRefresh(binding: ViewBinding, item: Book) {
        when (binding) {
            is ItemBookshelfGridBinding -> binding.run {
                if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
                    bvUnread.invisible()
                    rlLoading.visible()
                } else {
                    rlLoading.inVisible()
                    if (AppConfig.showUnread) {
                        bvUnread.setBadgeCount(item.getUnreadChapterNum())
                        bvUnread.setHighlight(item.lastCheckCount > 0)
                    } else {
                        bvUnread.invisible()
                    }
                }
            }
            is ItemBookshelfGrid2Binding -> binding.run {
                if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
                    bvUnread.invisible()
                    rlLoading.visible()
                } else {
                    rlLoading.inVisible()
                    if (AppConfig.showUnread) {
                        bvUnread.setBadgeCount(item.getUnreadChapterNum())
                        bvUnread.setHighlight(item.lastCheckCount > 0)
                    } else {
                        bvUnread.invisible()
                    }
                }
            }
        }
    }

    private fun bindReadProgress(item: Book, textView: BadgeView, progressBar: ProgressBar) {
        val progress = readProgressPercent(item)
        val context = textView.context
        val accent = ThemeStore.accentColor(context)
        val background = ContextCompat.getColor(context, R.color.background)
        val isFinished = isReadFinished(item, progress)
        val badgeColor = if (isFinished) accent else 0x99000000.toInt()
        val progressColor = if (AppConfig.isNightTheme) {
            ContextCompat.getColor(context, R.color.bookshelf_progress_dark)
        } else {
            0x99000000.toInt()
        }
        val track = ColorUtils.blendColors(background, progressColor, 0.12f)
        textView.setBackgroundColor(badgeColor)
        textView.setTextColor(Color.WHITE)
        progressBar.progressTintList = ColorStateList.valueOf(progressColor)
        progressBar.progressBackgroundTintList = ColorStateList.valueOf(track)
        textView.text = if (isFinished) {
            context.getString(R.string.read_finished)
        } else {
            "$progress%"
        }
        progressBar.progress = progress
    }

    private fun readProgressPercent(item: Book): Int {
        val total = item.totalChapterNum
        if (total <= 0) return 0
        val current = (item.durChapterIndex + 1).coerceIn(0, total)
        return (current * 100f / total).roundToInt().coerceIn(0, 100)
    }

    private fun isReadFinished(item: Book, progress: Int): Boolean {
        return item.totalChapterNum > 0 && progress >= 100
    }

    override fun registerListener(holder: ItemViewHolder, binding: ViewBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.open(it)
                }
            }

            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openBookInfo(it)
                }
            }
        }
    }
}
