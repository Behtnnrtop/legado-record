package io.legado.app.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ActivityReadRecordMergeBinding
import io.legado.app.databinding.ItemReadRecordMergeBookBinding
import io.legado.app.databinding.ItemReadRecordMergeHeaderBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.ReadRecordDisplayItem
import io.legado.app.model.ReadRecordTotalModel
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadRecordMergeActivity : BaseActivity<ActivityReadRecordMergeBinding>() {

    private val adapter by lazy { MergeAdapter() }
    private val selectedTargets = linkedMapOf<String, Book>()
    private var pendingLegacyItems = emptyList<ReadRecordDisplayItem>()

    override val binding by viewBinding(ActivityReadRecordMergeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
        binding.btnConfirmMerge.setOnClickListener {
            confirmMerge()
        }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val rows = withContext(IO) {
                buildRows()
            }
            binding.tvEmpty.isVisible = rows.isEmpty()
            binding.recyclerView.isVisible = rows.isNotEmpty()
            binding.btnConfirmMerge.isVisible = rows.isNotEmpty()
            adapter.setItems(rows)
        }
    }

    private fun buildRows(): List<MergeRow> {
        val rows = mutableListOf<MergeRow>()
        pendingLegacyItems = ReadRecordTotalModel.getPendingLegacyItems()
        selectedTargets.keys.retainAll(pendingLegacyItems.map { it.displayKey }.toSet())
        pendingLegacyItems.forEach { legacyItem ->
            rows.add(MergeRow.Header(legacyItem))
            ReadRecordTotalModel.getCandidates(legacyItem.bookName).forEach { book ->
                val readTime = appDb.readRecordDao
                    .getReadTimeByKey(io.legado.app.data.entities.ReadRecord.urlRecordKey(book.bookUrl))
                    ?: 0L
                rows.add(MergeRow.BookTarget(legacyItem, book, readTime))
            }
        }
        return rows
    }

    private fun confirmMerge() {
        if (pendingLegacyItems.isEmpty()) return
        val selectedItems = pendingLegacyItems.mapNotNull { legacyItem ->
            selectedTargets[legacyItem.displayKey]?.let { book -> legacyItem to book }
        }
        if (selectedItems.size != pendingLegacyItems.size) {
            toastOnUi(R.string.read_record_merge_select_required)
            return
        }
        alert(R.string.read_record_merge) {
            setMessage(getString(R.string.read_record_merge_confirm_selected))
            yesButton {
                lifecycleScope.launch {
                    withContext(IO) {
                        selectedItems.forEach { (legacyItem, book) ->
                            ReadRecordTotalModel.mergeLegacyToBook(legacyItem, book)
                        }
                    }
                    selectedTargets.clear()
                    toastOnUi(R.string.read_record_merge_done)
                    loadData()
                }
            }
            noButton()
        }
    }

    private fun formatDuring(mss: Long): String {
        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val d = if (days > 0) "${days}天" else ""
        val h = if (hours > 0) "${hours}小时" else ""
        val m = if (minutes > 0) "${minutes}分钟" else ""
        val s = if (seconds > 0) "${seconds}秒" else ""
        return "$d$h$m$s".ifBlank { "0秒" }
    }

    inner class MergeAdapter : RecyclerView.Adapter<MergeViewHolder>() {

        private val items = arrayListOf<MergeRow>()

        fun setItems(newItems: List<MergeRow>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is MergeRow.Header -> VIEW_TYPE_HEADER
                is MergeRow.BookTarget -> VIEW_TYPE_BOOK
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MergeViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_TYPE_HEADER -> MergeViewHolder.Header(
                    ItemReadRecordMergeHeaderBinding.inflate(inflater, parent, false)
                )

                else -> MergeViewHolder.BookTarget(
                    ItemReadRecordMergeBookBinding.inflate(inflater, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: MergeViewHolder, position: Int) {
            when (val row = items[position]) {
                is MergeRow.Header -> {
                    val binding = (holder as MergeViewHolder.Header).binding
                    binding.tvBookName.text = getString(
                        R.string.read_record_merge_legacy_title,
                        row.legacyItem.bookName
                    )
                    binding.tvReadingTime.text = getString(
                        R.string.read_record_merge_legacy_time,
                        formatDuring(row.legacyItem.readTime)
                    )
                }

                is MergeRow.BookTarget -> {
                    val binding = (holder as MergeViewHolder.BookTarget).binding
                    binding.tvName.text = row.book.name
                    binding.tvAuthor.text = getString(R.string.author_show, row.book.author)
                    binding.tvOrigin.text = row.book.originName.ifBlank { row.book.origin }
                    binding.tvReadingTime.text = getString(
                        R.string.read_record_merge_target_time,
                        formatDuring(row.readTime)
                    )
                    binding.rbSelected.isChecked =
                        selectedTargets[row.legacyItem.displayKey]?.bookUrl == row.book.bookUrl
                    binding.ivCover.load(row.book, AppConfig.loadCoverOnlyWifi)
                    binding.root.setOnClickListener {
                        selectedTargets[row.legacyItem.displayKey] = row.book
                        notifyDataSetChanged()
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    sealed class MergeViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        class Header(val binding: ItemReadRecordMergeHeaderBinding) : MergeViewHolder(binding)
        class BookTarget(val binding: ItemReadRecordMergeBookBinding) : MergeViewHolder(binding)
    }

    sealed class MergeRow {
        data class Header(val legacyItem: ReadRecordDisplayItem) : MergeRow()
        data class BookTarget(
            val legacyItem: ReadRecordDisplayItem,
            val book: Book,
            val readTime: Long,
        ) : MergeRow()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_BOOK = 2

        fun start(context: Context) {
            context.startActivity(Intent(context, ReadRecordMergeActivity::class.java))
        }
    }
}
