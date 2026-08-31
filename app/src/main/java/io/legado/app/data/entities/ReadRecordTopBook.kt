package io.legado.app.data.entities

data class ReadRecordTopBook(
    val bookUrl: String,
    val bookName: String?,
    val author: String?,
    val coverUrl: String?,
    val customCoverUrl: String?,
    val type: Int,
    val readTime: Long
) {
    val displayName: String
        get() = bookName?.takeIf { it.isNotBlank() } ?: bookUrl

    val displayCover: String?
        get() = customCoverUrl?.takeIf { it.isNotBlank() } ?: coverUrl
}
