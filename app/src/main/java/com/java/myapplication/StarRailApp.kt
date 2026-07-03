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
            if (database.characterDao().count() == 0) {
                seedImporter.importFromAssets()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}