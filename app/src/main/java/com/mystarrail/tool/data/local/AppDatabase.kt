package com.mystarrail.tool.data.local

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
        PlayerBuildEntity::class,
        SkillTreeNodeEntity::class
    ],
    version = 2,
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
    abstract fun skillTreeDao(): SkillTreeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "starrail.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
