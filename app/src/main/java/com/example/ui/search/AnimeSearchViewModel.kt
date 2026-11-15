package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AnimeCatalogRepository
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeReleaseSeason
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeries
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.AudioFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnimeSearchViewModel(
    private val catalogRepository: AnimeCatalogRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(AnimeSearchFilter())
    val filter: StateFlow<AnimeSearchFilter> = _filter.asStateFlow()

    private val _results = MutableStateFlow<List<AnimeSeries>>(emptyList())
    val results: StateFlow<List<AnimeSeries>> = _results.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasNextPage = MutableStateFlow(false)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    private val _fromCache = MutableStateFlow(false)
    val fromCache: StateFlow<Boolean> = _fromCache.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val searchHistory: StateFlow<List<String>> = catalogRepository.observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    init {
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun onQueryChanged(newQuery: String) {
        _filter.value = _filter.value.copy(query = newQuery, page = 1)
        suggestionJob?.cancel()
        if (newQuery.trim().length >= 2) {
            suggestionJob = viewModelScope.launch {
                delay(250)
                _suggestions.value = catalogRepository.getSuggestions(newQuery)
            }
        } else {
            _suggestions.value = emptyList()
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(420)
            executeSearch(resetPage = true, recordQuery = false)
        }
    }

    fun submitQuery(queryOverride: String? = null) {
        val targetQuery = queryOverride ?: _filter.value.query
        _filter.value = _filter.value.copy(query = targetQuery, page = 1)
        _suggestions.value = emptyList()
        searchJob?.cancel()
        executeSearch(resetPage = true, recordQuery = targetQuery.trim().length >= 2)
    }

    fun applyGenrePreset(genre: String?) {
        val normalized = genre?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        if (_filter.value.selectedGenre == normalized && _results.value.isNotEmpty()) return
        _filter.value = _filter.value.copy(selectedGenre = normalized, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun selectGenre(genre: String?) {
        val next = if (_filter.value.selectedGenre == genre) null else genre
        _filter.value = _filter.value.copy(selectedGenre = next, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun selectYear(year: Int?) {
        _filter.value = _filter.value.copy(releaseYear = year, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun selectSeason(season: AnimeReleaseSeason?) {
        _filter.value = _filter.value.copy(season = season, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun selectFormat(format: AnimeFormat?) {
        _filter.value = _filter.value.copy(format = format, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun selectAudioFilter(audio: AudioFilter) {
        _filter.value = _filter.value.copy(audioFilter = audio, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun selectSort(sort: AnimeSortOption) {
        _filter.value = _filter.value.copy(sortBy = sort, page = 1)
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun resetFilters() {
        _filter.value = AnimeSearchFilter()
        _suggestions.value = emptyList()
        executeSearch(resetPage = true, recordQuery = false)
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            catalogRepository.deleteSearchHistoryItem(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            catalogRepository.clearSearchHistory()
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasNextPage.value) return
        val nextFilter = _filter.value.copy(page = _filter.value.page + 1)
        _filter.value = nextFilter
        viewModelScope.launch {
            _isLoadingMore.value = true
            catalogRepository.searchAnime(nextFilter)
                .onSuccess { pageResult ->
                    _results.value = (_results.value + pageResult.items).distinctBy { it.id }
                    _hasNextPage.value = pageResult.hasNextPage
                    _fromCache.value = pageResult.fromCache
                }
                .onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Unable to load more results."
                }
            _isLoadingMore.value = false
        }
    }

    fun retry() {
        executeSearch(resetPage = true, recordQuery = false)
    }

    private fun executeSearch(resetPage: Boolean, recordQuery: Boolean) {
        val activeFilter = if (resetPage) _filter.value.copy(page = 1) else _filter.value
        _filter.value = activeFilter
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            if (recordQuery) {
                catalogRepository.recordSearchQuery(activeFilter.query)
            }
            catalogRepository.searchAnime(activeFilter)
                .onSuccess { pageResult ->
                    _results.value = pageResult.items
                    _hasNextPage.value = pageResult.hasNextPage
                    _fromCache.value = pageResult.fromCache
                }
                .onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Catalog search failed."
                }
            _isLoading.value = false
        }
    }

    class Factory(
        private val catalogRepository: AnimeCatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnimeSearchViewModel(catalogRepository) as T
        }
    }
}
