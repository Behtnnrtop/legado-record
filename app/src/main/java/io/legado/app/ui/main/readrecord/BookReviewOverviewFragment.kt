package io.legado.app.ui.main.readrecord

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.databinding.FragmentBookReviewOverviewBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.review.BookReviewDetailActivity
import io.legado.app.utils.flowWithLifecycleAndDatabaseChangeFirst
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class BookReviewOverviewFragment : BaseFragment(R.layout.fragment_book_review_overview),
    BookReviewOverviewAdapter.CallBack {

    private val binding by viewBinding(FragmentBookReviewOverviewBinding::bind)
    private val reviewAdapter by lazy {
        BookReviewOverviewAdapter(requireContext(), this, viewLifecycleOwner.lifecycle, this)
    }
    private var booksFlowJob: Job? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setBackgroundColor(requireContext().bottomBackground)
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRecyclerView() = binding.run {
        rvBookReviews.setEdgeEffectColor(primaryColor)
        refreshLayout.setColorSchemeColors(accentColor)
        refreshLayout.isEnabled = false
        rvBookReviews.layoutManager = LinearLayoutManager(context)
        reviewAdapter.stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        rvBookReviews.adapter = reviewAdapter
        rvBookReviews.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                when (position) {
                    0 -> outRect.set(0, 8.dp, 0, 4.dp)
                    reviewAdapter.itemCount - 1 -> outRect.set(0, 4.dp, 0, 8.dp)
                    else -> outRect.set(0, 4.dp, 0, 4.dp)
                }
            }
        })
    }

    private fun upRecyclerData() {
        booksFlowJob?.cancel()
        booksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowShelfReviewedBooks()
                .flowWithLifecycleAndDatabaseChangeFirst(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED,
                    AppDatabase.BOOK_TABLE_NAME
                ).catch {
                    AppLog.put("书评总览更新出错", it)
                }.conflate().flowOn(Dispatchers.Default).collect { list ->
                    binding.tvEmptyMsg.isGone = list.isNotEmpty()
                    reviewAdapter.setItems(list)
                }
        }
    }

    override fun openReview(book: Book) {
        startActivity<BookReviewDetailActivity> {
            putExtra("bookUrl", book.bookUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        booksFlowJob?.cancel()
        binding.rvBookReviews.adapter = null
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
