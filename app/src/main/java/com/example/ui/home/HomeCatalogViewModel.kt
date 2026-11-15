package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaybackProgressEntity
import com.example.data.repository.AnimeCatalogRepository
import com.example.data.repository.HomeCatalogPayload
import com.example.domain.model.AnimeSeries
import com.example.domain.model.CatalogSectionType
import com.example.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeCatalogViewModel(
    private val catalogRepository: AnimeCatalogRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow<UiState<HomeCatalogPayload>>(UiState.Loading)
    val homeState: StateFlow<UiState<HomeCatalogPayload>> = _homeState.asStateFlow()

    val favoritesList: StateFlow<List<AnimeSeries>> = catalogRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchLaterList: StateFlow<List<AnimeSeries>> = catalogRepository.observeWatchLater()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatchingList: StateFlow<List<PlaybackProgressEntity>> =
        catalogRepository.observeContinueWatching()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoritesList
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val watchLaterIds: StateFlow<Set<String>> = watchLaterList
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadHome()
    }

    fun loadHome(forceLoadingState: Boolean = true) {
        viewModelScope.launch {
            if (forceLoadingState || _homeState.value !is UiState.Success) {
                _homeState.value = UiState.Loading
            }
            catalogRepository.loadHomeCatalog()
                .onSuccess { payload ->
                    _homeState.value = if (payload.sections.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(payload)
                    }
                }
                .onFailure { error ->
                    _homeState.value = UiState.Error(
                        error.localizedMessage ?: "Failed to connect to anime catalog provider."
                    )
                }
        }
    }

    fun toggleFavorite(series: AnimeSeries) {
        viewModelScope.launch {
            catalogRepository.toggleFavorite(series)
        }
    }

    fun toggleWatchLater(series: AnimeSeries) {
        viewModelScope.launch {
            catalogRepository.toggleWatchLater(series)
        }
    }

    class Factory(
        private val catalogRepository: AnimeCatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeCatalogViewModel(catalogRepository) as T
        }
    }
}

class SectionPaginationViewModel(
    private val section: CatalogSectionType,
    private val catalogRepository: AnimeCatalogRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<AnimeSeries>>(emptyList())
    val items: StateFlow<List<AnimeSeries>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasNextPage = MutableStateFlow(false)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPage = 1

    init {
        refresh()
    }

    fun refresh() {
        currentPage = 1
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            catalogRepository.loadSectionPage(section, page = 1)
                .onSuccess { result ->
                    _items.value = result.items
                    currentPage = result.currentPage
                    _hasNextPage.value = result.hasNextPage
                }
                .onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Unable to load ${section.title}."
                }
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasNextPage.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = currentPage + 1
            catalogRepository.loadSectionPage(section, page = nextPage)
                .onSuccess { result ->
                    _items.value = (_items.value + result.items).distinctBy { it.id }
                    currentPage = result.currentPage
                    _hasNextPage.value = result.hasNextPage
                }
                .onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Failed to load more anime."
                }
            _isLoadingMore.value = false
        }
    }

    class Factory(
        private val section: CatalogSectionType,
        private val catalogRepository: AnimeCatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SectionPaginationViewModel(section, catalogRepository) as T
        }
    }
}
