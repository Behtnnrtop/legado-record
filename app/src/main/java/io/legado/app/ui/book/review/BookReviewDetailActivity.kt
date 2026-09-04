package io.legado.app.ui.book.review

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ActivityBookReviewDetailBinding
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookReviewDetailActivity :
    VMBaseActivity<ActivityBookReviewDetailBinding, BookReviewDetailViewModel>() {

    override val binding by viewBinding(ActivityBookReviewDetailBinding::inflate)
    override val viewModel by viewModels<BookReviewDetailViewModel>()
    private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    private var book: Book? = null

    private val bookReviewEditResult = registerForActivityResult(
        StartActivityContract(BookReviewEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            viewModel.refreshBook()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this) { upView(it) }
        intent.getStringExtra("bookUrl")?.let {
            viewModel.loadBook(it)
        } ?: run {
            toastOnUi("bookUrl is null")
            finish()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_review_detail, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> openEdit()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun upView(book: Book) = binding.run {
        this@BookReviewDetailActivity.book = book
        tvBookName.text = book.name
        tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
        tvReviewTime.text = formatReviewTime(book)
        bookRatingBar.rating = book.rating
        ivCover.load(book, false)
        tvBookReview.text = book.review?.takeIf { it.isNotBlank() }
            ?: getString(R.string.book_review_empty)
    }

    private fun openEdit() {
        val currentBook = book ?: return
        bookReviewEditResult.launch {
            putExtra("bookUrl", currentBook.bookUrl)
        }
    }

    private fun formatReviewTime(book: Book): String {
        if (book.reviewCreateTime <= 0L) return ""
        return getString(R.string.book_review_created, dateFormat.format(Date(book.reviewCreateTime)))
    }
}
