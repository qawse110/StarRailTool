package com.mystarrail.tool.ui.scraper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.repository.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class ScraperUiState(
    val seedCount: Int = 0,
    val characterCount: Int = 0,
    val lightConeCount: Int = 0,
    val relicCount: Int = 0,
    val scenarioCount: Int = 0,
    val isLoadingStatus: Boolean = true,
    val url: String = "",
    val isFetching: Boolean = false,
    val lastResult: String? = null,
    val lastError: String? = null
)

class ScraperViewModel(
    private val repository: CharacterRepository,
    /** 重新导入 seed 的回调 — 通常是 `seedImporter::importFromAssets`。 */
    private val reimportCallback: suspend () -> Unit
) : ViewModel() {

    private val _state = MutableStateFlow(ScraperUiState())
    val state: StateFlow<ScraperUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refreshStatus() }
    }

    suspend fun refreshStatus() {
        _state.value = _state.value.copy(isLoadingStatus = true)
        val chars = repository.observeAllCharacters().first()
        val cones = repository.observeAllLightCones().first()
        val relics = repository.observeAllRelicSets().first()
        val scenarios = repository.observeAllScenarios().first()
        _state.value = _state.value.copy(
            seedCount = if (chars.isNotEmpty()) 1 else 0, // 占位：seed-imported 标志
            characterCount = chars.size,
            lightConeCount = cones.size,
            relicCount = relics.size,
            scenarioCount = scenarios.size,
            isLoadingStatus = false
        )
    }

    fun setUrl(value: String) {
        _state.value = _state.value.copy(url = value)
    }

    /**
     * 用 jsoup 抓取 [url] 的 HTML，提取 <h1> + 前 3 个 <p> 作为预览。
     * 实际 wiki 集成时把选择器替换成目标站点的角色/光锥 selector 即可。
     */
    fun fetch() {
        val target = _state.value.url.trim()
        if (target.isEmpty()) {
            _state.value = _state.value.copy(lastError = "URL 为空")
            return
        }
        _state.value = _state.value.copy(isFetching = true, lastError = null, lastResult = null)
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val doc = Jsoup.connect(target)
                        .userAgent("Mozilla/5.0 StarRailTool/1.0")
                        .timeout(15_000)
                        .get()
                    val title = doc.title()
                    val firstParas = doc.select("p").take(3)
                        .joinToString("\n\n") { it.text() }
                        .take(500)
                    "📄 $title\n\n$firstParas"
                }
                _state.value = _state.value.copy(
                    isFetching = false,
                    lastResult = text,
                    lastError = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetching = false,
                    lastResult = null,
                    lastError = "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    /**
     * 手动重新触发 seed importer 把 assets/seed-data-v1.json 导入 DB。
     * 注：当前 SeedImporter 写时是 ON CONFLICT REPLACE，所以"重复导入"是安全的。
     */
    fun reimportSeed() {
        viewModelScope.launch {
            try {
                reimportCallback()
                refreshStatus()
                _state.value = _state.value.copy(
                    lastResult = "✅ 种子数据已重新导入"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(lastError = "导入失败: ${e.message}")
            }
        }
    }

    companion object {
        fun factory(
            repository: CharacterRepository,
            reimportCallback: suspend () -> Unit
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ScraperViewModel(repository, reimportCallback) as T
        }
    }
}
