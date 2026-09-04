package io.legado.app.ui.book.read.page.provider

import io.legado.app.R
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.api.DataSource
import io.legado.app.ui.book.read.page.api.PageFactory
import io.legado.app.ui.book.read.page.entities.TextPage
import splitties.init.appCtx

class TextPageFactory(dataSource: DataSource) : PageFactory<TextPage>(dataSource) {

    private val keepSwipeTip = appCtx.getString(R.string.keep_swipe_tip)
    private var showingBookReviewPage = false

    override fun hasPrev(): Boolean = with(dataSource) {
        return showingBookReviewPage || hasPrevChapter() || pageIndex > 0
    }

    override fun hasNext(): Boolean = with(dataSource) {
        showingBookReviewPage = showingBookReviewPage && canShowBookReviewPage()
        return !showingBookReviewPage &&
                (hasNextChapter()
                        || (currentChapter != null && currentChapter?.isLastIndex(pageIndex) != true)
                        || canShowBookReviewPage())
    }

    override fun hasNextPlus(): Boolean = with(dataSource) {
        return !showingBookReviewPage && (hasNextChapter() || pageIndex < (currentChapter?.pageSize ?: 1) - 2)
    }

    override fun moveToFirst() {
        showingBookReviewPage = false
        ReadBook.setPageIndex(0)
    }

    override fun moveToLast() = with(dataSource) {
        showingBookReviewPage = false
        currentChapter?.let {
            if (it.pageSize == 0) {
                ReadBook.setPageIndex(0)
            } else {
                ReadBook.setPageIndex(it.pageSize.minus(1))
            }
        } ?: ReadBook.setPageIndex(0)
    }

    override fun moveToNext(upContent: Boolean): Boolean = with(dataSource) {
        return if (hasNext()) {
            val pageIndex = pageIndex
            if (currentChapter == null || currentChapter?.isLastIndex(pageIndex) == true) {
                if (canShowBookReviewPage()) {
                    showingBookReviewPage = true
                    if (upContent) upContent(resetPageOffset = false)
                    return@with true
                }
                if ((currentChapter == null || isScroll) && nextChapter == null) {
                    return@with false
                }
                showingBookReviewPage = false
                ReadBook.moveToNextChapter(upContent, false)
            } else {
                if (pageIndex < 0 || currentChapter?.isLastIndexCurrent(pageIndex) == true) {
                    return@with false
                }
                showingBookReviewPage = false
                ReadBook.setPageIndex(pageIndex.plus(1))
            }
            if (upContent) upContent(resetPageOffset = false)
            true
        } else
            false
    }

    override fun moveToPrev(upContent: Boolean): Boolean = with(dataSource) {
        return if (hasPrev()) {
            if (showingBookReviewPage) {
                showingBookReviewPage = false
            } else if (pageIndex <= 0) {
                if (currentChapter == null && prevChapter == null) {
                    return@with false
                }
                if (prevChapter != null && prevChapter?.isCompleted == false) {
                    return@with false
                }
                ReadBook.moveToPrevChapter(upContent, upContentInPlace = false)
            } else {
                if (currentChapter == null) {
                    return@with false
                }
                ReadBook.setPageIndex(pageIndex.minus(1))
            }
            if (upContent) upContent(resetPageOffset = false)
            true
        } else
            false
    }

    override val curPage: TextPage
        get() = with(dataSource) {
            showingBookReviewPage = showingBookReviewPage && canShowBookReviewPage()
            if (showingBookReviewPage) {
                return@with bookReviewPage()
            }
            ReadBook.msg?.let {
                return@with TextPage(text = it).format()
            }
            currentChapter?.let {
                return@with it.getPage(pageIndex)
                    ?: TextPage(title = it.title).apply { textChapter = it }.format()
            }
            return TextPage().format()
        }

    override val nextPage: TextPage
        get() = with(dataSource) {
            if (!showingBookReviewPage && canShowBookReviewPage()) {
                return@with bookReviewPage()
            }
            ReadBook.msg?.let {
                return@with TextPage(text = it).format()
            }
            currentChapter?.let {
                val pageIndex = pageIndex
                if (pageIndex < it.pageSize - 1) {
                    return@with it.getPage(pageIndex + 1)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
                if (!it.isCompleted) {
                    return@with TextPage(title = it.title).format()
                }
            }
            nextChapter?.let {
                return@with it.getPage(0)?.removePageAloudSpan()
                    ?: TextPage(title = it.title).format()
            }
            return TextPage().format()
        }

    override val prevPage: TextPage
        get() = with(dataSource) {
            if (showingBookReviewPage) {
                currentChapter?.let {
                    return@with it.lastPage?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
            }
            ReadBook.msg?.let {
                return@with TextPage(text = it).format()
            }
            currentChapter?.let {
                val pageIndex = pageIndex
                if (pageIndex > 0) {
                    return@with it.getPage(pageIndex - 1)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
                if (!it.isCompleted) {
                    return@with TextPage(title = it.title).format()
                }
            }
            prevChapter?.let {
                return@with it.lastPage?.removePageAloudSpan()
                    ?: TextPage(title = it.title).format()
            }
            return TextPage().format()
        }

    override val nextPlusPage: TextPage
        get() = with(dataSource) {
            if (showingBookReviewPage || canShowBookReviewPage()) {
                return@with TextPage().format()
            }
            currentChapter?.let {
                val pageIndex = pageIndex
                if (pageIndex < it.pageSize - 2) {
                    return@with it.getPage(pageIndex + 2)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
                if (!it.isCompleted) {
                    return@with TextPage(title = it.title).format()
                }
                nextChapter?.let { nc ->
                    if (pageIndex < it.pageSize - 1) {
                        return@with nc.getPage(0)?.removePageAloudSpan()
                            ?: TextPage(title = nc.title).format()
                    }
                    return@with nc.getPage(1)?.removePageAloudSpan()
                        ?: TextPage(text = keepSwipeTip).format()
                }
            }
            return TextPage().format()
        }

    private fun canShowBookReviewPage() = with(dataSource) {
        currentChapter?.isCompleted == true &&
                currentChapter?.isLastIndex(pageIndex) == true &&
                !hasNextChapter() &&
                ReadBook.msg == null
    }

    private fun bookReviewPage() = TextPage(
        text = appCtx.getString(R.string.add_book_review),
        title = ReadBook.book?.name ?: appCtx.getString(R.string.book_review),
        chapterSize = ReadBook.simulatedChapterSize,
        chapterIndex = (ReadBook.simulatedChapterSize - 1).coerceAtLeast(0)
    ).apply {
        index = 0
        isBookReviewPage = true
        isCompleted = true
        height = ChapterProvider.visibleHeight.toFloat()
        renderHeight = height.toInt()
    }
}
