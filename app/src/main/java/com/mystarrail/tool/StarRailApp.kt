package com.mystarrail.tool

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.mystarrail.tool.data.seed.SeedImporter
import com.mystarrail.tool.util.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StarRailApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var services: ServiceLocator
        private set

    /**
     * 最近一次 seed 导入结果。null = 尚未完成首次启动导入检查。
     * UI 可读此判断"DB 为空 vs 导入失败 vs 成功"。
     */
    @Volatile
    var seedImportResult: SeedImporter.ImportResult? = null
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)

        // 首次启动 bootstrap：DB 为空时从 assets 导入 seed。
        // 改为在 appScope 内启动并 await——但 Application.onCreate 自身在主线程，
        // UI 启动会订阅 Flow。Flow 第一次 emit 的"空列表"是 Room 真实状态（数据尚未写入）。
        // 一旦 seed 写完，Room 的 invalidation tracker 会触发 Flow 重新 emit，UI 自动刷新。
        // 因此这里不需要阻塞 Application.onCreate；只需保留错误结果供 UI 读取。
        appScope.launch {
            try {
                val count = services.database.characterDao().count()
                if (count == 0) {
                    Log.i(TAG, "DB empty, importing seed from assets")
                    seedImportResult = services.seedImporter.importFromAssets()
                } else {
                    Log.i(TAG, "DB already has $count characters, skipping seed import")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Seed bootstrap failed", e)
                seedImportResult = SeedImporter.ImportResult.Failed("Bootstrap error: ${e.message}")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    companion object {
        private const val TAG = "StarRailApp"
    }
}