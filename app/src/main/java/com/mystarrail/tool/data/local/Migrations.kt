package com.mystarrail.tool.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS skill_tree_nodes (
                id TEXT NOT NULL PRIMARY KEY,
                characterId TEXT NOT NULL,
                name TEXT NOT NULL,
                desc TEXT NOT NULL,
                maxLevel INTEGER NOT NULL,
                skillType TEXT,
                effectType TEXT,
                paramListJson TEXT NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_tree_nodes_characterId ON skill_tree_nodes(characterId)")
    }
}