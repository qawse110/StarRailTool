package com.mystarrail.tool.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.SkillType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(
    tableName = "skill_tree_nodes",
    foreignKeys = [ForeignKey(
        entity = CharacterEntity::class,
        parentColumns = ["id"],
        childColumns = ["characterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("characterId")]
)
data class SkillTreeNodeEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String,
    val desc: String,
    val maxLevel: Int,
    val skillType: String?,
    val effectType: String?,
    val paramListJson: String,
    val position: Int
) {
    fun toModel(): SkillTreeNode = SkillTreeNode(
        id = id, name = name, desc = desc, maxLevel = maxLevel,
        skillType = skillType?.let { runCatching { SkillType.valueOf(it) }.getOrNull() },
        effectType = effectType,
        paramList = runCatching {
            Json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), paramListJson)
        }.getOrDefault(emptyList())
    )

    companion object {
        fun fromModel(characterId: String, node: SkillTreeNode, position: Int): SkillTreeNodeEntity = SkillTreeNodeEntity(
            id = node.id,
            characterId = characterId,
            name = node.name,
            desc = node.desc,
            maxLevel = node.maxLevel,
            skillType = node.skillType?.name,
            effectType = node.effectType,
            paramListJson = runCatching {
                Json.encodeToString(ListSerializer(ListSerializer(Double.serializer())), node.paramList)
            }.getOrDefault("[]"),
            position = position
        )
    }
}