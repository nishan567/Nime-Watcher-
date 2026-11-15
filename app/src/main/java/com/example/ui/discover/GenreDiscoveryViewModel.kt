package com.example.ui.discover

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AnimeCatalogRepository
import com.example.data.repository.GenreDiscoveryRepository
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeSeries
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.DiscoveryCategory
import com.example.domain.model.GenreDiscoveryCatalog
import com.example.domain.model.GenreDiscoveryPreference
import com.example.domain.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryShelfContent(
    val category: DiscoveryCategory,
    val items: List<AnimeSeries> = emptyList(),
    val isLoading: Boolean = true,
    val fromCache: Boolean = false,
    val errorMessage: String? = null
)

data class FocusedCategoryState(
    val category: DiscoveryCategory? = null,
    val items: List<AnimeSeries> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val fromCache: Boolean = false,
    val errorMessage: String? = null
)

class GenreDiscoveryViewModel(
    private val currentUserId: String,
    private val initialTag: String?,
    private val discoveryRepository: GenreDiscoveryRepository,
    private val catalogRepository: AnimeCatalogRepository
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _firestoreError = MutableStateFlow<String?>(null)
    val firestoreError: StateFlow<String?> = _firestoreError.asStateFlow()

    val preferences: StateFlow<GenreDiscoveryPreference> =
        discoveryRepository.observePreferences(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing genre preferences", root)
                _firestoreError.value = root.message ?: "Unable to sync genre preferences."
            }
            .let { flow ->
                MutableStateFlow(GenreDiscoveryPreference(userId = currentUserId)).also { state ->
                    viewModelScope.launch {
                        flow.collect { loaded ->
                            if (loaded != null) {
                                state.value = loaded
                            }
                        }
                    }
                }
            }

    val categories: StateFlow<List<DiscoveryCategory>> =
        discoveryRepository.observeCategories(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing discovery categories", root)
                _firestoreError.value = root.message ?: "Unable to sync discovery categories."
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _shelfMap = MutableStateFlow<Map<String, CategoryShelfContent>>(emptyMap())
    val shelfMap: StateFlow<Map<String, CategoryShelfContent>> = _shelfMap.asStateFlow()

    private val _focusedState = MutableStateFlow(FocusedCategoryState())
    val focusedState: StateFlow<FocusedCategoryState> = _focusedState.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val fetchedSignatureByCategory = mutableMapOf<String, String>()
    private var appliedInitialTag = false

    init {
        initializeDefaults()
        observeShelvesAndSelection()
    }

    private fun initializeDefaults() {
        viewModelScope.launch {
            _isInitializing.value = true
            val res = discoveryRepository.ensureDefaultsInitialized(currentUserId)
            if (res.isFailure) {
                _firestoreError.value = res.exceptionOrNull()?.localizedMessage
                    ?: "Failed to initialize Firestore discovery categories."
            }
            _isInitializing.value = false
        }
    }

    private fun observeShelvesAndSelection() {
        viewModelScope.launch {
            combine(categories, preferences, _selectedCategoryId) { cats, prefs, selectedId ->
                Triple(cats, prefs, selectedId)
            }.collectLatest { (cats, prefs, selectedId) ->
                if (!appliedInitialTag && !initialTag.isNullOrBlank() && cats.isNotEmpty()) {
                    appliedInitialTag = true
                    val matched = cats.firstOrNull {
                        it.queryValue.equals(initialTag, ignoreCase = true) ||
                            it.name.equals(initialTag, ignoreCase = true)
                    }
                    if (matched != null) {
                        _selectedCategoryId.value = matched.id
                        return@collectLatest
                    } else {
                        val transientCat = DiscoveryCategory(
                            id = "temp_${initialTag.lowercase()}",
                            userId = currentUserId,
                            name = initialTag,
                            queryValue = initialTag,
                            filterKind = if (initialTag.equals("Isekai", true) || initialTag.equals("Cyberpunk", true)) {
                                DiscoveryCategory.KIND_TAG
                            } else {
                                DiscoveryCategory.KIND_GENRE
                            }
                        )
                        loadFocusedCategory(transientCat, prefs, page = 1)
                        return@collectLatest
                    }
                }

                val activeCategories = filterVisibleCategories(cats, prefs)
                if (selectedId != null) {
                    val target = cats.firstOrNull { it.id == selectedId }
                    if (target != null) {
                        loadFocusedCategory(target, prefs, page = 1)
                    } else {
                        _selectedCategoryId.value = null
                    }
                } else if (activeCategories.isNotEmpty()) {
                    loadOverviewShelves(activeCategories, prefs)
                }
            }
        }
    }

    private fun filterVisibleCategories(
        cats: List<DiscoveryCategory>,
        prefs: GenreDiscoveryPreference
    ): List<DiscoveryCategory> {
        val preferredSet = prefs.preferredTags.map { it.lowercase() }.toSet()
        val sortedByPreference = cats.sortedWith(
            compareByDescending<DiscoveryCategory> { it.isPinned }
                .thenByDescending { preferredSet.contains(it.queryValue.lowercase()) }
                .thenBy { it.sortOrder }
                .thenBy { it.name.lowercase() }
        )
        return if (prefs.onlyShowPreferred && preferredSet.isNotEmpty()) {
            sortedByPreference.filter { preferredSet.contains(it.queryValue.lowercase()) }
                .ifEmpty { sortedByPreference }
        } else {
            sortedByPreference
        }
    }

    private suspend fun loadOverviewShelves(
        visibleCategories: List<DiscoveryCategory>,
        prefs: GenreDiscoveryPreference
    ) {
        val sortOption = prefs.toSortOption()
        val formatOption = prefs.toFormatOrNull()
        val signature = "${sortOption.apiSort}|${formatOption?.name ?: "ALL"}"

        val categoriesToLoad = visibleCategories.take(10)
        val initialMap = _shelfMap.value.toMutableMap()
        categoriesToLoad.forEach { cat ->
            val prev = initialMap[cat.id]
            if (prev == null || fetchedSignatureByCategory[cat.id] != signature) {
                initialMap[cat.id] = CategoryShelfContent(category = cat, isLoading = true)
            } else {
                initialMap[cat.id] = prev.copy(category = cat)
            }
        }
        _shelfMap.value = initialMap

        val pendingCategories = categoriesToLoad.filter { cat ->
            fetchedSignatureByCategory[cat.id] != signature ||
                _shelfMap.value[cat.id]?.items.isNullOrEmpty()
        }
        if (pendingCategories.isEmpty()) return

        coroutineScope {
            pendingCategories.map { cat ->
                async {
                    val result = catalogRepository.fetchCategoryAnime(
                        queryValue = cat.queryValue,
                        filterKind = cat.filterKind,
                        sortBy = sortOption,
                        format = formatOption,
                        page = 1
                    )
                    val updatedContent = result.fold(
                        onSuccess = { page ->
                            fetchedSignatureByCategory[cat.id] = signature
                            CategoryShelfContent(
                                category = cat,
                                items = page.items,
                                isLoading = false,
                                fromCache = page.fromCache,
                                errorMessage = null
                            )
                        },
                        onFailure = { err ->
                            CategoryShelfContent(
                                category = cat,
                                items = emptyList(),
                                isLoading = false,
                                fromCache = false,
                                errorMessage = err.localizedMessage ?: "Unable to fetch ${cat.name}."
                            )
                        }
                    )
                    _shelfMap.value = _shelfMap.value.toMutableMap().apply {
                        put(cat.id, updatedContent)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun loadFocusedCategory(
        category: DiscoveryCategory,
        prefs: GenreDiscoveryPreference,
        page: Int
    ) {
        val isFirstPage = page == 1
        _focusedState.value = if (isFirstPage) {
            FocusedCategoryState(category = category, isLoading = true)
        } else {
            _focusedState.value.copy(isLoadingMore = true, errorMessage = null)
        }

        val result = catalogRepository.fetchCategoryAnime(
            queryValue = category.queryValue,
            filterKind = category.filterKind,
            sortBy = prefs.toSortOption(),
            format = prefs.toFormatOrNull(),
            page = page
        )
        result.fold(
            onSuccess = { payload ->
                val merged = if (isFirstPage) {
                    payload.items
                } else {
                    (_focusedState.value.items + payload.items).distinctBy { it.id }
                }
                _focusedState.value = FocusedCategoryState(
                    category = category,
                    items = merged,
                    currentPage = payload.currentPage,
                    hasNextPage = payload.hasNextPage,
                    isLoading = false,
                    isLoadingMore = false,
                    fromCache = payload.fromCache,
                    errorMessage = null
                )
            },
            onFailure = { err ->
                _focusedState.value = _focusedState.value.copy(
                    category = category,
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = err.localizedMessage ?: "Failed to load ${category.name}."
                )
            }
        )
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun loadNextFocusedPage() {
        val cur = _focusedState.value
        val cat = cur.category ?: return
        if (cur.isLoading || cur.isLoadingMore || !cur.hasNextPage) return
        viewModelScope.launch {
            loadFocusedCategory(cat, preferences.value, cur.currentPage + 1)
        }
    }

    fun refreshActive() {
        fetchedSignatureByCategory.clear()
        val cats = categories.value
        val prefs = preferences.value
        val selectedId = _selectedCategoryId.value
        viewModelScope.launch {
            if (selectedId != null) {
                val target = cats.firstOrNull { it.id == selectedId } ?: _focusedState.value.category
                if (target != null) loadFocusedCategory(target, prefs, page = 1)
            } else {
                val active = filterVisibleCategories(cats, prefs)
                if (active.isNotEmpty()) loadOverviewShelves(active, prefs)
            }
        }
    }

    fun togglePreferredTag(tag: String) {
        viewModelScope.launch {
            val res = discoveryRepository.togglePreferredTag(tag, preferences.value)
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not update tag preference."
            }
        }
    }

    fun saveFullPreferences(
        preferredTags: List<String>,
        sortBy: AnimeSortOption,
        formatFilter: AnimeFormat?,
        onlyShowPreferred: Boolean
    ) {
        viewModelScope.launch {
            val res = discoveryRepository.savePreferences(
                preferredTags = preferredTags,
                sortBy = sortBy,
                formatFilter = formatFilter,
                onlyShowPreferred = onlyShowPreferred
            )
            _statusMessage.value = if (res.isSuccess) {
                "Genre preferences synced to Firestore"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save preferences."
            }
        }
    }

    fun updateSortOption(sortBy: AnimeSortOption) {
        val current = preferences.value
        saveFullPreferences(
            preferredTags = current.preferredTags,
            sortBy = sortBy,
            formatFilter = current.toFormatOrNull(),
            onlyShowPreferred = current.onlyShowPreferred
        )
    }

    fun updateFormatFilter(format: AnimeFormat?) {
        val current = preferences.value
        saveFullPreferences(
            preferredTags = current.preferredTags,
            sortBy = current.toSortOption(),
            formatFilter = format,
            onlyShowPreferred = current.onlyShowPreferred
        )
    }

    fun toggleOnlyShowPreferred() {
        val current = preferences.value
        saveFullPreferences(
            preferredTags = current.preferredTags,
            sortBy = current.toSortOption(),
            formatFilter = current.toFormatOrNull(),
            onlyShowPreferred = !current.onlyShowPreferred
        )
    }

    fun createCategory(
        name: String,
        queryValue: String,
        filterKind: String,
        accentKey: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            val res = discoveryRepository.createCategory(
                name = name,
                queryValue = queryValue,
                filterKind = filterKind,
                accentKey = accentKey,
                isPinned = isPinned,
                sortOrder = (categories.value.size + 1).toLong()
            )
            _statusMessage.value = if (res.isSuccess) {
                "Added \"$name\" category to Firestore"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Unable to create category."
            }
        }
    }

    fun toggleCategoryPinned(category: DiscoveryCategory) {
        viewModelScope.launch {
            val res = discoveryRepository.toggleCategoryPinned(category.id, !category.isPinned)
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update pin."
            }
        }
    }

    fun deleteCategory(category: DiscoveryCategory) {
        viewModelScope.launch {
            if (_selectedCategoryId.value == category.id) {
                _selectedCategoryId.value = null
            }
            val res = discoveryRepository.deleteCategory(category.id)
            _statusMessage.value = if (res.isSuccess) {
                "Removed \"${category.name}\""
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to delete category."
            }
        }
    }

    fun resetDefaultCategories() {
        viewModelScope.launch {
            val res = discoveryRepository.resetDefaultCategories()
            _statusMessage.value = if (res.isSuccess) {
                "Default tag categories restored"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to restore categories."
            }
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(
        private val currentUserId: String,
        private val initialTag: String?,
        private val discoveryRepository: GenreDiscoveryRepository,
        private val catalogRepository: AnimeCatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GenreDiscoveryViewModel(
                currentUserId = currentUserId,
                initialTag = initialTag,
                discoveryRepository = discoveryRepository,
                catalogRepository = catalogRepository
            ) as T
        }
    }

    private companion object {
        const val TAG = "GenreDiscoveryVM"
    }
}
