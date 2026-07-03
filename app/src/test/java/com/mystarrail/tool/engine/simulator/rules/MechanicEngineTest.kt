package com.mystarrail.tool.engine.simulator.rules

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.buffs.BuffTarget
import com.mystarrail.tool.engine.simulator.sim.RoundEvent
import com.mystarrail.tool.engine.simulator.sim.toCombatant
import org.junit.Test

class MechanicEngineTest {
    private val engine = MechanicEngine()

    @Test fun `onActionStart does not throw on empty team`() {
        val c = sampleChar().toCombatant()
        val events = mutableListOf<RoundEvent>()
        engine.onActionStart(c, listOf(c), emptyList(), events)
    }

    @Test fun `ult charge increments after action end`() {
        val c = sampleChar().toCombatant()
        val initialCharge = c.ultCharge
        val events = mutableListOf<RoundEvent>()
        engine.onActionEnd(c, listOf(c), emptyList(), events)
        assertThat(c.ultCharge).isGreaterThan(initialCharge)
    }

    @Test fun `round end decays buff duration`() {
        val c = sampleChar().toCombatant()
        c.buffs.add(Buff.StatBoost("test", 2, StatType.ATK, 0.2))
        val events = mutableListOf<RoundEvent>()
        engine.onRoundEnd(listOf(c), emptyList(), events)
        assertThat(c.buffs.first().duration).isEqualTo(1)
    }

    @Test fun `round end removes expired buffs`() {
        val c = sampleChar().toCombatant()
        c.buffs.add(Buff.StatBoost("test", 1, StatType.ATK, 0.2))
        val events = mutableListOf<RoundEvent>()
        engine.onRoundEnd(listOf(c), emptyList(), events)
        assertThat(c.buffs).isEmpty()
    }

    @Test fun `cleanse rule removes debuffs on action start`() {
        val c = sampleChar().toCombatant()  // has CLEANSE tag
        c.debuffs.add(Buff.StatBoost("enemyAtk", 3, StatType.ATK, 0.5, BuffTarget.ENEMY))
        val events = mutableListOf<RoundEvent>()
        engine.onActionStart(c, listOf(c), emptyList(), events)
        assertThat(c.debuffs).isEmpty()
    }

    private fun sampleChar() = Character(
        id = "test", name = "Test", rarity = 5,
        path = Path.HUNT, element = Element.PHYSICAL, role = Role.DPS,
        tags = setOf(Tag.CLEANSE),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 1.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )
}