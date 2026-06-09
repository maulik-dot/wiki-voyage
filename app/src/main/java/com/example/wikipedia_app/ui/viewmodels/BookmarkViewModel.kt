package com.example.wikipedia_app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikipedia_app.data.Bookmark
import com.example.wikipedia_app.data.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkViewModel(private val repository: BookmarkRepository) : ViewModel() {
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    val allBookmarks = repository.allBookmarks

    fun checkBookmarkStatus(title: String) {
        viewModelScope.launch {
            _isBookmarked.value = repository.isBookmarked(title)
        }
    }

    fun toggleBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            if (_isBookmarked.value) {
                repository.removeBookmark(bookmark)
            } else {
                repository.addBookmark(bookmark)
            }
            _isBookmarked.value = !_isBookmarked.value
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark)
        }
    }
} 