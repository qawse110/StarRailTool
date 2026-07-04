package com.mystarrail.tool.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val conv = Converters()

    @Test fun `stringSet round-trip preserves elements`() {
        val original = setOf("summon", "dot", "action_advance")
        val serialized = conv.fromStringSet(original)
        val deserialized = conv.toStringSet(serialized)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test fun `stringSet empty round-trip`() {
        val original = emptySet<String>()
        val serialized = conv.fromStringSet(original)
        val deserialized = conv.toStringSet(serialized)
        assertThat(deserialized).isEmpty()
    }

    @Test fun `stringList round-trip preserves elements`() {
        val original = listOf("enemy_001", "enemy_002", "enemy_003")
        val serialized = conv.fromStringList(original)
        val deserialized = conv.toStringList(serialized)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test fun `stringList empty round-trip`() {
        val original = emptyList<String>()
        val serialized = conv.fromStringList(original)
        val deserialized = conv.toStringList(serialized)
        assertThat(deserialized).isEmpty()
    }
}
