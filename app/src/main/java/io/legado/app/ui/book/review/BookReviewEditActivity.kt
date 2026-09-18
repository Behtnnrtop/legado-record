package io.legado.app.ui.book.review

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ActivityBookReviewEditBinding
import io.legado.app.utils.setOnApplyWindowInsetsListenerCompat
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.bottomPadding

class BookReviewEditActivity :
    VMBaseActivity<ActivityBookReviewEditBinding, BookReviewEditViewModel>() {

    override val binding by viewBinding(ActivityBookReviewEditBinding::inflate)
    override val viewModel by viewModels<BookReviewEditViewModel>()

    private var bindingBookData = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.root.setOnApplyWindowInsetsListenerCompat { view, windowInsets ->
            val typeMask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val insets = windowInsets.getInsets(typeMask)
            view.bottomPadding = insets.bottom
            windowInsets
        }
        viewModel.bookData.observe(this) { upView(it) }
        binding.bookRatingBar.onRatingChanged = {
            updateReviewDraft()
        }
        binding.tieBookReview.addTextChangedListener {
            updateReviewDraft()
        }
        intent.getStringExtra("bookUrl")?.let {
            viewModel.loadBook(it)
        } ?: run {
            toastOnUi("bookUrl is null")
            finish()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveData()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun upView(book: Book) = binding.run {
        bindingBookData = true
        try {
            tvBookName.text = book.name
            tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
            bookRatingBar.rating = viewModel.getEditingRating(book)
            tieBookReview.setText(viewModel.getEditingReview(book))
        } finally {
            bindingBookData = false
        }
    }

    private fun saveData() = binding.run {
        viewModel.saveReview(
            bookRatingBar.rating,
            tieBookReview.text?.toString().orEmpty()
        ) {
            viewModel.clearDraft()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun updateReviewDraft() = binding.run {
        if (!bindingBookData) {
            viewModel.updateDraft(
                bookRatingBar.rating,
                tieBookReview.text?.toString().orEmpty()
            )
        }
    }

}
