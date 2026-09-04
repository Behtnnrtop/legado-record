package io.legado.app.ui.main.readrecord

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.base.adapter.DiffRecyclerAdapter
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemBookReviewOverviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookReviewOverviewAdapter(
    context: Context,
    private val fragment: Fragment,
    private val lifecycle: Lifecycle,
    private val callBack: CallBack
) : DiffRecyclerAdapter<Book, ItemBookReviewOverviewBinding>(context) {

    private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    override val keepScrollPosition = true

    override val diffItemCallback: DiffUtil.ItemCallback<Book> =
        object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.name == newItem.name
                        && oldItem.author == newItem.author
                        && oldItem.review == newItem.review
                        && oldItem.reviewCreateTime == newItem.reviewCreateTime
                        && oldItem.reviewUpdateTime == newItem.reviewUpdateTime
                        && oldItem.rating == newItem.rating
                        && oldItem.getDisplayCover() == newItem.getDisplayCover()
            }

            override fun getChangePayload(oldItem: Book, newItem: Book): Any? {
                val bundle = bundleOf()
                if (oldItem.name != newItem.name) bundle.putString("name", newItem.name)
                if (oldItem.author != newItem.author) bundle.putString("author", newItem.author)
                if (oldItem.review != newItem.review) bundle.putString("review", newItem.review)
                if (oldItem.reviewCreateTime != newItem.reviewCreateTime) {
                    bundle.putLong("reviewCreateTime", newItem.reviewCreateTime)
                }
                if (oldItem.rating != newItem.rating) bundle.putFloat("rating", newItem.rating)
                if (oldItem.getDisplayCover() != newItem.getDisplayCover()) {
                    bundle.putString("cover", newItem.getDisplayCover())
                }
                if (bundle.isEmpty) return null
                return bundle
            }
        }

    override fun getViewBinding(parent: ViewGroup): ItemBookReviewOverviewBinding {
        return ItemBookReviewOverviewBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookReviewOverviewBinding,
        item: Book,
        payloads: MutableList<Any>
    ) = binding.run {
        if (payloads.isEmpty()) {
            bindAll(this, item)
        } else {
            payloads.filterIsInstance<Bundle>().forEach { bundle ->
                bundle.keySet().forEach {
                    when (it) {
                        "name" -> tvName.text = item.name
                        "author" -> tvAuthor.text = item.author
                        "review" -> tvReview.setReviewPreview(item.review.orEmpty())
                        "reviewCreateTime" -> tvReviewTime.text = formatReviewTime(item)
                        "rating" -> bookRatingBar.rating = item.rating
                        "cover" -> ivCover.load(item, false, fragment, lifecycle)
                    }
                }
            }
        }
    }

    private fun bindAll(binding: ItemBookReviewOverviewBinding, item: Book) = binding.run {
        tvName.text = item.name
        tvAuthor.text = item.author
        tvReview.setReviewPreview(item.review.orEmpty())
        tvReviewTime.text = formatReviewTime(item)
        bookRatingBar.rating = item.rating
        ivCover.load(item, false, fragment, lifecycle)
    }

    private fun formatReviewTime(item: Book): String {
        if (item.reviewCreateTime <= 0L) return ""
        return dateFormat.format(Date(item.reviewCreateTime))
    }

    private fun TextView.setReviewPreview(source: String) {
        tag = source
        text = source
        post {
            if (tag != source) return@post
            val availableTextWidth = width - compoundPaddingLeft - compoundPaddingRight
            if (availableTextWidth <= 0) return@post
            val textLayout = createPreviewLayout(source, availableTextWidth)
            val maxLineCount = maxLines
            if (textLayout.lineCount <= maxLineCount) return@post

            val lastLine = maxLineCount - 1
            var previewEnd = textLayout.getLineVisibleEnd(lastLine)
            var preview: String
            while (true) {
                preview = source.substring(0, previewEnd).trimEnd() + "..."
                if (createPreviewLayout(preview, availableTextWidth).lineCount <= maxLineCount ||
                    previewEnd == 0
                ) {
                    break
                }
                previewEnd = source.offsetByCodePoints(previewEnd, -1)
            }
            if (tag == source) text = preview
        }
    }

    private fun TextView.createPreviewLayout(value: CharSequence, layoutWidth: Int): StaticLayout {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder.obtain(value, 0, value.length, paint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setIncludePad(includeFontPadding)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            }
            return builder.build()
        }
        @Suppress("DEPRECATION")
        return StaticLayout(
            value,
            paint,
            layoutWidth,
            Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier,
            lineSpacingExtra,
            includeFontPadding
        )
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookReviewOverviewBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.openReview(it)
            }
        }
    }

    override fun onViewRecycled(holder: ItemViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.setOnClickListener(null)
    }

    interface CallBack {
        fun openReview(book: Book)
    }

}
