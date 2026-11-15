package com.example.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AnimeCatalogRepository
import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.model.AnimeSeries
import com.example.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnimeDetailsViewModel(
    private val animeId: String,
    private val catalogRepository: AnimeCatalogRepository
) : ViewModel() {

    private val _detailsState = MutableStateFlow<UiState<AnimeDetails>>(UiState.Loading)
    val detailsState: StateFlow<UiState<AnimeDetails>> = _detailsState.asStateFlow()

    private val _selectedSeasonId = MutableStateFlow<String?>(null)
    val selectedSeasonId: StateFlow<String?> = _selectedSeasonId.asStateFlow()

    private val _episodes = MutableStateFlow<List<AnimeEpisode>>(emptyList())
    val episodes: StateFlow<List<AnimeEpisode>> = _episodes.asStateFlow()

    val isFavorite: StateFlow<Boolean> = catalogRepository.observeFavorites()
        .map { list -> list.any { it.id == animeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isWatchLater: StateFlow<Boolean> = catalogRepository.observeWatchLater()
        .map { list -> list.any { it.id == animeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _detailsState.value = UiState.Loading
            catalogRepository.getAnimeDetails(animeId)
                .onSuccess { details ->
                    _detailsState.value = UiState.Success(details)
                    val firstSeason = details.seasons.firstOrNull()
                    if (firstSeason != null) {
                        _selectedSeasonId.value = firstSeason.seasonId
                        _episodes.value = details.episodesBySeason[firstSeason.seasonId].orEmpty()
                    }
                }
                .onFailure { err ->
                    _detailsState.value = UiState.Error(
                        err.localizedMessage ?: "Failed to load anime details."
                    )
                }
        }
    }

    fun selectSeason(season: AnimeSeasonGroup) {
        _selectedSeasonId.value = season.seasonId
        val details = (_detailsState.value as? UiState.Success)?.data ?: return
        val cachedEps = details.episodesBySeason[season.seasonId]
        if (cachedEps != null) {
            _episodes.value = cachedEps
        } else {
            viewModelScope.launch {
                _episodes.value = catalogRepository.getEpisodesForSeason(animeId, season)
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
        private val animeId: String,
        private val catalogRepository: AnimeCatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnimeDetailsViewModel(animeId, catalogRepository) as T
        }
    }
}
