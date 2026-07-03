package com.java.myapplication.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CharactersUiState(
    val characters: List<Character> = emptyList(),
    val search: String = "",
    val pathFilter: Path? = null,
    val elementFilter: Element? = null
) {
    val filtered: List<Character>
        get() = characters.filter { c ->
            (search.isEmpty() ||
                c.name.contains(search, ignoreCase = true) ||
                c.id.contains(search, ignoreCase = true)) &&
                (pathFilter == null || c.path == pathFilter) &&
                (elementFilter == null || c.element == elementFilter)
        }
}

class CharactersViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val pathFilter = MutableStateFlow<Path?>(null)
    private val elementFilter = MutableStateFlow<Element?>(null)

    val uiState: StateFlow<CharactersUiState> = combine(
        repository.observeAllCharacters(),
        search,
        pathFilter,
        elementFilter
    ) { chars, q, p, e ->
        CharactersUiState(
            characters = chars,
            search = q,
            pathFilter = p,
            elementFilter = e
        )
    }.stateIn(
        viewModelScope,
        // Eagerly: 启动即开始收集（适合测试）；生产可改 WhileSubscribed(5000)
        SharingStarted.Eagerly,
        CharactersUiState()
    )

    fun setSearch(value: String) {
        search.value = value
    }

    fun setPathFilter(p: Path?) {
        pathFilter.value = p
    }

    fun setElementFilter(e: Element?) {
        elementFilter.value = e
    }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CharactersViewModel(repo) as T
        }
    }
}