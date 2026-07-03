package com.mystarrail.tool.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CycleProfile
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.Tag

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