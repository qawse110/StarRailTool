package com.mystarrail.tool.data.seed.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Mar-7th/StarRailRes 数据源。
 *
 * 数据格式：每个文件是 `{ "<id>": { ... } }` 字典结构。角色/光锥/遗器/星魂分
 * 别用 6/3/1/1 个文件，且文件间通过 ID 引用（如 character.ranks[i] 指向
 * character_ranks[ranks[i]].id）。
 *
 * 拉取策略：10 个核心文件并发 fetch，全部成功后返回 [FetchResult]。
 */
class RemoteSeedSource(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient()
) {

    /** 文件名 + 在 Mar-7th 仓库的相对路径。 */
    enum class File(val filename: String) {
        CHARACTERS("characters.json"),
        PATHS("paths.json"),
        ELEMENTS("elements.json"),
        LIGHT_CONES("light_cones.json"),
        CHARACTER_PROMOTIONS("character_promotions.json"),
        CHARACTER_SKILLS("character_skills.json"),
        CHARACTER_RANKS("character_ranks.json"),
        LIGHT_CONE_PROMOTIONS("light_cone_promotions.json"),
        LIGHT_CONE_RANKS("light_cone_ranks.json"),
        RELIC_SETS("relic_sets.json")
    }

    data class FetchResult(
        val files: Map<File, JsonElement>,
        val fetchedAt: Long = System.currentTimeMillis()
    )

    suspend fun fetch(
        files: Set<File> = CORE_FILES,
        timeoutMs: Long = 8_000
    ): FetchResult = withContext(Dispatchers.IO) {
        coroutineScope {
            files.map { file ->
                async { file to fetchFile(file, timeoutMs) }
            }.awaitAll()
        }.toMap()
            .let { FetchResult(files = it) }
    }

    private fun fetchFile(file: File, timeoutMs: Long): JsonElement {
        val url = "$baseUrl/${file.filename}"
        // 直接复用注入的 [client]，避免每次 newBuilder 克隆连接池 + Dispatcher
        // （10 并发浪费 10 套线程）。timeoutMs 由 defaultClient() 的 callTimeout 统一约束。
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "StarRailTool/1.0")
            .build()
        return client.newCall(req)
            .execute()
            .use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
                val body = resp.body?.string() ?: error("Empty body from $url")
                Log.d(TAG, "Fetched $url (${body.length} bytes)")
                jsonLenient.parseToJsonElement(body)
            }
    }

    companion object {
        private const val TAG = "RemoteSeedSource"

        /**
         * https://github.com/Mar-7th/StarRailRes master 分支，**简中**索引。
         *
         * cn 版的 name/desc 是中文（如 "三月七"），但 path/element/type 等
         * 枚举字段仍是英文枚举名（Hunt/Erudition/Ice...），不影响 transformer。
         */
        const val DEFAULT_BASE_URL =
            "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/index_new/cn"

        /** 角色/光锥/遗器/星魂全套共 10 个文件。transformer 需全部。 */
        val CORE_FILES: Set<File> = setOf(
            File.CHARACTERS, File.PATHS, File.ELEMENTS,
            File.LIGHT_CONES, File.CHARACTER_PROMOTIONS, File.CHARACTER_SKILLS,
            File.CHARACTER_RANKS, File.LIGHT_CONE_PROMOTIONS, File.LIGHT_CONE_RANKS,
            File.RELIC_SETS
        )

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // 全局 callTimeout 覆盖 connect+read+write；fetch 接收的 timeoutMs
            // 由上层 withTimeout 协程约束（StarRailApp.bootstrap 7 秒）
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        /** 容忍 unknown keys（StarRailRes 会持续加字段）。 */
        private val jsonLenient = Json { ignoreUnknownKeys = true }
    }
}