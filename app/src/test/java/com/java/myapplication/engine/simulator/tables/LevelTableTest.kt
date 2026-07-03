package com.java.myapplication.engine.simulator.tables

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LevelTableTest {
    private val table = LevelTable()

    @Test fun `same level gives 1_0`() {
        assertThat(table.suppression(80, 80)).isEqualTo(1.0)
    }

    @Test fun `attacker 5 levels higher gives +10 percent`() {
        assertThat(table.suppression(85, 80)).isEqualTo(1.10)
    }

    @Test fun `attacker 10 levels higher capped at +10 percent`() {
        assertThat(table.suppression(90, 80)).isEqualTo(1.10)
    }

    @Test fun `attacker 5 levels lower gives -10 percent`() {
        assertThat(table.suppression(75, 80)).isEqualTo(0.90)
    }

    @Test fun `attacker 1 level higher gives +2 percent`() {
        assertThat(table.suppression(81, 80)).isEqualTo(1.02)
    }
}