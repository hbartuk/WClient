package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.*
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class WAuraModule : Module("WAura", ModuleCategory.Combat) {

    private var playersOnly by boolValue("players_only", true)
    private var mobsOnly by boolValue("mobs_only", false)

    private var rangeValue by floatValue("range", 50f, 2f..50f)
    private var cpsValue by intValue("cps", 25, 1..50)
    private var boost by intValue("packets", 2, 1..10)

    private var lastAttackNanoTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentNanoTime = System.nanoTime()
            val minAttackDelay = 1_000_000_000L / cpsValue

            if ((currentNanoTime - lastAttackNanoTime) >= minAttackDelay) {
                val targets = searchForTargets()
                if (targets.isEmpty()) return

                val player = session.localPlayer
                for (entity in targets) {
                    repeat(boost) {
                        player.attack(entity)
                    }
                    lastAttackNanoTime = currentNanoTime
                }
            }
        }
    }

    private fun searchForTargets(): List<Entity> {
        return session.level.entityMap.values
            .filter { it.isTarget() && it.distance(session.localPlayer) <= rangeValue }
            .sortedBy { it.distance(session.localPlayer) } // Closest first
    }

    private fun Entity.isTarget(): Boolean {
        return when (this) {
            is LocalPlayer -> false
            is Player -> {
                if (mobsOnly) false else !this.isBot()
            }
            is EntityUnknown -> {
                if (mobsOnly) this.identifier in MobList.mobTypes
                else if (playersOnly) false
                else true
            }
            else -> false
        }
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        val playerData = session.level.playerMap[this.uuid]
        return playerData?.name.isNullOrBlank()
    }
}
