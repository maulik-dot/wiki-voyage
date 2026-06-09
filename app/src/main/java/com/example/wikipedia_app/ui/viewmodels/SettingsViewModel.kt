package com.example.wikipedia_app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikipedia_app.data.ArticleCacheDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val articleCacheDao: ArticleCacheDao) : ViewModel() {

    private val _cacheSize = MutableStateFlow(0)
    val cacheSize: StateFlow<Int> = _cacheSize.asStateFlow()

    init {
        loadCacheSize()
    }

    fun loadCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = articleCacheDao.getCacheSize()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            articleCacheDao.clearAll()
            _cacheSize.value = 0
        }
    }
}
