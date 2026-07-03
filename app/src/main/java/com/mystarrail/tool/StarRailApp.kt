package com.mystarrail.tool

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.mystarrail.tool.data.seed.SeedImporter
import com.mystarrail.tool.data.seed.remote.Mar7thToSeedTransformer
import com.mystarrail.tool.util.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class StarRailApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var services: ServiceLocator
        private set

    @Volatile
    var seedImportResult: SeedImporter.ImportResult? = null
        private set

    /**
     * 远程拉取结果（用于 UI 显示"已是最新"/"有更新可用"）
     */
    @Volatile
    var remoteFetchResult: RemoteFetchOutcome? = null
        private set

    sealed interface RemoteFetchOutcome {
        data class Success(val characters: Int, val lightCones: Int, val relicSets: Int, val eidolons: Int) : RemoteFetchOutcome
        data class Failed(val reason: String) : RemoteFetchOutcome
    }

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)

        appScope.launch { bootstrap() }
    }

    /**
     * 启动 bootstrap：先尝试拉远程 Mar-7th 数据，成功则导入；失败/超时则回退 assets。
     *
     * 7 秒总超时（含 10 文件并发拉取）。PlayerBuild 表永不被覆盖（它不参与本流程）。
     */
    private suspend fun bootstrap() {
        // 先看 DB 是否已有数据 — 已有则跳过 bootstrap（用户可手动"重新导入"或"更新远程"）
        val existingCount = runCatching { services.database.characterDao().count() }.getOrElse { 0 }
        if (existingCount > 0) {
            Log.i(TAG, "DB already has $existingCount characters; skipping bootstrap")
            // 即使 DB 已有，也尝试后台拉远程（但失败不报错，仅更新 remoteFetchResult 供 UI 显示）
            tryFetchRemoteInBackground()
            return
        }

        // 首次启动：拉远程 → 失败回退 assets
        seedImportResult = try {
            withTimeoutOrNull(7_000) { fetchAndImportRemote() }
        } catch (e: Exception) {
            Log.w(TAG, "Remote fetch threw: ${e.message}")
            SeedImporter.ImportResult.Failed("Remote error: ${e.message}")
        }

        if (seedImportResult is SeedImporter.ImportResult.Success) {
            Log.i(TAG, "Bootstrap from remote succeeded")
            return
        }

        Log.w(TAG, "Remote bootstrap failed, falling back to assets")
        seedImportResult = runCatching { services.seedImporter.importFromAssets() }
            .getOrElse { SeedImporter.ImportResult.Failed("Assets fallback failed: ${it.message}") }
    }

    /**
     * 主动从远程拉取（用户点击"从 Mar-7th 更新"按钮时调用）
     */
    suspend fun fetchAndImportRemote(): SeedImporter.ImportResult {
        val fetch = services.remoteSeedSource.fetch()
        val transformed = Mar7thToSeedTransformer.transform(fetch.files)
        val result = services.seedImporter.importSeed(transformed)
        remoteFetchResult = when (result) {
            is SeedImporter.ImportResult.Success -> RemoteFetchOutcome.Success(
                characters = result.characters,
                lightCones = result.lightCones,
                relicSets = result.relicSets,
                eidolons = result.eidolons
            )
            is SeedImporter.ImportResult.Failed -> RemoteFetchOutcome.Failed(result.reason)
        }
        return result
    }

    private suspend fun tryFetchRemoteInBackground() {
        runCatching {
            val fetch = services.remoteSeedSource.fetch(timeoutMs = 5_000)
            val transformed = Mar7thToSeedTransformer.transform(fetch.files)
            remoteFetchResult = RemoteFetchOutcome.Success(
                characters = transformed.characters.size,
                lightCones = transformed.lightCones.size,
                relicSets = transformed.relicSets.size,
                eidolons = transformed.eidolons.size
            )
        }.onFailure {
            Log.d(TAG, "Background remote probe failed: ${it.message}")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    companion object {
        private const val TAG = "StarRailApp"
    }
}