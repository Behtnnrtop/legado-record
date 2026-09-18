package io.legado.app.ui.book.review

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.help.book.addType
import io.legado.app.model.ReadBook
import kotlin.math.roundToInt

class BookReviewEditViewModel(application: Application) : BaseViewModel(application) {

    var book: Book? = null
    val bookData = MutableLiveData<Book>()
    private var draftRating: Float = 0f
    private var draftReview: String = ""
    private var hasDraft = false

    fun loadBook(bookUrl: String) {
        execute {
            book = appDb.bookDao.getBook(bookUrl) ?: ReadBook.book?.takeIf {
                it.bookUrl == bookUrl
            }
            book?.let {
                bookData.postValue(it)
            }
        }
    }

    fun updateDraft(rating: Float, review: String) {
        draftRating = normalizeRating(rating)
        draftReview = review
        hasDraft = true
    }

    fun clearDraft() {
        draftRating = 0f
        draftReview = ""
        hasDraft = false
    }

    fun getEditingRating(book: Book): Float {
        return if (hasDraft) draftRating else book.rating
    }

    fun getEditingReview(book: Book): String {
        return if (hasDraft) draftReview else book.review.orEmpty()
    }

    fun saveReview(rating: Float, review: String, success: (() -> Unit)? = null) {
        val book = book ?: return
        val normalizedRating = normalizeRating(rating)
        val normalizedReview = review.trim().ifBlank { null }
        val now = System.currentTimeMillis()
        val ratingChanged = normalizedRating != book.rating
        val ratingUpdateTime = if (normalizedRating != book.rating) {
            now
        } else {
            book.ratingUpdateTime
        }
        val reviewChanged = normalizedReview != book.review
        if (!ratingChanged && !reviewChanged) {
            success?.invoke()
            return
        }
        val reviewCreateTime = when {
            normalizedReview == null -> 0L
            book.reviewCreateTime > 0L -> book.reviewCreateTime
            else -> now
        }
        val reviewUpdateTime = when {
            normalizedReview == null -> 0L
            reviewChanged -> now
            else -> book.reviewUpdateTime
        }
        execute {
            book.rating = normalizedRating
            book.ratingUpdateTime = ratingUpdateTime
            book.review = normalizedReview
            book.reviewCreateTime = reviewCreateTime
            book.reviewUpdateTime = reviewUpdateTime
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
            }
            if (appDb.bookDao.has(book.bookUrl)) {
                appDb.bookDao.updateReviewAndRating(
                    book.bookUrl,
                    book.rating,
                    book.ratingUpdateTime,
                    book.review,
                    book.reviewCreateTime,
                    book.reviewUpdateTime
                )
            } else {
                book.addType(BookType.notShelf)
                book.save()
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    private fun normalizeRating(value: Float): Float {
        if (value.isNaN() || value.isInfinite() || value <= 0f) return 0f
        return (value.coerceIn(0.5f, 5f) * 2).roundToInt() / 2f
    }

}
