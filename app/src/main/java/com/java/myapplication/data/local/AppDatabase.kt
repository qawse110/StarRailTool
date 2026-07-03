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