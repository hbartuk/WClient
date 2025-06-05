package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import kotlin.math.cos
import kotlin.math.sin

class TrollerModule : Module("Troller", ModuleCategory.Combat) {

    private var range by floatValue("Range", 4.0f, 2f..6f)
    private var hitSpeed by intValue("Hit Speed", 200, 100..1000)
    private var jumpHeight by floatValue("Jump Height", 0.42f, 0.1f..1f)
    private var circleRadius by floatValue("Radium", 1.5f, 0.5f..3f)
    private var playersOnly by boolValue("players_only", true)
    private var cooldown by intValue("Cooldown", 1000, 500..5000)
    private var smartTargeting by boolValue("Smart Target", true)

    private var lastHitTime = 0L
    private var comboAngle = 0f
    private var targetCooldowns = mutableMapOf<Entity, Long>()
    private var combatStatistics = CombatStatistics()

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()

            val nearbyEntities = session.level.entityMap.values.filter {
                it.distance(session.localPlayer) <= range &&
                        it != session.localPlayer &&
                        isValidTarget(it) &&
                        (targetCooldowns[it] ?: 0L) <= currentTime
            }

            if (nearbyEntities.isNotEmpty() && currentTime - lastHitTime >= hitSpeed) {
                val target = if (smartTargeting) prioritizeTarget(nearbyEntities) else nearbyEntities.first()
                executeCombo(target)
                lastHitTime = currentTime
                targetCooldowns[target] = currentTime + cooldown
            }
        }
    }

    private fun isValidTarget(entity: Entity): Boolean {
        return when (entity) {
            is LocalPlayer -> false
            is Player -> {
                if (playersOnly) {
                    !isBot(entity)
                } else {
                    false
                }
            }
            else -> !playersOnly
        }
    }

    private fun isBot(player: Player): Boolean {
        if (player is LocalPlayer) return false
        val playerList = session.level.playerMap[player.uuid] ?: return true
        return playerList.name.isBlank()
    }

    private fun prioritizeTarget(entities: List<Entity>): Entity {
        // Prioritize players over other entities
        return entities.sortedByDescending { it is Player }.first()
    }

    private fun executeCombo(target: Entity) {
        comboAngle += 45f
        if (comboAngle >= 360f) comboAngle = 0f

        val offsetX = circleRadius * cos(Math.toRadians(comboAngle.toDouble())).toFloat()
        val offsetZ = circleRadius * sin(Math.toRadians(comboAngle.toDouble())).toFloat()

        session.clientBound(SetEntityMotionPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            motion = Vector3f.from(offsetX, jumpHeight, offsetZ)
        })

        session.localPlayer.attack(target)
        combatStatistics.recordHit(target)
    }

    private inner class CombatStatistics {
        private var hits = 0
        private var misses = 0
        private var damageDealt = 0f

        fun recordHit(target: Entity) {
            hits++
            damageDealt += calculateDamage(target)
        }

        private fun calculateDamage(target: Entity): Float {
            // Implement damage calculation logic
            return 10f // Example fixed damage value
        }
    }
}
