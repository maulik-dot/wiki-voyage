package com.example.wikipedia_app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikipedia_app.model.SearchResult
import com.example.wikipedia_app.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suggestion = MutableStateFlow<String?>(null)
    val suggestion: StateFlow<String?> = _suggestion.asStateFlow()

    private val _totalHits = MutableStateFlow(0)
    val totalHits: StateFlow<Int> = _totalHits.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) {
            _results.value = emptyList()
            _suggestion.value = null
            _totalHits.value = 0
            _isLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            search(q)
        }
    }

    private suspend fun search(q: String) {
        _isLoading.value = true
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitInstance.api.searchArticles(q).execute()
            }
            if (response.isSuccessful) {
                val body = response.body()
                _results.value = body?.query?.search ?: emptyList()
                _suggestion.value = body?.query?.searchinfo?.suggestion
                _totalHits.value = body?.query?.searchinfo?.totalhits ?: 0
            }
        } catch (_: Exception) {
            // keep previous results on transient errors
        } finally {
            _isLoading.value = false
        }
    }
}
