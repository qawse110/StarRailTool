# 崩坏星穹铁道强度量化工具 — 实施计划 01：基础与数据层

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 M1~M3：搭建可编译的工程脚手架、Room 数据层、种子数据导入。

**Architecture:** 单 Activity + Compose + Room + Retrofit + jsoup + WorkManager。包结构按职责拆分（ui/data/engine/util），不分 Gradle module。引擎层不依赖 Android API，可纯 JVM 单测。

**Tech Stack:** Kotlin 2.3.10 / AGP 9.0.0 / Jetpack Compose / Material 3 / Room 2.6 / Retrofit 2.11 / Moshi 1.15 / Coil 3 / jsoup 1.18 / WorkManager 2.9 / JUnit4 / Truth 1.4

**参考设计**：`docs/superpowers/specs/2026-07-03-starrail-strength-tool-design.md` §2 §3 §4 §7

## Global Constraints

- Kotlin `2.3.10`，AGP `9.0.0`，compileSdk `35`，minSdk `24`，targetSdk `35`
- namespace / applicationId: `com.java.myapplication`（保持模板不变）
- 所有源码文件 UTF-8（无 BOM）
- 包根：`com.java.myapplication`
- 单元测试目录：`app/src/test/java/...`（JVM），仪器测试：`app/src/androidTest/java/...`（Android）
- 任何新依赖必须先加 `gradle/libs.versions.toml`，再在 `app/build.gradle.kts` 引用
- 每次 commit 前必须 `./gradlew assembleDebug` 通过
- 暂不写任何 UI 业务代码（M8 才有 UI），本计划只产出可在 Room 中查询到的种子数据 + 编译通过的工程

---

## 文件结构总览

```
app/src/main/java/com/java/myapplication/
├── StarRailApp.kt                    # Application（M8 才有完整逻辑，先放空壳）
├── data/
│   ├── model/                        # 全部领域模型（§4 全部 data class）
│   │   ├── Path.kt
│   │   ├── Element.kt
│   │   ├── Role.kt
│   │   ├── Tag.kt
│   │   ├── StatType.kt
│   │   ├── Target.kt
│   │   ├── DmgCondition.kt
│   │   ├── SkillType.kt
│   │   ├── Stats.kt
│   │   ├── Scaling.kt
│   │   ├── CycleProfile.kt
│   │   ├── Character.kt
│   │   ├── PassiveEffect.kt
│   │   ├── LightCone.kt
│   │   ├── RelicSet.kt
│   │   ├── MainStats.kt
│   │   ├── RelicBuild.kt
│   │   ├── EidolonEffect.kt
│   │   ├── Eidolon.kt
│   │   ├── Enemy.kt
│   │   ├── Scenario.kt
│   │   ├── SubStat.kt
│   │   ├── PlayerBuild.kt
│   │   ├── PresetSource.kt
│   │   ├── BuildPreset.kt
│   │   ├── Tier.kt
│   │   ├── ScoringConfig.kt
│   │   ├── CharacterScore.kt
│   │   └── TeamScore.kt
│   ├── local/
│   │   ├── AppDatabase.kt            # Room database
│   │   ├── Converters.kt             # TypeConverters（Enum/Set/List <-> String）
│   │   ├── CharacterEntity.kt
│   │   ├── CharacterDao.kt
│   │   ├── LightConeEntity.kt
│   │   ├── LightConeDao.kt
│   │   ├── RelicSetEntity.kt
│   │   ├── RelicSetDao.kt
│   │   ├── EnemyEntity.kt
│   │   ├── EnemyDao.kt
│   │   ├── ScenarioEntity.kt
│   │   ├── ScenarioDao.kt
│   │   ├── EidolonEntity.kt
│   │   ├── EidolonDao.kt
│   │   ├── PlayerBuildEntity.kt
│   │   └── PlayerBuildDao.kt
│   ├── seed/
│   │   ├── SeedImporter.kt           # 从 assets/seed-data-v1.json 导入到 Room
│   │   └── SeedData.kt               # JSON 反序列化模型
│   └── repository/
│       └── CharacterRepository.kt    # 暴露查询 API（M4 才会用，但 M3 就要建好接口）
app/src/main/assets/
├── seed-data-v1.json                 # 种子数据：5 角色 + 5 光锥 + 3 遗器 + 10 敌人 + 3 场景 + 5×6 星魂
└── seed-changelog.md
app/src/test/java/com/java/myapplication/
├── data/seed/SeedImporterTest.kt
└── data/local/CharacterDaoTest.kt    # Robolectric 模拟
```

---

## Task 1: 添加核心依赖到 Version Catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- 后续 Task 都需要这些库

- [ ] **Step 1: 修改 libs.versions.toml，追加依赖**

完整替换文件内容：

```toml
[versions]
agp = "9.0.0"
kotlin = "2.3.10"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.6.1"
activityCompose = "1.8.0"
composeBom = "2026.01.01"
# 新增
room = "2.6.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
moshi = "1.15.1"
coil = "3.0.4"
workManager = "2.9.1"
jsoup = "1.18.1"
coroutines = "1.9.0"
serialization = "1.7.3"
navigation = "2.8.5"
truth = "1.4.4"
robolectric = "4.14"
turbine = "1.2.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
# 新增
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
moshi-kotlin = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

- [ ] **Step 2: 修改 app/build.gradle.kts 引用新依赖**

完整替换文件内容：

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.java.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.java.myapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.android.tools.build" && requested.name == "aapt2") {
            useTarget("com.android.tools.build:aapt2:${'$'}{requested.version}:linux-aarch64")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.serialization.json)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // HTML parsing
    implementation(libs.jsoup)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

- [ ] **Step 3: 构建验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -30`
Expected: `BUILD SUCCESSFUL`（首次可能慢，需下载依赖）

- [ ] **Step 4: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Room/Retrofit/WorkManager/jsoup/Coil deps"
```

---

## Task 2: 创建枚举与基础类型

**Files:**
- Create: 12 个枚举文件，1 个 Stats 数据类

- [ ] **Step 1: 逐个创建枚举文件**

`app/src/main/java/com/java/myapplication/data/model/Path.kt`:
```kotlin
package com.java.myapplication.data.model

enum class Path { WARRIOR, ROGUE, MAGE, SHAMAN, WARLOCK, HUNT, PRIEST }
```

`app/src/main/java/com/java/myapplication/data/model/Element.kt`:
```kotlin
package com.java.myapplication.data.model

enum class Element { PHYSICAL, FIRE, ICE, LIGHTNING, WIND, QUANTUM, IMAGINARY }
```

`app/src/main/java/com/java/myapplication/data/model/Role.kt`:
```kotlin
package com.java.myapplication.data.model

enum class Role { DPS, SUB_DPS, SUPPORT, HEALER, SHIELD }
```

`app/src/main/java/com/java/myapplication/data/model/Tag.kt`:
```kotlin
package com.java.myapplication.data.model

enum class Tag {
    DOT, ULT_CHARGE, ACTION_ADVANCE, SPEED_BOOST, ATK_BOOST, CRIT_BOOST,
    DEBUFF, SHIELD, HEAL, CLEANSE, BREAK_EFFECT, FOLLOW_UP,
    ULT_DMG_BONUS, ENERGY_REGEN, SINGLE_TARGET, AOE, IMPULSE, SUMMON
}
```

`app/src/main/java/com/java/myapplication/data/model/StatType.kt`:
```kotlin
package com.java.myapplication.data.model

enum class StatType { ATK, HP, DEF, SPD, CRIT_RATE, CRIT_DMG, EHR, BRK_EFF, EFFECT_RES }
```

`app/src/main/java/com/java/myapplication/data/model/Target.kt`:
```kotlin
package com.java.myapplication.data.model

enum class Target { SELF, ALLY, TEAM, ENEMY, ALLIES_WITH_PATH }
```

`app/src/main/java/com/java/myapplication/data/model/DmgCondition.kt`:
```kotlin
package com.java.myapplication.data.model

enum class DmgCondition { ALWAYS, ULT_ACTIVE, FOLLOW_UP, DOT, BREAK, ULT_AFTER_SKILL, AFTER_EAT_SP }
```

`app/src/main/java/com/java/myapplication/data/model/SkillType.kt`:
```kotlin
package com.java.myapplication.data.model

enum class SkillType { SKILL, ULT, TALENT, FOLLOW_UP, DOT, ALL }
```

`app/src/main/java/com/java/myapplication/data/model/EnemyType.kt`:
```kotlin
package com.java.myapplication.data.model

enum class EnemyType { BOSS, ELITE, MOB, DOOM, SUMMON }
```

`app/src/main/java/com/java/myapplication/data/model/PresetSource.kt`:
```kotlin
package com.java.myapplication.data.model

enum class PresetSource { COMMUNITY, OFFICIAL, USER }
```

`app/src/main/java/com/java/myapplication/data/model/Tier.kt`:
```kotlin
package com.java.myapplication.data.model

enum class Tier { S, A, B, C }
```

`app/src/main/java/com/java/myapplication/data/model/Stats.kt`:
```kotlin
package com.java.myapplication.data.model

data class Stats(val hp: Double, val atk: Double, val def: Double, val spd: Double)
```

- [ ] **Step 2: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/data/model/
git commit -m "feat(data): add enum types and Stats"
```

---

## Task 3: 创建复杂数据模型

**Files:**
- Create: 13 个数据类文件

- [ ] **Step 1: 创建 Scaling.kt**

`app/src/main/java/com/java/myapplication/data/model/Scaling.kt`:
```kotlin
package com.java.myapplication.data.model

data class Scaling(
    val skillMult: Double,
    val ultMult: Double,
    val talentMult: Double,
    val followUpMult: Double = 0.0,
    val aoeRatio: Double = 0.0
)
```

- [ ] **Step 2: 创建 CycleProfile.kt**

```kotlin
package com.java.myapplication.data.model

data class CycleProfile(
    val cycleActions: Int,
    val spdBreakpoints: List<Double>,
    val isFollowUp: Boolean = false,
    val isDot: Boolean = false
)
```

- [ ] **Step 3: 创建 Character.kt**

```kotlin
package com.java.myapplication.data.model

data class Character(
    val id: String,
    val name: String,
    val rarity: Int,
    val path: Path,
    val element: Element,
    val role: Role,
    val tags: Set<Tag>,
    val baseStats: Stats,
    val scaling: Scaling,
    val cycleProfile: CycleProfile?,
    val iconUrl: String,
    val version: Int
)
```

- [ ] **Step 4: 创建 PassiveEffect.kt（sealed interface）**

```kotlin
package com.java.myapplication.data.model

sealed interface PassiveEffect {
    data class StatBoost(
        val stat: StatType,
        val value: Double,
        val target: Target = Target.SELF
    ) : PassiveEffect

    data class DamageBonus(
        val multiplier: Double,
        val condition: DmgCondition
    ) : PassiveEffect

    data class SkillBoost(
        val type: SkillType,
        val multiplier: Double
    ) : PassiveEffect

    data class EnergyRegen(val perTurn: Double) : PassiveEffect

    data class Composite(val effects: List<PassiveEffect>) : PassiveEffect
}
```

- [ ] **Step 5: 创建 LightCone.kt**

```kotlin
package com.java.myapplication.data.model

data class LightCone(
    val id: String,
    val name: String,
    val path: Path,
    val rarity: Int,
    val passiveName: String,
    val passiveEffect: PassiveEffect,
    val s5Multiplier: Double = 1.0
)
```

- [ ] **Step 6: 创建 MainStats.kt 和 RelicSet.kt**

`app/src/main/java/com/java/myapplication/data/model/MainStats.kt`:
```kotlin
package com.java.myapplication.data.model

data class MainStats(
    val body: StatType,
    val boots: StatType,
    val sphere: StatType,
    val rope: StatType
)
```

`app/src/main/java/com/java/myapplication/data/model/RelicSet.kt`:
```kotlin
package com.java.myapplication.data.model

data class RelicSet(
    val id: String,
    val name: String,
    val twoPiece: PassiveEffect,
    val fourPiece: PassiveEffect,
    val suitableFor: Set<Role>
)
```

- [ ] **Step 7: 创建 RelicBuild.kt**

```kotlin
package com.java.myapplication.data.model

data class RelicBuild(
    val set4: String,
    val set2: String? = null,
    val mainStats: MainStats,
    val targetSubs: Set<StatType>,
    val notes: String = ""
)
```

- [ ] **Step 8: 创建 EidolonEffect.kt 和 Eidolon.kt**

`app/src/main/java/com/java/myapplication/data/model/EidolonEffect.kt`:
```kotlin
package com.java.myapplication.data.model

sealed interface EidolonEffect {
    data class StatBoost(
        val stat: StatType,
        val value: Double,
        val target: Target = Target.SELF
    ) : EidolonEffect

    data class NewMechanic(
        val mechanic: Tag,
        val param: Double = 1.0,
        val note: String = ""
    ) : EidolonEffect

    data class DamageBonus(
        val multiplier: Double,
        val condition: DmgCondition
    ) : EidolonEffect

    data class EnemyDebuff(
        val stat: StatType,
        val value: Double
    ) : EidolonEffect

    data class Composite(val effects: List<EidolonEffect>) : EidolonEffect
}
```

`app/src/main/java/com/java/myapplication/data/model/Eidolon.kt`:
```kotlin
package com.java.myapplication.data.model

data class Eidolon(
    val id: String,
    val characterId: String,
    val level: Int,
    val name: String,
    val effect: EidolonEffect,
    val major: Boolean = false
)
```

- [ ] **Step 9: 创建 Enemy.kt 和 Scenario.kt**

`app/src/main/java/com/java/myapplication/data/model/Enemy.kt`:
```kotlin
package com.java.myapplication.data.model

data class Enemy(
    val id: String,
    val name: String,
    val count: Int,
    val weaknesses: Set<Element>,
    val type: EnemyType,
    val hp: Double,
    val toughness: Double = 0.0,
    val mechanics: Set<String> = emptySet()
)
```

`app/src/main/java/com/java/myapplication/data/model/Scenario.kt`:
```kotlin
package com.java.myapplication.data.model

data class Scenario(
    val id: String,
    val name: String,
    val enemies: List<Enemy>,
    val difficulty: Int,
    val notes: String = ""
)
```

- [ ] **Step 10: 创建 SubStat/PlayerBuild/BuildPreset**

`app/src/main/java/com/java/myapplication/data/model/SubStat.kt`:
```kotlin
package com.java.myapplication.data.model

data class SubStat(val type: StatType, val value: Double, val rolls: Int)
```

`app/src/main/java/com/java/myapplication/data/model/PlayerBuild.kt`:
```kotlin
package com.java.myapplication.data.model

data class PlayerBuild(
    val id: Long = 0,
    val characterId: String,
    val level: Int = 80,
    val ascension: Int = 6,
    val lightConeId: String,
    val lightConeLevel: Int = 80,
    val lightConeSuperimposition: Int = 1,
    val relicSet4: String,
    val relicSet2: String? = null,
    val mainStats: MainStats,
    val subStats: List<SubStat>,
    val eidolons: Set<Int> = emptySet(),
    val notes: String = ""
)
```

`app/src/main/java/com/java/myapplication/data/model/BuildPreset.kt`:
```kotlin
package com.java.myapplication.data.model

data class BuildPreset(
    val id: String,
    val name: String,
    val characterId: String,
    val source: PresetSource,
    val build: PlayerBuild
)
```

- [ ] **Step 11: 创建评分结果模型**

`app/src/main/java/com/java/myapplication/data/model/ScoringConfig.kt`:
```kotlin
package com.java.myapplication.data.model

data class ScoringConfig(
    val playerBuild: PlayerBuild,
    val enemy: Enemy? = null
)
```

`app/src/main/java/com/java/myapplication/data/model/CharacterScore.kt`:
```kotlin
package com.java.myapplication.data.model

data class CharacterScore(
    val characterId: String,
    val unitValueScore: Double,
    val cycleScore: Double,
    val teamSynergyScore: Double,
    val scenarioScore: Double,
    val mechanicCoverage: Double,
    val total: Double,
    val tier: Tier
) {
    init {
        require(total in 0.0..100.0) { "total must be 0..100, was $total" }
    }
}
```

`app/src/main/java/com/java/myapplication/data/model/TeamScore.kt`:
```kotlin
package com.java.myapplication.data.model

data class TeamScore(
    val totalDamage: Double,
    val totalHealing: Double,
    val totalShielding: Double,
    val roundsToKill: Int?,
    val ultsCast: Map<String, Int>,
    val buffUptime: Map<String, Double>,
    val breakdown: Map<String, Double>,
    val score: Double
)
```

- [ ] **Step 12: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 13: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/data/model/
git commit -m "feat(data): add complex domain models"
```

---

## Task 4: Room 实体与 DAO

**Files:**
- Create: 14 个 Room 文件（7 entity + 7 dao + 1 converters + 1 database）

- [ ] **Step 1: 创建 Converters.kt**

`app/src/main/java/com/java/myapplication/data/local/Converters.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.TypeConverter
import com.java.myapplication.data.model.*

class Converters {
    @TypeConverter
    fun fromElement(value: Element): String = value.name

    @TypeConverter
    fun toElement(value: String): Element = Element.valueOf(value)

    @TypeConverter
    fun fromPath(value: Path): String = value.name

    @TypeConverter
    fun toPath(value: String): Path = Path.valueOf(value)

    @TypeConverter
    fun fromRole(value: Role): String = value.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)

    @TypeConverter
    fun fromTagSet(value: Set<Tag>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toTagSet(value: String): Set<Tag> =
        if (value.isEmpty()) emptySet()
        else value.split(",").map { Tag.valueOf(it) }.toSet()

    @TypeConverter
    fun fromStatTypeSet(value: Set<StatType>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toStatTypeSet(value: String): Set<StatType> =
        if (value.isEmpty()) emptySet()
        else value.split(",").map { StatType.valueOf(it) }.toSet()

    @TypeConverter
    fun fromStringSet(value: Set<String>): String =
        value.joinToString("\u0001") { it }

    @TypeConverter
    fun toStringSet(value: String): Set<String> =
        if (value.isEmpty()) emptySet()
        else value.split("\u0001").toSet()
}
```

- [ ] **Step 2: 创建 CharacterEntity + CharacterDao**

`app/src/main/java/com/java/myapplication/data/local/CharacterEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rarity: Int,
    val path: Path,
    val element: Element,
    val role: Role,
    val tags: Set<Tag>,
    val hp: Double,
    val atk: Double,
    val def: Double,
    val spd: Double,
    val skillMult: Double,
    val ultMult: Double,
    val talentMult: Double,
    val followUpMult: Double,
    val aoeRatio: Double,
    val cycleActions: Int?,
    val cycleSpdBreakpoints: String?,
    val cycleIsFollowUp: Boolean?,
    val cycleIsDot: Boolean?,
    val iconUrl: String,
    val version: Int
) {
    fun toModel(): Character = Character(
        id = id, name = name, rarity = rarity, path = path, element = element,
        role = role, tags = tags,
        baseStats = Stats(hp, atk, def, spd),
        scaling = Scaling(skillMult, ultMult, talentMult, followUpMult, aoeRatio),
        cycleProfile = cycleActions?.let {
            CycleProfile(
                cycleActions = it,
                spdBreakpoints = cycleSpdBreakpoints?.split(",")?.map(String::toDouble) ?: emptyList(),
                isFollowUp = cycleIsFollowUp ?: false,
                isDot = cycleIsDot ?: false
            )
        },
        iconUrl = iconUrl, version = version
    )

    companion object {
        fun fromModel(c: Character): CharacterEntity = CharacterEntity(
            id = c.id, name = c.name, rarity = c.rarity, path = c.path,
            element = c.element, role = c.role, tags = c.tags,
            hp = c.baseStats.hp, atk = c.baseStats.atk, def = c.baseStats.def, spd = c.baseStats.spd,
            skillMult = c.scaling.skillMult, ultMult = c.scaling.ultMult,
            talentMult = c.scaling.talentMult, followUpMult = c.scaling.followUpMult,
            aoeRatio = c.scaling.aoeRatio,
            cycleActions = c.cycleProfile?.cycleActions,
            cycleSpdBreakpoints = c.cycleProfile?.spdBreakpoints?.joinToString(","),
            cycleIsFollowUp = c.cycleProfile?.isFollowUp,
            cycleIsDot = c.cycleProfile?.isDot,
            iconUrl = c.iconUrl, version = c.version
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/CharacterDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: String): CharacterEntity?

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<CharacterEntity>)

    @Query("DELETE FROM characters")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(characters: List<CharacterEntity>) {
        deleteAll()
        insertAll(characters)
    }
}
```

- [ ] **Step 3: 创建 LightConeEntity + Dao**

`app/src/main/java/com/java/myapplication/data/local/LightConeEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "light_cones")
data class LightConeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: Path,
    val rarity: Int,
    val passiveName: String,
    val passiveEffectJson: String,
    val s5Multiplier: Double
) {
    fun toModel(passiveEffect: PassiveEffect): LightCone = LightCone(
        id = id, name = name, path = path, rarity = rarity,
        passiveName = passiveName, passiveEffect = passiveEffect,
        s5Multiplier = s5Multiplier
    )

    companion object {
        fun fromModel(lc: LightCone): LightConeEntity = LightConeEntity(
            id = lc.id, name = lc.name, path = lc.path, rarity = lc.rarity,
            passiveName = lc.passiveName,
            passiveEffectJson = PassiveEffectJson.encode(lc.passiveEffect),
            s5Multiplier = lc.s5Multiplier
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/LightConeDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LightConeDao {
    @Query("SELECT * FROM light_cones")
    fun observeAll(): Flow<List<LightConeEntity>>

    @Query("SELECT * FROM light_cones")
    suspend fun getAll(): List<LightConeEntity>

    @Query("SELECT * FROM light_cones WHERE id = :id")
    suspend fun getById(id: String): LightConeEntity?

    @Query("SELECT COUNT(*) FROM light_cones")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cones: List<LightConeEntity>)

    @Query("DELETE FROM light_cones")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(cones: List<LightConeEntity>) {
        deleteAll()
        insertAll(cones)
    }
}
```

- [ ] **Step 4: 创建 PassiveEffectJson（编解码器）**

`app/src/main/java/com/java/myapplication/data/local/PassiveEffectJson.kt`:
```kotlin
package com.java.myapplication.data.local

import com.java.myapplication.data.model.*
import kotlinx.serialization.json.*

object PassiveEffectJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(effect: PassiveEffect): String = json.encodeToString(
        JsonElement.serializer(), toJsonElement(effect)
    )

    fun decode(s: String): PassiveEffect = fromJsonElement(json.parseToJsonElement(s))

    private fun toJsonElement(e: PassiveEffect): JsonElement = buildJsonObject {
        when (e) {
            is PassiveEffect.StatBoost -> {
                put("type", "StatBoost")
                put("stat", e.stat.name)
                put("value", e.value)
                put("target", e.target.name)
            }
            is PassiveEffect.DamageBonus -> {
                put("type", "DamageBonus")
                put("multiplier", e.multiplier)
                put("condition", e.condition.name)
            }
            is PassiveEffect.SkillBoost -> {
                put("type", "SkillBoost")
                put("skillType", e.type.name)
                put("multiplier", e.multiplier)
            }
            is PassiveEffect.EnergyRegen -> {
                put("type", "EnergyRegen")
                put("perTurn", e.perTurn)
            }
            is PassiveEffect.Composite -> {
                put("type", "Composite")
                putJsonArray("effects") { e.effects.forEach { add(toJsonElement(it)) } }
            }
        }
    }

    private fun fromJsonElement(el: JsonElement): PassiveEffect {
        val obj = el.jsonObject
        return when (val type = obj["type"]!!.jsonPrimitive.content) {
            "StatBoost" -> PassiveEffect.StatBoost(
                stat = StatType.valueOf(obj["stat"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double,
                target = Target.valueOf(obj["target"]!!.jsonPrimitive.content)
            )
            "DamageBonus" -> PassiveEffect.DamageBonus(
                multiplier = obj["multiplier"]!!.jsonPrimitive.double,
                condition = DmgCondition.valueOf(obj["condition"]!!.jsonPrimitive.content)
            )
            "SkillBoost" -> PassiveEffect.SkillBoost(
                type = SkillType.valueOf(obj["skillType"]!!.jsonPrimitive.content),
                multiplier = obj["multiplier"]!!.jsonPrimitive.double
            )
            "EnergyRegen" -> PassiveEffect.EnergyRegen(
                perTurn = obj["perTurn"]!!.jsonPrimitive.double
            )
            "Composite" -> PassiveEffect.Composite(
                effects = obj["effects"]!!.jsonArray.map { fromJsonElement(it) }
            )
            else -> error("Unknown PassiveEffect type: $type")
        }
    }
}
```

- [ ] **Step 5: 创建 RelicSetEntity + Dao**

`app/src/main/java/com/java/myapplication/data/local/RelicSetEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "relic_sets")
data class RelicSetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val twoPieceJson: String,
    val fourPieceJson: String,
    val suitableFor: Set<Role>
) {
    fun toModel(two: PassiveEffect, four: PassiveEffect): RelicSet = RelicSet(
        id = id, name = name, twoPiece = two, fourPiece = four,
        suitableFor = suitableFor
    )

    companion object {
        fun fromModel(r: RelicSet): RelicSetEntity = RelicSetEntity(
            id = r.id, name = r.name,
            twoPieceJson = PassiveEffectJson.encode(r.twoPiece),
            fourPieceJson = PassiveEffectJson.encode(r.fourPiece),
            suitableFor = r.suitableFor
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/RelicSetDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RelicSetDao {
    @Query("SELECT * FROM relic_sets")
    fun observeAll(): Flow<List<RelicSetEntity>>

    @Query("SELECT * FROM relic_sets")
    suspend fun getAll(): List<RelicSetEntity>

    @Query("SELECT * FROM relic_sets WHERE id = :id")
    suspend fun getById(id: String): RelicSetEntity?

    @Query("SELECT COUNT(*) FROM relic_sets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<RelicSetEntity>)

    @Query("DELETE FROM relic_sets")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(sets: List<RelicSetEntity>) {
        deleteAll()
        insertAll(sets)
    }
}
```

- [ ] **Step 6: 创建 EnemyEntity + Dao**

`app/src/main/java/com/java/myapplication/data/local/EnemyEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "enemies")
data class EnemyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val count: Int,
    val weaknesses: Set<Element>,
    val type: EnemyType,
    val hp: Double,
    val toughness: Double,
    val mechanics: Set<String>
) {
    fun toModel(): Enemy = Enemy(
        id = id, name = name, count = count,
        weaknesses = weaknesses, type = type,
        hp = hp, toughness = toughness, mechanics = mechanics
    )

    companion object {
        fun fromModel(e: Enemy): EnemyEntity = EnemyEntity(
            id = e.id, name = e.name, count = e.count,
            weaknesses = e.weaknesses, type = e.type,
            hp = e.hp, toughness = e.toughness, mechanics = e.mechanics
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/EnemyDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EnemyDao {
    @Query("SELECT * FROM enemies")
    fun observeAll(): Flow<List<EnemyEntity>>

    @Query("SELECT * FROM enemies")
    suspend fun getAll(): List<EnemyEntity>

    @Query("SELECT * FROM enemies WHERE id = :id")
    suspend fun getById(id: String): EnemyEntity?

    @Query("SELECT COUNT(*) FROM enemies")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(enemies: List<EnemyEntity>)

    @Query("DELETE FROM enemies")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(enemies: List<EnemyEntity>) {
        deleteAll()
        insertAll(enemies)
    }
}
```

- [ ] **Step 7: 创建 ScenarioEntity + Dao**

`app/src/main/java/com/java/myapplication/data/local/ScenarioEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enemyIds: List<String>,
    val difficulty: Int,
    val notes: String
) {
    fun toModel(enemies: List<Enemy>): Scenario = Scenario(
        id = id, name = name, enemies = enemies,
        difficulty = difficulty, notes = notes
    )

    companion object {
        fun fromModel(s: Scenario): ScenarioEntity = ScenarioEntity(
            id = s.id, name = s.name,
            enemyIds = s.enemies.map { it.id },
            difficulty = s.difficulty, notes = s.notes
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/ScenarioDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios")
    fun observeAll(): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenarios")
    suspend fun getAll(): List<ScenarioEntity>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    suspend fun getById(id: String): ScenarioEntity?

    @Query("SELECT COUNT(*) FROM scenarios")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scenarios: List<ScenarioEntity>)

    @Query("DELETE FROM scenarios")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(scenarios: List<ScenarioEntity>) {
        deleteAll()
        insertAll(scenarios)
    }
}
```

- [ ] **Step 8: 创建 EidolonEntity + Dao**

`app/src/main/java/com/java/myapplication/data/local/EidolonEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "eidolons")
data class EidolonEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val level: Int,
    val name: String,
    val effectJson: String,
    val major: Boolean
) {
    fun toModel(effect: EidolonEffect): Eidolon = Eidolon(
        id = id, characterId = characterId, level = level,
        name = name, effect = effect, major = major
    )

    companion object {
        fun fromModel(e: Eidolon): EidolonEntity = EidolonEntity(
            id = e.id, characterId = e.characterId, level = e.level,
            name = e.name,
            effectJson = EidolonEffectJson.encode(e.effect),
            major = e.major
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/EidolonDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EidolonDao {
    @Query("SELECT * FROM eidolons")
    fun observeAll(): Flow<List<EidolonEntity>>

    @Query("SELECT * FROM eidolons")
    suspend fun getAll(): List<EidolonEntity>

    @Query("SELECT * FROM eidolons WHERE characterId = :cid ORDER BY level ASC")
    suspend fun getForCharacter(cid: String): List<EidolonEntity>

    @Query("SELECT COUNT(*) FROM eidolons")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eidolons: List<EidolonEntity>)

    @Query("DELETE FROM eidolons")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(eidolons: List<EidolonEntity>) {
        deleteAll()
        insertAll(eidolons)
    }
}
```

- [ ] **Step 9: 创建 EidolonEffectJson（编解码器）**

`app/src/main/java/com/java/myapplication/data/local/EidolonEffectJson.kt`:
```kotlin
package com.java.myapplication.data.local

import com.java.myapplication.data.model.*
import kotlinx.serialization.json.*

object EidolonEffectJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(effect: EidolonEffect): String = json.encodeToString(
        JsonElement.serializer(), toJsonElement(effect)
    )

    fun decode(s: String): EidolonEffect = fromJsonElement(json.parseToJsonElement(s))

    private fun toJsonElement(e: EidolonEffect): JsonElement = buildJsonObject {
        when (e) {
            is EidolonEffect.StatBoost -> {
                put("type", "StatBoost")
                put("stat", e.stat.name)
                put("value", e.value)
                put("target", e.target.name)
            }
            is EidolonEffect.NewMechanic -> {
                put("type", "NewMechanic")
                put("mechanic", e.mechanic.name)
                put("param", e.param)
                put("note", e.note)
            }
            is EidolonEffect.DamageBonus -> {
                put("type", "DamageBonus")
                put("multiplier", e.multiplier)
                put("condition", e.condition.name)
            }
            is EidolonEffect.EnemyDebuff -> {
                put("type", "EnemyDebuff")
                put("stat", e.stat.name)
                put("value", e.value)
            }
            is EidolonEffect.Composite -> {
                put("type", "Composite")
                putJsonArray("effects") { e.effects.forEach { add(toJsonElement(it)) } }
            }
        }
    }

    private fun fromJsonElement(el: JsonElement): EidolonEffect {
        val obj = el.jsonObject
        return when (val type = obj["type"]!!.jsonPrimitive.content) {
            "StatBoost" -> EidolonEffect.StatBoost(
                stat = StatType.valueOf(obj["stat"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double,
                target = Target.valueOf(obj["target"]!!.jsonPrimitive.content)
            )
            "NewMechanic" -> EidolonEffect.NewMechanic(
                mechanic = Tag.valueOf(obj["mechanic"]!!.jsonPrimitive.content),
                param = obj["param"]!!.jsonPrimitive.double,
                note = obj["note"]!!.jsonPrimitive.content
            )
            "DamageBonus" -> EidolonEffect.DamageBonus(
                multiplier = obj["multiplier"]!!.jsonPrimitive.double,
                condition = DmgCondition.valueOf(obj["condition"]!!.jsonPrimitive.content)
            )
            "EnemyDebuff" -> EidolonEffect.EnemyDebuff(
                stat = StatType.valueOf(obj["stat"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double
            )
            "Composite" -> EidolonEffect.Composite(
                effects = obj["effects"]!!.jsonArray.map { fromJsonElement(it) }
            )
            else -> error("Unknown EidolonEffect type: $type")
        }
    }
}
```

- [ ] **Step 10: 创建 PlayerBuildEntity + Dao**

`app/src/main/java/com/java/myapplication/data/local/PlayerBuildEntity.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import com.java.myapplication.data.model.*

@Entity(tableName = "player_builds")
data class PlayerBuildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val level: Int,
    val ascension: Int,
    val lightConeId: String,
    val lightConeLevel: Int,
    val lightConeSuperimposition: Int,
    val relicSet4: String,
    val relicSet2: String?,
    val body: StatType,
    val boots: StatType,
    val sphere: StatType,
    val rope: StatType,
    val subStatsJson: String,
    val eidolons: Set<Int>,
    val notes: String
) {
    fun toModel(): PlayerBuild = PlayerBuild(
        id = id, characterId = characterId, level = level, ascension = ascension,
        lightConeId = lightConeId, lightConeLevel = lightConeLevel,
        lightConeSuperimposition = lightConeSuperimposition,
        relicSet4 = relicSet4, relicSet2 = relicSet2,
        mainStats = MainStats(body, boots, sphere, rope),
        subStats = SubStatJson.decode(subStatsJson),
        eidolons = eidolons, notes = notes
    )

    companion object {
        fun fromModel(b: PlayerBuild): PlayerBuildEntity = PlayerBuildEntity(
            id = b.id, characterId = b.characterId, level = b.level, ascension = b.ascension,
            lightConeId = b.lightConeId, lightConeLevel = b.lightConeLevel,
            lightConeSuperimposition = b.lightConeSuperimposition,
            relicSet4 = b.relicSet4, relicSet2 = b.relicSet2,
            body = b.mainStats.body, boots = b.mainStats.boots,
            sphere = b.mainStats.sphere, rope = b.mainStats.rope,
            subStatsJson = SubStatJson.encode(b.subStats),
            eidolons = b.eidolons, notes = b.notes
        )
    }
}
```

`app/src/main/java/com/java/myapplication/data/local/PlayerBuildDao.kt`:
```kotlin
package com.java.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerBuildDao {
    @Query("SELECT * FROM player_builds")
    fun observeAll(): Flow<List<PlayerBuildEntity>>

    @Query("SELECT * FROM player_builds WHERE characterId = :cid")
    fun observeForCharacter(cid: String): Flow<List<PlayerBuildEntity>>

    @Query("SELECT * FROM player_builds WHERE id = :id")
    suspend fun getById(id: Long): PlayerBuildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(build: PlayerBuildEntity): Long

    @Update
    suspend fun update(build: PlayerBuildEntity)

    @Delete
    suspend fun delete(build: PlayerBuildEntity)
}
```

- [ ] **Step 11: 创建 SubStatJson 编解码器**

`app/src/main/java/com/java/myapplication/data/local/SubStatJson.kt`:
```kotlin
package com.java.myapplication.data.local

import com.java.myapplication.data.model.*
import kotlinx.serialization.json.*

object SubStatJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(subs: List<SubStat>): String = json.encodeToString(
        JsonElement.serializer(),
        buildJsonArray {
            subs.forEach { sub ->
                add(buildJsonObject {
                    put("type", sub.type.name)
                    put("value", sub.value)
                    put("rolls", sub.rolls)
                })
            }
        }
    )

    fun decode(s: String): List<SubStat> {
        if (s.isEmpty()) return emptyList()
        val arr = json.parseToJsonElement(s).jsonArray
        return arr.map {
            val obj = it.jsonObject
            SubStat(
                type = StatType.valueOf(obj["type"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double,
                rolls = obj["rolls"]!!.jsonPrimitive.int
            )
        }
    }
}
```

- [ ] **Step 12: 创建 AppDatabase**

`app/src/main/java/com/java/myapplication/data/local/AppDatabase.kt`:
```kotlin
package com.java.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CharacterEntity::class,
        LightConeEntity::class,
        RelicSetEntity::class,
        EnemyEntity::class,
        ScenarioEntity::class,
        EidolonEntity::class,
        PlayerBuildEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun lightConeDao(): LightConeDao
    abstract fun relicSetDao(): RelicSetDao
    abstract fun enemyDao(): EnemyDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun eidolonDao(): EidolonDao
    abstract fun playerBuildDao(): PlayerBuildDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "starrail.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
```

- [ ] **Step 13: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -20`
Expected: `BUILD SUCCESSFUL`（KSP 会生成 DAO 实现）

- [ ] **Step 14: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/data/local/
git commit -m "feat(data): add Room entities, DAOs, and AppDatabase"
```

---

## Task 5: 种子 JSON 反序列化模型

**Files:**
- Create: `app/src/main/java/com/java/myapplication/data/seed/SeedData.kt`

- [ ] **Step 1: 创建 SeedData.kt**

```kotlin
package com.java.myapplication.data.seed

import com.java.myapplication.data.model.*
import kotlinx.serialization.Serializable

@Serializable
data class SeedRoot(
    val version: Int,
    val publishedAt: String,
    val characters: List<SeedCharacter>,
    val lightCones: List<SeedLightCone>,
    val relicSets: List<SeedRelicSet>,
    val enemies: List<SeedEnemy>,
    val scenarios: List<SeedScenario>,
    val eidolons: List<SeedEidolon>
)

@Serializable
data class SeedCharacter(
    val id: String, val name: String, val rarity: Int,
    val path: String, val element: String, val role: String,
    val tags: List<String>,
    val baseStats: SeedStats,
    val scaling: SeedScaling,
    val cycleProfile: SeedCycleProfile? = null,
    val iconUrl: String,
    val version: Int
)

@Serializable
data class SeedStats(val hp: Double, val atk: Double, val def: Double, val spd: Double)

@Serializable
data class SeedScaling(
    val skillMult: Double, val ultMult: Double, val talentMult: Double,
    val followUpMult: Double = 0.0, val aoeRatio: Double = 0.0
)

@Serializable
data class SeedCycleProfile(
    val cycleActions: Int,
    val spdBreakpoints: List<Double> = emptyList(),
    val isFollowUp: Boolean = false,
    val isDot: Boolean = false
)

@Serializable
data class SeedLightCone(
    val id: String, val name: String, val path: String, val rarity: Int,
    val passiveName: String,
    val passiveEffect: SeedPassiveEffect,
    val s5Multiplier: Double = 1.0
)

@Serializable
data class SeedPassiveEffect(
    val type: String,                              // StatBoost / DamageBonus / SkillBoost / EnergyRegen / Composite
    val stat: String? = null,                      // for StatBoost
    val value: Double? = null,
    val target: String? = null,
    val multiplier: Double? = null,                // for DamageBonus / SkillBoost
    val condition: String? = null,
    val skillType: String? = null,                 // for SkillBoost
    val perTurn: Double? = null,                   // for EnergyRegen
    val effects: List<SeedPassiveEffect> = emptyList()  // for Composite
)

@Serializable
data class SeedRelicSet(
    val id: String, val name: String,
    val twoPiece: SeedPassiveEffect,
    val fourPiece: SeedPassiveEffect,
    val suitableFor: List<String>
)

@Serializable
data class SeedEnemy(
    val id: String, val name: String, val count: Int,
    val weaknesses: List<String>, val type: String,
    val hp: Double, val toughness: Double = 0.0,
    val mechanics: List<String> = emptyList()
)

@Serializable
data class SeedScenario(
    val id: String, val name: String,
    val enemyIds: List<String>,
    val difficulty: Int, val notes: String = ""
)

@Serializable
data class SeedEidolon(
    val id: String, val characterId: String, val level: Int,
    val name: String,
    val effect: SeedEidolonEffect,
    val major: Boolean = false
)

@Serializable
data class SeedEidolonEffect(
    val type: String,                              // StatBoost / NewMechanic / DamageBonus / EnemyDebuff / Composite
    val stat: String? = null,
    val value: Double? = null,
    val target: String? = null,
    val mechanic: String? = null,
    val param: Double? = null,
    val note: String? = null,
    val multiplier: Double? = null,
    val condition: String? = null,
    val effects: List<SeedEidolonEffect> = emptyList()
)
```

- [ ] **Step 2: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/data/seed/SeedData.kt
git commit -m "feat(seed): add JSON deserialization models"
```

---

## Task 6: SeedImporter（导入资产文件到 Room）

**Files:**
- Create: `app/src/main/java/com/java/myapplication/data/seed/SeedImporter.kt`

- [ ] **Step 1: 创建 SeedImporter.kt**

```kotlin
package com.java.myapplication.data.seed

import android.content.Context
import com.java.myapplication.data.local.*
import com.java.myapplication.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class SeedImporter(
    private val context: Context,
    private val db: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importFromAssets(assetPath: String = "seed-data-v1.json"): ImportResult =
        withContext(Dispatchers.IO) {
            val root = try {
                context.assets.open(assetPath).use { input ->
                    json.decodeFromString(SeedRoot.serializer(), input.readBytes().decodeToString())
                }
            } catch (e: IOException) {
                return@withContext ImportResult.Failed("Cannot read $assetPath: ${e.message}")
            } catch (e: Exception) {
                return@withContext ImportResult.Failed("Parse error: ${e.message}")
            }

            try {
                db.runInTransaction {
                    // Characters
                    val chars = root.characters.map { it.toModel() }
                    db.characterDao().insertAll(chars.map(CharacterEntity::fromModel))

                    // LightCones
                    val cones = root.lightCones.map { it.toModel() }
                    db.lightConeDao().insertAll(cones.map(LightConeEntity::fromModel))

                    // RelicSets
                    val sets = root.relicSets.map { it.toModel() }
                    db.relicSetDao().insertAll(sets.map(RelicSetEntity::fromModel))

                    // Enemies
                    val enemies = root.enemies.map { it.toModel() }
                    db.enemyDao().insertAll(enemies.map(EnemyEntity::fromModel))

                    // Scenarios
                    val scenarios = root.scenarios.map { it.toModel() }
                    db.scenarioDao().insertAll(scenarios.map(ScenarioEntity::fromModel))

                    // Eidolons
                    val eidolons = root.eidolons.map { it.toModel() }
                    db.eidolonDao().insertAll(eidolons.map(EidolonEntity::fromModel))
                }
                ImportResult.Success(
                    characters = root.characters.size,
                    lightCones = root.lightCones.size,
                    relicSets = root.relicSets.size,
                    enemies = root.enemies.size,
                    scenarios = root.scenarios.size,
                    eidolons = root.eidolons.size
                )
            } catch (e: Exception) {
                ImportResult.Failed("DB error: ${e.message}")
            }
        }

    sealed interface ImportResult {
        data class Success(
            val characters: Int, val lightCones: Int, val relicSets: Int,
            val enemies: Int, val scenarios: Int, val eidolons: Int
        ) : ImportResult
        data class Failed(val reason: String) : ImportResult
    }

    // ===== Mappers =====

    private fun SeedCharacter.toModel(): Character = Character(
        id = id, name = name, rarity = rarity,
        path = Path.valueOf(path), element = Element.valueOf(element), role = Role.valueOf(role),
        tags = tags.map(Tag::valueOf).toSet(),
        baseStats = Stats(baseStats.hp, baseStats.atk, baseStats.def, baseStats.spd),
        scaling = Scaling(scaling.skillMult, scaling.ultMult, scaling.talentMult,
            scaling.followUpMult, scaling.aoeRatio),
        cycleProfile = cycleProfile?.let { cp ->
            CycleProfile(cp.cycleActions, cp.spdBreakpoints, cp.isFollowUp, cp.isDot)
        },
        iconUrl = iconUrl, version = version
    )

    private fun SeedLightCone.toModel(): LightCone = LightCone(
        id = id, name = name, path = Path.valueOf(path), rarity = rarity,
        passiveName = passiveName,
        passiveEffect = passiveEffect.toModel(),
        s5Multiplier = s5Multiplier
    )

    private fun SeedPassiveEffect.toModel(): PassiveEffect = when (type) {
        "StatBoost" -> PassiveEffect.StatBoost(
            stat = StatType.valueOf(stat!!),
            value = value!!,
            target = target?.let { Target.valueOf(it) } ?: Target.SELF
        )
        "DamageBonus" -> PassiveEffect.DamageBonus(
            multiplier = multiplier!!,
            condition = DmgCondition.valueOf(condition!!)
        )
        "SkillBoost" -> PassiveEffect.SkillBoost(
            type = SkillType.valueOf(skillType!!),
            multiplier = multiplier!!
        )
        "EnergyRegen" -> PassiveEffect.EnergyRegen(perTurn = perTurn!!)
        "Composite" -> PassiveEffect.Composite(effects.map { it.toModel() })
        else -> error("Unknown PassiveEffect type: $type")
    }

    private fun SeedRelicSet.toModel(): RelicSet = RelicSet(
        id = id, name = name,
        twoPiece = twoPiece.toModel(),
        fourPiece = fourPiece.toModel(),
        suitableFor = suitableFor.map(Role::valueOf).toSet()
    )

    private fun SeedEnemy.toModel(): Enemy = Enemy(
        id = id, name = name, count = count,
        weaknesses = weaknesses.map(Element::valueOf).toSet(),
        type = EnemyType.valueOf(type),
        hp = hp, toughness = toughness, mechanics = mechanics.toSet()
    )

    private fun SeedScenario.toModel(): Scenario = Scenario(
        id = id, name = name, enemyIds = enemyIds,
        difficulty = difficulty, notes = notes
    )

    private fun SeedEidolon.toModel(): Eidolon = Eidolon(
        id = id, characterId = characterId, level = level,
        name = name, effect = effect.toModel(), major = major
    )

    private fun SeedEidolonEffect.toModel(): EidolonEffect = when (type) {
        "StatBoost" -> EidolonEffect.StatBoost(
            stat = StatType.valueOf(stat!!),
            value = value!!,
            target = target?.let { Target.valueOf(it) } ?: Target.SELF
        )
        "NewMechanic" -> EidolonEffect.NewMechanic(
            mechanic = Tag.valueOf(mechanic!!),
            param = param ?: 1.0,
            note = note ?: ""
        )
        "DamageBonus" -> EidolonEffect.DamageBonus(
            multiplier = multiplier!!,
            condition = DmgCondition.valueOf(condition!!)
        )
        "EnemyDebuff" -> EidolonEffect.EnemyDebuff(
            stat = StatType.valueOf(stat!!),
            value = value!!
        )
        "Composite" -> EidolonEffect.Composite(effects.map { it.toModel() })
        else -> error("Unknown EidolonEffect type: $type")
    }
}

private fun com.java.myapplication.data.local.ScenarioEntity.toModel(): Scenario = Scenario(
    id = id, name = name, enemies = emptyList(),  // populated separately if needed
    difficulty = difficulty, notes = notes
)
```

**注意**：上面 Scenario 导入只存了 enemyIds，enemies 列表要通过 EnemyDao 查——这是设计权衡。如果业务需要完整 Scenario.enemies，重构为 Scenario 不用 enemyIds，存 JSON。这里保持简单。

- [ ] **Step 2: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -20`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/data/seed/SeedImporter.kt
git commit -m "feat(seed): add SeedImporter with JSON->DB mapper"
```

---

## Task 7: 种子数据 JSON（5 角色 + 5 光锥 + 3 遗器 + 10 敌人 + 3 场景 + 5×6 星魂）

**Files:**
- Create: `app/src/main/assets/seed-data-v1.json`
- Create: `app/src/main/assets/seed-changelog.md`

- [ ] **Step 1: 创建 seed-changelog.md**

`app/src/main/assets/seed-changelog.md`:
```markdown
# Seed Data v1 Changelog

首版种子数据（2026-07-03）。

## 内容
- 5 个角色：希儿、姬子、景元、卡芙卡、布洛妮娅
- 5 把光锥：拂晓之前、沿用追迹、胜利时刻、银河铁道之夜、致明日
- 3 套遗器：熔岩锻造者的火套、街头出身的拳套、量子套
- 10 个敌人：含 BOSS/精英/小怪
- 3 个场景：混沌回忆 1/2、虚构叙事 1
- 5×6=30 条星魂

## 数据来源
- 数值参考公开 wiki 整理，**非官方**，仅作示例
- 后续版本会通过 App 内 wiki 抓取器增量更新

## 使用
App 首次启动时自动导入 Room（`SeedImporter`）。已存在的数据不会重复导入（用 `OnConflictStrategy.REPLACE`）。
```

- [ ] **Step 2: 创建 seed-data-v1.json**

`app/src/main/assets/seed-data-v1.json`（完整文件，**注意 JSON 格式严格**）：

```json
{
  "version": 1,
  "publishedAt": "2026-07-03T00:00:00Z",
  "characters": [
    {
      "id": "seele",
      "name": "希儿",
      "rarity": 5,
      "path": "HUNT",
      "element": "QUANTUM",
      "role": "DPS",
      "tags": ["SINGLE_TARGET", "CRIT_BOOST", "FOLLOW_UP"],
      "baseStats": { "hp": 931.0, "atk": 756.0, "def": 363.0, "spd": 115 },
      "scaling": { "skillMult": 2.2, "ultMult": 4.2, "talentMult": 3.0, "followUpMult": 2.0, "aoeRatio": 0.0 },
      "cycleProfile": { "cycleActions": 4, "spdBreakpoints": [134, 143, 160], "isFollowUp": true },
      "iconUrl": "https://example.com/seele.png",
      "version": 1
    },
    {
      "id": "himeko",
      "name": "姬子",
      "rarity": 5,
      "path": "MAGE",
      "element": "FIRE",
      "role": "DPS",
      "tags": ["AOE", "FOLLOW_UP", "ULT_DMG_BONUS"],
      "baseStats": { "hp": 1041.0, "atk": 756.0, "def": 363.0, "spd": 112 },
      "scaling": { "skillMult": 1.6, "ultMult": 3.4, "talentMult": 1.5, "followUpMult": 1.4, "aoeRatio": 0.8 },
      "cycleProfile": { "cycleActions": 4, "spdBreakpoints": [134, 143, 160], "isFollowUp": true },
      "iconUrl": "https://example.com/himeko.png",
      "version": 1
    },
    {
      "id": "jingyuan",
      "name": "景元",
      "rarity": 5,
      "path": "MAGE",
      "element": "LIGHTNING",
      "role": "DPS",
      "tags": ["AOE", "FOLLOW_UP", "ULT_DMG_BONUS"],
      "baseStats": { "hp": 1164.0, "atk": 698.0, "def": 485.0, "spd": 99 },
      "scaling": { "skillMult": 1.4, "ultMult": 2.8, "talentMult": 2.5, "followUpMult": 1.3, "aoeRatio": 0.7 },
      "cycleProfile": { "cycleActions": 3, "spdBreakpoints": [134, 143, 160], "isFollowUp": true },
      "iconUrl": "https://example.com/jingyuan.png",
      "version": 1
    },
    {
      "id": "kafka",
      "name": "卡芙卡",
      "rarity": 5,
      "path": "WARLOCK",
      "element": "LIGHTNING",
      "role": "DPS",
      "tags": ["DOT", "ULT_DMG_BONUS", "DEBUFF"],
      "baseStats": { "hp": 1106.0, "atk": 706.0, "def": 461.0, "spd": 112 },
      "scaling": { "skillMult": 2.0, "ultMult": 2.6, "talentMult": 2.8, "followUpMult": 0.0, "aoeRatio": 0.3 },
      "cycleProfile": { "cycleActions": 4, "spdBreakpoints": [134, 143, 160], "isDot": true },
      "iconUrl": "https://example.com/kafka.png",
      "version": 1
    },
    {
      "id": "bronya",
      "name": "布洛妮娅",
      "rarity": 5,
      "path": "HARMONY",
      "element": "WIND",
      "role": "SUPPORT",
      "tags": ["ACTION_ADVANCE", "ATK_BOOST", "SPEED_BOOST"],
      "baseStats": { "hp": 1241.0, "atk": 582.0, "def": 533.0, "spd": 134 },
      "scaling": { "skillMult": 0.0, "ultMult": 1.0, "talentMult": 0.0, "followUpMult": 0.0, "aoeRatio": 0.0 },
      "cycleProfile": { "cycleActions": 5, "spdBreakpoints": [160] },
      "iconUrl": "https://example.com/bronya.png",
      "version": 1
    }
  ],
  "lightCones": [
    {
      "id": "in_the_night",
      "name": "拂晓之前",
      "path": "HUNT",
      "rarity": 5,
      "passiveName": "夜晚的尽头",
      "passiveEffect": { "type": "DamageBonus", "multiplier": 0.6, "condition": "ALWAYS" },
      "s5Multiplier": 1.0
    },
    {
      "id": "along_the_passing_shine",
      "name": "沿用追迹",
      "path": "HUNT",
      "rarity": 4,
      "passiveName": "前进的勇气",
      "passiveEffect": { "type": "DamageBonus", "multiplier": 0.4, "condition": "ALWAYS" },
      "s5Multiplier": 1.0
    },
    {
      "id": "victory_moment",
      "name": "胜利时刻",
      "path": "HUNT",
      "rarity": 3,
      "passiveName": "致胜",
      "passiveEffect": { "type": "StatBoost", "stat": "ATK", "value": 0.2, "target": "SELF" },
      "s5Multiplier": 1.0
    },
    {
      "id": "galactic_railway_night",
      "name": "银河铁道之夜",
      "path": "MAGE",
      "rarity": 5,
      "passiveName": "星光璀璨",
      "passiveEffect": { "type": "Composite", "effects": [
        { "type": "DamageBonus", "multiplier": 0.3, "condition": "ULT_ACTIVE" },
        { "type": "StatBoost", "stat": "ATK", "value": 0.2, "target": "SELF" }
      ] },
      "s5Multiplier": 1.0
    },
    {
      "id": "to_tomorrow",
      "name": "致明日",
      "path": "HARMONY",
      "rarity": 5,
      "passiveName": "前进",
      "passiveEffect": { "type": "Composite", "effects": [
        { "type": "StatBoost", "stat": "ATK", "value": 0.4, "target": "SELF" },
        { "type": "StatBoost", "stat": "SPD", "value": 0.12, "target": "SELF" }
      ] },
      "s5Multiplier": 1.0
    }
  ],
  "relicSets": [
    {
      "id": "forge_of_lava",
      "name": "熔岩锻造者的火套",
      "twoPiece": { "type": "StatBoost", "stat": "ATK", "value": 0.12, "target": "SELF" },
      "fourPiece": { "type": "DamageBonus", "multiplier": 0.12, "condition": "ALWAYS" },
      "suitableFor": ["DPS", "SUB_DPS"]
    },
    {
      "id": "street_glove",
      "name": "街头出身的拳套",
      "twoPiece": { "type": "StatBoost", "stat": "ATK", "value": 0.12, "target": "SELF" },
      "fourPiece": { "type": "DamageBonus", "multiplier": 0.15, "condition": "FOLLOW_UP" },
      "suitableFor": ["DPS"]
    },
    {
      "id": "quantum_set",
      "name": "量子套",
      "twoPiece": { "type": "StatBoost", "stat": "ATK", "value": 0.12, "target": "SELF" },
      "fourPiece": { "type": "DamageBonus", "multiplier": 0.10, "condition": "ALWAYS" },
      "suitableFor": ["DPS", "SUPPORT"]
    }
  ],
  "enemies": [
    { "id": "boss_cocolia", "name": "可可利亚", "count": 1, "weaknesses": ["QUANTUM", "WIND"], "type": "BOSS", "hp": 200000, "toughness": 240, "mechanics": ["冰封", "召唤"] },
    { "id": "boss_frigid_pillar", "name": "寒冰柱", "count": 1, "weaknesses": ["FIRE"], "type": "BOSS", "hp": 180000, "toughness": 200, "mechanics": [] },
    { "id": "elite_watcher", "name": "反物质监视者", "count": 1, "weaknesses": ["FIRE", "IMAGINARY"], "type": "ELITE", "hp": 80000, "toughness": 120 },
    { "id": "elite_shadow_jackal", "name": "暗影豺狼", "count": 1, "weaknesses": ["ICE"], "type": "ELITE", "hp": 60000, "toughness": 100 },
    { "id": "mob_thief", "name": "虚卒·掠夺者", "count": 3, "weaknesses": ["PHYSICAL", "QUANTUM"], "type": "MOB", "hp": 30000, "toughness": 60 },
    { "id": "mob_mage", "name": "虚卒·咒法师", "count": 2, "weaknesses": ["LIGHTNING"], "type": "MOB", "hp": 25000, "toughness": 50 },
    { "id": "mob_dog", "name": "虚卒·犬", "count": 4, "weaknesses": ["FIRE"], "type": "MOB", "hp": 15000, "toughness": 30 },
    { "id": "summon_ice_pillar", "name": "冰柱召唤物", "count": 1, "weaknesses": ["FIRE"], "type": "SUMMON", "hp": 20000, "toughness": 30 },
    { "id": "doom_keeper", "name": "毁灭者", "count": 1, "weaknesses": ["WIND"], "type": "DOOM", "hp": 300000, "toughness": 360, "mechanics": ["全队易伤"] },
    { "id": "elite_lackey", "name": "打手", "count": 2, "weaknesses": ["IMAGINARY"], "type": "ELITE", "hp": 50000, "toughness": 90 }
  ],
  "scenarios": [
    { "id": "mf_1", "name": "混沌回忆 第一期", "enemyIds": ["boss_cocolia", "elite_watcher", "mob_thief"], "difficulty": 4, "notes": "建议带风/量子" },
    { "id": "mf_2", "name": "混沌回忆 第二期", "enemyIds": ["doom_keeper", "elite_lackey"], "difficulty": 5, "notes": "考验生存" },
    { "id": "pf_1", "name": "虚构叙事 第一期", "enemyIds": ["mob_thief", "mob_mage", "mob_dog", "summon_ice_pillar"], "difficulty": 3, "notes": "对群场合" }
  ],
  "eidolons": [
    { "id": "seele_e1", "characterId": "seele", "level": 1, "name": "再相会", "effect": { "type": "StatBoost", "stat": "CRIT_DMG", "value": 0.2, "target": "SELF" }, "major": false },
    { "id": "seele_e2", "characterId": "seele", "level": 2, "name": "蝴蝶效应", "effect": { "type": "Composite", "effects": [
      { "type": "StatBoost", "stat": "SPD", "value": 0.3, "target": "SELF" }
    ] }, "major": true },
    { "id": "seele_e3", "characterId": "seele", "level": 3, "name": "破晓", "effect": { "type": "SkillBoost", "skillType": "ULT", "multiplier": 0.2 }, "major": false },
    { "id": "seele_e4", "characterId": "seele", "level": 4, "name": "突袭", "effect": { "type": "DamageBonus", "multiplier": 0.2, "condition": "FOLLOW_UP" }, "major": false },
    { "id": "seele_e5", "characterId": "seele", "level": 5, "name": "加速", "effect": { "type": "SkillBoost", "skillType": "SKILL", "multiplier": 0.2 }, "major": false },
    { "id": "seele_e6", "characterId": "seele", "level": 6, "name": "重逢", "effect": { "type": "NewMechanic", "mechanic": "FOLLOW_UP", "param": 1.0, "note": "再现状态下追击必暴击" }, "major": true },

    { "id": "kafka_e1", "characterId": "kafka", "level": 1, "name": "折磨", "effect": { "type": "DamageBonus", "multiplier": 0.2, "condition": "DOT" }, "major": false },
    { "id": "kafka_e2", "characterId": "kafka", "level": 2, "name": "尖叫", "effect": { "type": "NewMechanic", "mechanic": "DOT", "param": 2.0, "note": "DOT 伤害翻倍" }, "major": true },
    { "id": "kafka_e3", "characterId": "kafka", "level": 3, "name": "私语", "effect": { "type": "SkillBoost", "skillType": "ULT", "multiplier": 0.2 }, "major": false },
    { "id": "kafka_e4", "characterId": "kafka", "level": 4, "name": "入侵", "effect": { "type": "StatBoost", "stat": "EHR", "value": 0.3, "target": "SELF" }, "major": false },
    { "id": "kafka_e5", "characterId": "kafka", "level": 5, "name": "渗透", "effect": { "type": "SkillBoost", "skillType": "SKILL", "multiplier": 0.2 }, "major": false },
    { "id": "kafka_e6", "characterId": "kafka", "level": 6, "name": "控制", "effect": { "type": "NewMechanic", "mechanic": "DEBUFF", "param": 1.0, "note": "DOT 触发立即再触发一次" }, "major": true },

    { "id": "bronya_e1", "characterId": "bronya", "level": 1, "name": "战术", "effect": { "type": "NewMechanic", "mechanic": "ACTION_ADVANCE", "param": 1.0, "note": "拉条 100%" }, "major": true },
    { "id": "bronya_e2", "characterId": "bronya", "level": 2, "name": "协同", "effect": { "type": "StatBoost", "stat": "ATK", "value": 0.15, "target": "SELF" }, "major": false },
    { "id": "bronya_e3", "characterId": "bronya", "level": 3, "name": "节拍", "effect": { "type": "SkillBoost", "skillType": "ULT", "multiplier": 0.2 }, "major": false },
    { "id": "bronya_e4", "characterId": "bronya", "level": 4, "name": "协奏", "effect": { "type": "StatBoost", "stat": "CRIT_DMG", "value": 0.2, "target": "ALLY" }, "major": false },
    { "id": "bronya_e5", "characterId": "bronya", "level": 5, "name": "战鼓", "effect": { "type": "SkillBoost", "skillType": "SKILL", "multiplier": 0.2 }, "major": false },
    { "id": "bronya_e6", "characterId": "bronya", "level": 6, "name": "终章", "effect": { "type": "NewMechanic", "mechanic": "ACTION_ADVANCE", "param": 2.0, "note": "战技后下个队友也获得拉条 50%" }, "major": true },

    { "id": "himeko_e1", "characterId": "himeko", "level": 1, "name": "热情", "effect": { "type": "StatBoost", "stat": "ATK", "value": 0.2, "target": "SELF" }, "major": false },
    { "id": "himeko_e2", "characterId": "himeko", "level": 2, "name": "赤焰", "effect": { "type": "DamageBonus", "multiplier": 0.25, "condition": "FOLLOW_UP" }, "major": true },
    { "id": "himeko_e3", "characterId": "himeko", "level": 3, "name": "炎舞", "effect": { "type": "SkillBoost", "skillType": "ULT", "multiplier": 0.2 }, "major": false },
    { "id": "himeko_e4", "characterId": "himeko", "level": 4, "name": "燃烧", "effect": { "type": "StatBoost", "stat": "CRIT_RATE", "value": 0.12, "target": "SELF" }, "major": false },
    { "id": "himeko_e5", "characterId": "himeko", "level": 5, "name": "星火", "effect": { "type": "SkillBoost", "skillType": "SKILL", "multiplier": 0.2 }, "major": false },
    { "id": "himeko_e6", "characterId": "himeko", "level": 6, "name": "天火", "effect": { "type": "NewMechanic", "mechanic": "FOLLOW_UP", "param": 1.0, "note": "追击伤害 +30%" }, "major": true },

    { "id": "jingyuan_e1", "characterId": "jingyuan", "level": 1, "name": "雷电", "effect": { "type": "DamageBonus", "multiplier": 0.2, "condition": "FOLLOW_UP" }, "major": false },
    { "id": "jingyuan_e2", "characterId": "jingyuan", "level": 2, "name": "神君", "effect": { "type": "DamageBonus", "multiplier": 0.25, "condition": "FOLLOW_UP" }, "major": true },
    { "id": "jingyuan_e3", "characterId": "jingyuan", "level": 3, "name": "威压", "effect": { "type": "SkillBoost", "skillType": "ULT", "multiplier": 0.2 }, "major": false },
    { "id": "jingyuan_e4", "characterId": "jingyuan", "level": 4, "name": "雷鸣", "effect": { "type": "StatBoost", "stat": "CRIT_RATE", "value": 0.12, "target": "SELF" }, "major": false },
    { "id": "jingyuan_e5", "characterId": "jingyuan", "level": 5, "name": "天雷", "effect": { "type": "SkillBoost", "skillType": "SKILL", "multiplier": 0.2 }, "major": false },
    { "id": "jingyuan_e6", "characterId": "jingyuan", "level": 6, "name": "终末", "effect": { "type": "NewMechanic", "mechanic": "FOLLOW_UP", "param": 1.0, "note": "神君层数上限 +6" }, "major": true }
  ]
}
```

- [ ] **Step 3: 验证 JSON 格式**

Run: `cat /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622/app/src/main/assets/seed-data-v1.json | python3 -m json.tool > /dev/null && echo "JSON OK"`
Expected: `JSON OK`

- [ ] **Step 4: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/assets/
git commit -m "feat(seed): add seed-data-v1.json (5 chars, 5 cones, 3 sets, 10 enemies, 3 scenarios, 30 eidolons)"
```

---

## Task 8: SeedImporter 单元测试（TDD）

**Files:**
- Create: `app/src/test/java/com/java/myassistance/.../SeedImporterTest.kt`

- [ ] **Step 1: 创建测试（用 Robolectric 模拟 Context）**

`app/src/test/java/com/java/myapplication/data/seed/SeedImporterTest.kt`:
```kotlin
package com.java.myapplication.data.seed

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.local.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class SeedImporterTest {

    private lateinit var db: AppDatabase
    private lateinit var importer: SeedImporter

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = SeedImporter(ctx, db)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `import from default asset populates all tables`() = runTest {
        val result = importer.importFromAssets()
        assertThat(result).isInstanceOf(SeedImporter.ImportResult.Success::class.java)
        val s = result as SeedImporter.ImportResult.Success
        assertThat(s.characters).isEqualTo(5)
        assertThat(s.lightCones).isEqualTo(5)
        assertThat(s.relicSets).isEqualTo(3)
        assertThat(s.enemies).isEqualTo(10)
        assertThat(s.scenarios).isEqualTo(3)
        assertThat(s.eidolons).isEqualTo(30)

        // Verify a known character
        val seele = db.characterDao().getById("seele")
        assertThat(seele).isNotNull()
        assertThat(seele!!.name).isEqualTo("希儿")
        assertThat(seele.element.name).isEqualTo("QUANTUM")
        assertThat(seele.tags).contains(Tag.SINGLE_TARGET)
    }

    @Test
    fun `import from missing asset returns Failed`() = runTest {
        val result = importer.importFromAssets("nonexistent.json")
        assertThat(result).isInstanceOf(SeedImporter.ImportResult.Failed::class.java)
        val failed = result as SeedImporter.ImportResult.Failed
        assertThat(failed.reason).contains("nonexistent.json")
    }

    @Test
    fun `light cone passive effect roundtrips through json`() = runTest {
        importer.importFromAssets()
        val cone = db.lightConeDao().getById("galactic_railway_night")!!
        val model = cone.toModel(
            com.java.myapplication.data.local.PassiveEffectJson.decode(cone.passiveEffectJson)
        )
        assertThat(model.passiveEffect).isInstanceOf(
            com.java.myapplication.data.model.PassiveEffect.Composite::class.java
        )
        val composite = model.passiveEffect as com.java.myapplication.data.model.PassiveEffect.Composite
        assertThat(composite.effects).hasSize(2)
    }

    @Test
    fun `eidolon kafka E2 is major new mechanic DOT`() = runTest {
        importer.importFromAssets()
        val eidolons = db.eidolonDao().getForCharacter("kafka")
        val e2 = eidolons.first { it.level == 2 }
        assertThat(e2.major).isTrue()
        val effect = com.java.myapplication.data.local.EidolonEffectJson.decode(e2.effectJson)
        assertThat(effect).isInstanceOf(com.java.myapplication.data.model.EidolonEffect.NewMechanic::class.java)
        val nm = effect as com.java.myapplication.data.model.EidolonEffect.NewMechanic
        assertThat(nm.mechanic).isEqualTo(com.java.myapplication.data.model.Tag.DOT)
        assertThat(nm.param).isEqualTo(2.0)
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew testDebugUnitTest --no-daemon --tests "com.java.myapplication.data.seed.SeedImporterTest" 2>&1 | tail -30`
Expected: `BUILD SUCCESSFUL` + 4 tests pass

- [ ] **Step 3: 修复发现的问题（如有）**

如果测试失败，**根据错误信息修复**。常见问题：
- `Tag` 引用未 import：补全 import
- `ScenarioEntity.toModel()` 错误：调整

- [ ] **Step 4: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/test/java/com/java/myapplication/data/seed/SeedImporterTest.kt
git commit -m "test(seed): SeedImporterTest with 4 cases"
```

---

## Task 9: CharacterRepository 骨架（M4 才会用，先建接口）

**Files:**
- Create: `app/src/main/java/com/java/myapplication/data/repository/CharacterRepository.kt`

- [ ] **Step 1: 创建 Repository**

```kotlin
package com.java.myapplication.data.repository

import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CharacterRepository(private val db: AppDatabase) {

    fun observeAllCharacters(): Flow<List<Character>> =
        db.characterDao().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getCharacter(id: String): Character? =
        db.characterDao().getById(id)?.toModel()

    fun observeAllLightCones(): Flow<List<LightCone>> =
        db.lightConeDao().observeAll().map { list ->
            list.map { entity ->
                entity.toModel(
                    com.java.myapplication.data.local.PassiveEffectJson.decode(entity.passiveEffectJson)
                )
            }
        }

    suspend fun getLightCone(id: String): LightCone? =
        db.lightConeDao().getById(id)?.let { entity ->
            entity.toModel(
                com.java.myapplication.data.local.PassiveEffectJson.decode(entity.passiveEffectJson)
            )
        }

    fun observeAllRelicSets(): Flow<List<RelicSet>> =
        db.relicSetDao().observeAll().map { list ->
            list.map { entity ->
                entity.toModel(
                    com.java.myapplication.data.local.PassiveEffectJson.decode(entity.twoPieceJson),
                    com.java.myapplication.data.local.PassiveEffectJson.decode(entity.fourPieceJson)
                )
            }
        }

    fun observeAllEnemies(): Flow<List<Enemy>> =
        db.enemyDao().observeAll().map { list -> list.map { it.toModel() } }

    fun observeAllScenarios(): Flow<List<Scenario>> =
        db.scenarioDao().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getEidolonsFor(characterId: String): List<Eidolon> {
        return db.eidolonDao().getForCharacter(characterId).map { entity ->
            entity.toModel(
                com.java.myapplication.data.local.EidolonEffectJson.decode(entity.effectJson)
            )
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 写最小测试**

`app/src/test/java/com/java/myapplication/data/repository/CharacterRepositoryTest.kt`:
```kotlin
package com.java.myapplication.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.seed.SeedImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class CharacterRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: CharacterRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = CharacterRepository(db)
    }

    @After fun tearDown() { db.close() }

    @Test fun `repository returns imported characters`() = runTest {
        SeedImporter(ApplicationProvider.getApplicationContext(), db).importFromAssets()
        val seele = repo.getCharacter("seele")
        assertThat(seele).isNotNull()
        assertThat(seele!!.element).isEqualTo(com.java.myapplication.data.model.Element.QUANTUM)
    }

    @Test fun `light cone flow emits models with decoded passive`() = runTest {
        SeedImporter(ApplicationProvider.getApplicationContext(), db).importFromAssets()
        val cones = repo.observeAllLightCones().first()
        assertThat(cones).hasSize(5)
        val composite = cones.first { it.id == "galactic_railway_night" }
        assertThat(composite.passiveEffect).isInstanceOf(
            com.java.myapplication.data.model.PassiveEffect.Composite::class.java
        )
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew testDebugUnitTest --no-daemon --tests "com.java.myapplication.data.repository.CharacterRepositoryTest" 2>&1 | tail -15`
Expected: `BUILD SUCCESSFUL` + 2 tests pass

- [ ] **Step 5: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/data/repository/CharacterRepository.kt
git add app/src/test/java/com/java/myapplication/data/repository/CharacterRepositoryTest.kt
git commit -m "feat(data): CharacterRepository skeleton + 2 tests"
```

---

## Task 10: Application 类（启动时导入种子）

**Files:**
- Create: `app/src/main/java/com/java/myapplication/StarRailApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`（注册 applicationName）

- [ ] **Step 1: 创建 StarRailApp.kt**

```kotlin
package com.java.myapplication

import android.app.Application
import androidx.work.Configuration
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.data.seed.SeedImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StarRailApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: CharacterRepository by lazy { CharacterRepository(database) }
    val seedImporter: SeedImporter by lazy { SeedImporter(this, database) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // 首次启动导入种子
            if (database.characterDao().count() == 0) {
                seedImporter.importFromAssets()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
```

- [ ] **Step 2: 修改 AndroidManifest.xml**

`app/src/main/AndroidManifest.xml` 在 `<manifest>` 标签内的 `<application>` 标签上加 `android:name=".StarRailApp"`：

```xml
<application
    android:name=".StarRailApp"
    android:allowBackup="true"
    ...其他属性保持不变
/>
```

- [ ] **Step 3: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 写 Application 启动测试**

`app/src/test/java/com/java/myapplication/StarRailAppTest.kt`:
```kotlin
package com.java.myapplication

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StarRailAppTest {

    @Test fun `app seeds database on first launch`() = runTest {
        val app = ApplicationProvider.getApplicationContext<StarRailApp>()
        // 等待 import 完成
        kotlinx.coroutines.delay(500)
        assertThat(app.database.characterDao().count()).isEqualTo(5)
        assertThat(app.database.lightConeDao().count()).isEqualTo(5)
        assertThat(app.database.relicSetDao().count()).isEqualTo(3)
        assertThat(app.database.enemyDao().count()).isEqualTo(10)
        assertThat(app.database.scenarioDao().count()).isEqualTo(3)
        assertThat(app.database.eidolonDao().count()).isEqualTo(30)
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew testDebugUnitTest --no-daemon --tests "com.java.myapplication.StarRailAppTest" 2>&1 | tail -20`
Expected: `BUILD SUCCESSFUL` + test passes

- [ ] **Step 6: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/StarRailApp.kt
git add app/src/main/AndroidManifest.xml
git add app/src/test/java/com/java/myapplication/StarRailAppTest.kt
git commit -m "feat(app): StarRailApp seeds DB on first launch"
```

---

## Task 11: 完整编译 + 测试 + APK 验证

**Files:** 无

- [ ] **Step 1: 完整测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew test --no-daemon 2>&1 | tail -20`
Expected: 全部测试通过（至少 7 个：SeedImporter 4 + Repository 2 + App 1）

- [ ] **Step 2: 构建 Debug APK**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL` + APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: 最终 commit（若有未提交修改）**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git status
# 如果有未提交文件：
# git add -A && git commit -m "chore: final cleanup"
```

- [ ] **Step 4: 验收清单**

确认：
- [x] `./gradlew assembleDebug` 成功
- [x] `./gradlew test` 全绿
- [x] Room 中能查到 5 角色 / 5 光锥 / 3 遗器 / 10 敌人 / 3 场景 / 30 星魂
- [x] APK 生成成功

---

## 计划 01 完成标志

完成以上 11 个 Task 后：
- ✅ M1 完成（脚手架 + 依赖）
- ✅ M2 完成（数据模型 + Room + DAOs）
- ✅ M3 完成（种子数据导入 + 验证）

可以开始 **计划 02：战斗模拟器核心（M4~M7）**。

**预计剩余工作**：再写 5 份计划文档（02-06）+ 实施。每个计划大约 10-15 个 Task。