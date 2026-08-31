package io.legado.app.ui.main.readrecord

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReadRecordTopBook
import io.legado.app.databinding.FragmentReadRecordBinding
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.ui.widget.readrecord.ReadRecordHeatmapHelper
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import io.legado.app.utils.gone
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.visible
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.roundToInt

class ReadRecordOverviewFragment :
    VMBaseFragment<ReadRecordViewModel>(R.layout.fragment_read_record) {

    override val viewModel by viewModels<ReadRecordViewModel>()
    private val binding by viewBinding(FragmentReadRecordBinding::bind)
    private val titleView: TextView by lazy {
        binding.tvTotalReadTime
    }
    private val recentBookViews: List<RecentBookView> by lazy {
        listOf(
            RecentBookView(
                binding.llRecentBook1,
                binding.ivRecentCover1,
                binding.tvRecentName1,
                binding.tvRecentProgress1,
                binding.pbRecentProgress1
            ),
            RecentBookView(
                binding.llRecentBook2,
                binding.ivRecentCover2,
                binding.tvRecentName2,
                binding.tvRecentProgress2,
                binding.pbRecentProgress2
            ),
            RecentBookView(
                binding.llRecentBook3,
                binding.ivRecentCover3,
                binding.tvRecentName3,
                binding.tvRecentProgress3,
                binding.pbRecentProgress3
            ),
            RecentBookView(
                binding.llRecentBook4,
                binding.ivRecentCover4,
                binding.tvRecentName4,
                binding.tvRecentProgress4,
                binding.pbRecentProgress4
            )
        )
    }
    private val topBookViews: List<TopBookView> by lazy {
        listOf(
            TopBookView(
                binding.llTopBook1,
                binding.tvTopBookRank1,
                binding.ivTopBookCover1,
                binding.tvTopBookName1,
                binding.tvTopBookAuthor1,
                binding.tvTopBookTime1,
                binding.pbTopBookProgress1
            ),
            TopBookView(
                binding.llTopBook2,
                binding.tvTopBookRank2,
                binding.ivTopBookCover2,
                binding.tvTopBookName2,
                binding.tvTopBookAuthor2,
                binding.tvTopBookTime2,
                binding.pbTopBookProgress2
            ),
            TopBookView(
                binding.llTopBook3,
                binding.tvTopBookRank3,
                binding.ivTopBookCover3,
                binding.tvTopBookName3,
                binding.tvTopBookAuthor3,
                binding.tvTopBookTime3,
                binding.pbTopBookProgress3
            ),
            TopBookView(
                binding.llTopBook4,
                binding.tvTopBookRank4,
                binding.ivTopBookCover4,
                binding.tvTopBookName4,
                binding.tvTopBookAuthor4,
                binding.tvTopBookTime4,
                binding.pbTopBookProgress4
            )
        )
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val background = requireContext().bottomBackground
        binding.root.setBackgroundColor(background)
        binding.titleBar.setBackgroundColor(background)
        binding.titleBar.gone()
        binding.nsvContent.setBackgroundColor(background)
        binding.hsvHeatmap.setBackgroundColor(background)
        tintRecentReadProgress()
        tintTopBookProgress()
        binding.readRecordHeatmap.setOnDayClickListener { date, readTime ->
            toastOnUi("$date 阅读${ReadRecordHeatmapHelper.formatDuration(readTime)}")
        }
        binding.tvPeriodYear.setOnClickListener {
            viewModel.switchPeriodMode(ReadRecordPeriodMode.YEAR)
        }
        binding.tvPeriodMonth.setOnClickListener {
            viewModel.switchPeriodMode(ReadRecordPeriodMode.MONTH)
        }
        binding.tvPeriodWeek.setOnClickListener {
            viewModel.switchPeriodMode(ReadRecordPeriodMode.WEEK)
        }
        binding.tvPeriodPrevious.setOnClickListener {
            viewModel.previousPeriod()
        }
        binding.tvPeriodNext.setOnClickListener {
            viewModel.nextPeriod()
        }
        binding.tvWeekBarPrevious.setOnClickListener {
            viewModel.previousWeekBar()
        }
        binding.tvWeekBarNext.setOnClickListener {
            viewModel.nextWeekBar()
        }
        binding.readRecordWeekBarChart.setOnBarClickListener { date, readTime ->
            toastOnUi("$date 阅读${ReadRecordHeatmapHelper.formatDuration(readTime)}")
        }
        viewModel.heatmapLiveData.observe(viewLifecycleOwner) {
            titleView.text = buildTitleText(it.totalReadTime, it.bookCount)
            binding.readRecordHeatmap.setData(it)
            binding.hsvHeatmap.post {
                binding.hsvHeatmap.fullScroll(View.FOCUS_RIGHT)
            }
        }
        viewModel.recentReadBooksLiveData.observe(viewLifecycleOwner) {
            bindRecentReadBooks(it)
        }
        viewModel.periodSummaryLiveData.observe(viewLifecycleOwner) {
            bindPeriodSummary(it)
        }
        viewModel.topBooksLiveData.observe(viewLifecycleOwner) {
            bindTopBooks(it)
        }
        viewModel.weekBarLiveData.observe(viewLifecycleOwner) {
            bindWeekBars(it)
        }
        viewModel.hourHistogramLiveData.observe(viewLifecycleOwner) {
            binding.readRecordHourHistogram.setCounts(it.counts)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHeatmap()
        viewModel.loadRecentReadBooks()
        viewModel.loadPeriodSummary()
        viewModel.loadWeekBars()
    }

    private fun bindRecentReadBooks(books: List<Book>) {
        binding.tvRecentReadEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
        recentBookViews.forEachIndexed { index, recentBookView ->
            val book = books.getOrNull(index)
            if (book == null) {
                recentBookView.container.gone()
            } else {
                recentBookView.container.visible()
                recentBookView.cover.load(book, false)
                recentBookView.name.text = book.name
                bindReadProgress(recentBookView, book)
                recentBookView.container.setOnClickListener {
                    startActivityForBook(book)
                }
            }
        }
    }

    private fun buildTitleText(totalReadTime: Long, bookCount: Int): SpannableString {
        val duration = ReadRecordHeatmapHelper.formatDurationWithSpaces(totalReadTime)
        val text = getString(R.string.read_record_total_summary, duration, bookCount)
        val spannable = SpannableString(text)
        val accent = ThemeStore.accentColor(requireContext())
        "\\d+".toRegex().findAll(text).forEach {
            spannable.setSpan(
                ForegroundColorSpan(accent),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                RelativeSizeSpan(1.24f),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun bindReadProgress(recentBookView: RecentBookView, book: Book) {
        val progress = readProgressPercent(book)
        if (progress == null) {
            recentBookView.progressText.gone()
            recentBookView.progressBar.gone()
        } else {
            recentBookView.progressText.visible()
            recentBookView.progressBar.visible()
            recentBookView.progressText.text = getString(R.string.read_progress_percent, progress)
            recentBookView.progressBar.progress = progress
        }
    }

    private fun readProgressPercent(book: Book): Int? {
        val total = book.totalChapterNum
        if (total <= 0) return null
        val current = (book.durChapterIndex + 1).coerceIn(0, total)
        return (current * 100f / total).roundToInt().coerceIn(if (current > 0) 1 else 0, 100)
    }

    private fun tintRecentReadProgress() {
        val accent = ThemeStore.accentColor(requireContext())
        val background = ContextCompat.getColor(requireContext(), R.color.background)
        val track = ColorUtils.blendColors(background, accent, 0.12f)
        recentBookViews.forEach {
            it.progressText.setTextColor(accent)
            it.progressBar.progressTintList = ColorStateList.valueOf(accent)
            it.progressBar.progressBackgroundTintList = ColorStateList.valueOf(track)
        }
    }

    private fun tintTopBookProgress() {
        val accent = ThemeStore.accentColor(requireContext())
        val background = ContextCompat.getColor(requireContext(), R.color.background)
        val track = ColorUtils.blendColors(background, accent, 0.10f)
        val secondaryProgress = ColorUtils.blendColors(background, accent, 0.58f)
        topBookViews.forEachIndexed { index, topBookView ->
            topBookView.progress.progressTintList =
                ColorStateList.valueOf(if (index == 0) accent else secondaryProgress)
            topBookView.progress.progressBackgroundTintList = ColorStateList.valueOf(track)
        }
    }

    private fun bindPeriodSummary(state: ReadRecordPeriodUiState) {
        binding.tvPeriodRange.text = periodRangeText(state)
        val accent = ThemeStore.accentColor(requireContext())
        binding.tvPeriodTotalTime.text = accentNumberSpan(
            compactDuration(state.totalReadTime),
            accent,
            22
        )
        binding.tvPeriodReadDays.text = accentNumberSpan(
            getString(R.string.read_record_days_count, state.readDayCount),
            accent,
            22
        )
        binding.tvPeriodBookCount.text = accentNumberSpan(
            getString(R.string.read_record_books_count, state.bookCount),
            accent,
            22
        )
        binding.tvPeriodNext.isEnabled = state.canGoNext
        binding.tvPeriodNext.alpha = if (state.canGoNext) 1f else 0.32f
        bindPeriodMode(state.mode)
    }

    private fun compactDuration(readTime: Long): String {
        val minutes = readTime / 60_000L
        return when {
            minutes <= 0L -> "0分钟"
            minutes < 60L -> "${minutes}分钟"
            else -> {
                val hours = minutes / 60
                val remain = minutes % 60
                if (remain == 0L) "${hours}小时" else "${hours}小时${remain}分"
            }
        }
    }

    private fun accentNumberSpan(text: String, accent: Int, numberSizeSp: Int): CharSequence {
        val spannable = SpannableString(text)
        Regex("\\d+").findAll(text).forEach { match ->
            spannable.setSpan(
                AbsoluteSizeSpan(numberSizeSp, true),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(accent),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun bindTopBooks(books: List<ReadRecordTopBook>) {
        binding.tvTopBooksEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
        binding.llTopBooksContent.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
        val maxReadTime = books.maxOfOrNull { it.readTime } ?: 0L
        topBookViews.forEachIndexed { index, topBookView ->
            val book = books.getOrNull(index)
            if (book == null) {
                topBookView.container.gone()
            } else {
                topBookView.container.visible()
                bindTopBook(topBookView, book, index, maxReadTime)
            }
        }
    }

    private fun bindWeekBars(state: ReadRecordWeekBarUiState) {
        binding.tvWeekBarRange.text = getString(
            R.string.read_record_period_range_week,
            periodDateText(state.startDate),
            periodDateText(state.endDate)
        )
        binding.tvWeekBarNext.isEnabled = state.canGoNext
        binding.tvWeekBarNext.alpha = if (state.canGoNext) 1f else 0.32f
        binding.readRecordWeekBarChart.setData(state)
    }

    private fun bindTopBook(
        topBookView: TopBookView,
        book: ReadRecordTopBook,
        index: Int,
        maxReadTime: Long
    ) {
        val accent = ThemeStore.accentColor(requireContext())
        val normalText = ContextCompat.getColor(requireContext(), R.color.primaryText)
        val summaryText = ContextCompat.getColor(requireContext(), R.color.tv_text_summary)
        val isFirst = index == 0
        topBookView.rank.text = (index + 1).toString()
        topBookView.name.text = book.displayName
        topBookView.time.text = ReadRecordHeatmapHelper.formatDuration(book.readTime)
        topBookView.cover.load(
            path = book.displayCover,
            name = book.displayName,
            author = book.author,
            loadOnlyWifi = false
        )
        if (book.author.isNullOrBlank()) {
            topBookView.author.gone()
        } else {
            topBookView.author.visible()
            topBookView.author.text = book.author
        }
        topBookView.rank.setTextColor(if (isFirst) accent else summaryText)
        topBookView.time.setTextColor(if (isFirst) accent else normalText)
        topBookView.progress.progress = if (maxReadTime <= 0L) {
            0
        } else {
            (book.readTime * 100 / maxReadTime).toInt().coerceIn(1, 100)
        }
        topBookView.container.setOnClickListener {
            startActivityForBook(Book(bookUrl = book.bookUrl, type = book.type))
        }
    }

    private fun periodRangeText(state: ReadRecordPeriodUiState): String {
        return when (state.mode) {
            ReadRecordPeriodMode.YEAR -> getString(
                R.string.read_record_period_range_year,
                state.startDate.year
            )
            ReadRecordPeriodMode.MONTH -> getString(
                R.string.read_record_period_range_month,
                state.startDate.year,
                state.startDate.monthValue
            )
            ReadRecordPeriodMode.WEEK -> getString(
                R.string.read_record_period_range_week,
                periodDateText(state.startDate),
                periodDateText(state.endDate)
            )
        }
    }

    private fun periodDateText(date: java.time.LocalDate): String {
        return getString(
            R.string.read_record_period_range_day,
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
    }

    private fun bindPeriodMode(mode: ReadRecordPeriodMode) {
        val accent = ThemeStore.accentColor(requireContext())
        val unselectedText = ContextCompat.getColor(requireContext(), R.color.seg_control_text)
        val modeViews = listOf(
            ReadRecordPeriodMode.YEAR to binding.tvPeriodYear,
            ReadRecordPeriodMode.MONTH to binding.tvPeriodMonth,
            ReadRecordPeriodMode.WEEK to binding.tvPeriodWeek
        )
        modeViews.forEach { (viewMode, view) ->
            val selected = viewMode == mode
            view.setTextColor(if (selected) Color.WHITE else unselectedText)
            view.background = GradientDrawable().apply {
                cornerRadius = 14.dpToPx().toFloat()
                setColor(if (selected) accent else Color.TRANSPARENT)
            }
        }
    }

    private data class RecentBookView(
        val container: ViewGroup,
        val cover: CoverImageView,
        val name: TextView,
        val progressText: TextView,
        val progressBar: ProgressBar
    )

    private data class TopBookView(
        val container: ViewGroup,
        val rank: TextView,
        val cover: CoverImageView,
        val name: TextView,
        val author: TextView,
        val time: TextView,
        val progress: ProgressBar
    )

}
