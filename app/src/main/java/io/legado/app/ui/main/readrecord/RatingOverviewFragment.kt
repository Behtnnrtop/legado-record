package io.legado.app.ui.main.readrecord

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.databinding.FragmentRatingOverviewBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.main.bookshelf.style1.books.BaseBooksAdapter
import io.legado.app.ui.main.bookshelf.style1.books.BooksAdapterGrid
import io.legado.app.ui.main.bookshelf.style1.books.BooksAdapterList
import io.legado.app.ui.main.bookshelf.style1.books.BooksAdapterList2
import io.legado.app.utils.flowWithLifecycleAndDatabaseChangeFirst
import io.legado.app.utils.observeEvent
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.startActivity
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class RatingOverviewFragment : BaseFragment(R.layout.fragment_rating_overview),
    BaseBooksAdapter.CallBack {

    private val binding by viewBinding(FragmentRatingOverviewBinding::bind)
    private val activityViewModel by activityViewModels<MainViewModel>()
    private val ratings = List(11) { index -> 5f - index * 0.5f }
    private val bookshelfLayout by lazy { AppConfig.bookshelfLayout }
    private val bookshelfMargin by lazy { AppConfig.bookshelfMargin }
    private val booksAdapter: BaseBooksAdapter<*> by lazy {
        when (bookshelfLayout) {
            0 -> BooksAdapterList(requireContext(), this, this, viewLifecycleOwner.lifecycle)
            1 -> BooksAdapterList2(requireContext(), this, this, viewLifecycleOwner.lifecycle)
            else -> BooksAdapterGrid(requireContext(), this)
        }
    }
    private var selectedRating = 5f
    private var booksFlowJob: Job? = null
    private var upLastUpdateTimeJob: Job? = null
    private var itemCount = 0
    private var totalRows = 0
    private var shouldScrollToTop = false

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setBackgroundColor(requireContext().bottomBackground)
        initRatingButtons()
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRatingButtons() {
        ratings.forEach { rating ->
            val button = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_fillet_text, binding.llRatingFilters, false) as TextView
            button.text = ratingLabel(rating)
            button.background = requireContext().getDrawable(R.drawable.selector_rating_filter_btn_bg)
            button.isSelected = rating == selectedRating
            button.setOnClickListener {
                if (selectedRating != rating) {
                    selectedRating = rating
                    shouldScrollToTop = true
                    upRatingButtonState()
                    upRecyclerData()
                }
            }
            binding.llRatingFilters.addView(button)
        }
    }

    private fun upRatingButtonState() {
        for (i in 0 until binding.llRatingFilters.childCount) {
            val child = binding.llRatingFilters.getChildAt(i)
            child.isSelected = ratings.getOrNull(i) == selectedRating
            if (child.isSelected) {
                binding.hsvRatingFilters.post {
                    binding.hsvRatingFilters.smoothScrollTo(child.left - 8.dp, 0)
                }
            }
        }
    }

    private fun initRecyclerView() {
        binding.rvBookshelf.setEdgeEffectColor(primaryColor)
        upFastScrollerBar()
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.isEnabled = false
        if (bookshelfLayout >= 2) {
            binding.rvBookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout)
            binding.rvBookshelf.setRecycledViewPool(activityViewModel.booksGridRecycledViewPool)
        } else {
            binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
            binding.rvBookshelf.setRecycledViewPool(activityViewModel.booksListRecycledViewPool)
        }
        booksAdapter.stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rvBookshelf.adapter = booksAdapter
        binding.rvBookshelf.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (bookshelfLayout >= 2) {
                    val rowIndex = position / bookshelfLayout
                    when (rowIndex) {
                        0 -> outRect.set(
                            bookshelfMargin,
                            bookshelfMargin + 24,
                            bookshelfMargin,
                            bookshelfMargin
                        )

                        totalRows - 1 -> outRect.set(
                            bookshelfMargin,
                            bookshelfMargin,
                            bookshelfMargin,
                            bookshelfMargin + 24
                        )

                        else -> outRect.set(
                            bookshelfMargin,
                            bookshelfMargin,
                            bookshelfMargin,
                            bookshelfMargin
                        )
                    }
                } else {
                    when (position) {
                        0 -> outRect.set(0, bookshelfMargin + 24, 0, bookshelfMargin)
                        itemCount - 1 -> outRect.set(0, bookshelfMargin, 0, bookshelfMargin + 24)
                        else -> outRect.set(0, bookshelfMargin, 0, bookshelfMargin)
                    }
                }
            }
        })
        startLastUpdateTimeJob()
    }

    private fun upFastScrollerBar() {
        val showBookshelfFastScroller = AppConfig.showBookshelfFastScroller
        binding.rvBookshelf.setFastScrollEnabled(showBookshelfFastScroller)
        if (showBookshelfFastScroller) {
            binding.rvBookshelf.scrollBarSize = 0
        } else {
            binding.rvBookshelf.scrollBarSize =
                ViewConfiguration.get(requireContext()).scaledScrollBarSize
        }
    }

    private fun upRecyclerData() {
        booksFlowJob?.cancel()
        booksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowShelfByRating(selectedRating)
                .flowWithLifecycleAndDatabaseChangeFirst(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED,
                    AppDatabase.BOOK_TABLE_NAME
                ).catch {
                    AppLog.put("评分总览更新出错", it)
                }.conflate().flowOn(Dispatchers.Default).collect { list ->
                    itemCount = list.size
                    if (bookshelfLayout >= 2) {
                        totalRows = if (itemCount % bookshelfLayout == 0) {
                            itemCount / bookshelfLayout
                        } else {
                            itemCount / bookshelfLayout + 1
                        }
                    }
                    binding.tvEmptyMsg.isGone = itemCount > 0
                    booksAdapter.setItems(list)
                    if (shouldScrollToTop) {
                        binding.rvBookshelf.scrollToPosition(0)
                        shouldScrollToTop = false
                    }
                    delay(100)
                }
        }
    }

    private fun startLastUpdateTimeJob() {
        upLastUpdateTimeJob?.cancel()
        if (!AppConfig.showLastUpdateTime || bookshelfLayout >= 2) {
            return
        }
        upLastUpdateTimeJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                booksAdapter.upLastUpdateTime()
                delay(30 * 1000)
            }
        }
    }

    private fun ratingLabel(rating: Float): String {
        return "★${String.format(Locale.US, "%.1f", rating)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        booksFlowJob?.cancel()
        upLastUpdateTimeJob?.cancel()
        binding.rvBookshelf.setItemViewCacheSize(0)
        binding.rvBookshelf.adapter = null
    }

    override fun open(book: Book) {
        startActivityForBook(book)
    }

    override fun openBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return activityViewModel.isUpdate(bookUrl)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
            startLastUpdateTimeJob()
            upFastScrollerBar()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
