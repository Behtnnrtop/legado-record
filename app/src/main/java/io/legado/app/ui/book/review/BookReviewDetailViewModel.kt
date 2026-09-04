package io.legado.app.ui.book.review

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.model.ReadBook

class BookReviewDetailViewModel(application: Application) : BaseViewModel(application) {

    private var bookUrl: String? = null
    val bookData = MutableLiveData<Book>()

    fun loadBook(bookUrl: String) {
        this.bookUrl = bookUrl
        refreshBook()
    }

    fun refreshBook() {
        val currentBookUrl = bookUrl ?: return
        execute {
            appDb.bookDao.getBook(currentBookUrl) ?: ReadBook.book?.takeIf {
                it.bookUrl == currentBookUrl
            }
        }.onSuccess {
            it?.let { book -> bookData.postValue(book) }
        }
    }
}
