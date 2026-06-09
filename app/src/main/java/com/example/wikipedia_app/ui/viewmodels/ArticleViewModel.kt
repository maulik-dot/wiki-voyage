package com.example.wikipedia_app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikipedia_app.data.ArticleParser
import com.example.wikipedia_app.data.Bookmark
import com.example.wikipedia_app.data.BookmarkRepository
import com.example.wikipedia_app.model.ParsedArticle
import com.example.wikipedia_app.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticleViewModel(private val bookmarkRepository: BookmarkRepository) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val article: ParsedArticle) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    fun load(title: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _isBookmarked.value = bookmarkRepository.isBookmarked(title)
            try {
                // Fetch full article HTML and thumbnail concurrently
                val articleDef = async(Dispatchers.IO) {
                    RetrofitInstance.api.getFullArticle(title)
                }
                val thumbDef = async(Dispatchers.IO) {
                    try {
                        RetrofitInstance.api.getArticleSummary(title).execute().body()?.thumbnail?.source
                    } catch (_: Exception) { null }
                }

                val response = articleDef.await()
                val thumbnail = thumbDef.await()

                if (response.error != null) throw Exception(response.error.info)
                val parse = response.parse ?: throw Exception("Article not found")

                val article = ArticleParser.parse(
                    title = parse.displaytitle ?: parse.title,
                    html = parse.text,
                    thumbnailUrl = thumbnail
                )
                _uiState.value = UiState.Success(article)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load article")
            }
        }
    }

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            val bookmark = Bookmark(title = title, url = url)
            if (_isBookmarked.value) bookmarkRepository.removeBookmark(bookmark)
            else bookmarkRepository.addBookmark(bookmark)
            _isBookmarked.value = !_isBookmarked.value
        }
    }
}
