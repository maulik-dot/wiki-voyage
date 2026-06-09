package com.example.wikipedia_app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikipedia_app.data.TrendingRepository
import com.example.wikipedia_app.model.FeaturedArticleResponse
import com.example.wikipedia_app.model.TrendingArticleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrendingViewModel(private val repository: TrendingRepository) : ViewModel() {
    private val _featuredContent = MutableStateFlow<FeaturedArticleResponse?>(null)
    val featuredContent: StateFlow<FeaturedArticleResponse?> = _featuredContent.asStateFlow()

    private val _trendingArticles = MutableStateFlow<List<TrendingArticleItem>>(emptyList())
    val trendingArticles: StateFlow<List<TrendingArticleItem>> = _trendingArticles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTrendingContent()
    }

    private fun loadTrendingContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Run both fetches concurrently and independently so one failure doesn't block the other
            val featuredJob = launch {
                try {
                    repository.getTodayFeaturedContent().collect { _featuredContent.value = it }
                } catch (_: Exception) { /* partial failure is acceptable */ }
            }
            val trendingJob = launch {
                try {
                    repository.getTrendingArticles().collect { _trendingArticles.value = it }
                } catch (_: Exception) {}
            }

            featuredJob.join()
            trendingJob.join()
            _isLoading.value = false

            if (_featuredContent.value == null && _trendingArticles.value.isEmpty()) {
                _error.value = "Failed to load content. Please check your connection."
            }
        }
    }

    fun refresh() {
        loadTrendingContent()
    }
} 