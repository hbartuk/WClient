package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.*
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.cos
import kotlin.math.sin

class KillauraModule : Module("killaura", ModuleCategory.Combat) {

    private var playersOnly by boolValue("players_only", true)
    private var mobsOnly by boolValue("mobs_only", false)
    private var tpAuraEnabled by boolValue("tp_aura", true)
    private var teleportBehind by boolValue("tp_behind", true)
    private var criticalHits by boolValue("critical_hit", true)
    private var strafe by boolValue("strafe", false)

    private var rangeValue by floatValue("range", 9.5f, 2f..16f)
    private var cpsValue by intValue("cps", 20, 10..25)
    private var tpSpeed by intValue("tp_speed", 100, 10..500)
    private var tpYOffset by intValue("tp_y_offset", 1, -10..10)
    private var distanceToKeep by floatValue("keep_distance", 1.0f, 0.5f..5f)

    private var strafeAngle = 0.0f
    private val strafeSpeed by floatValue("strafe_speed", 2.5f, 1f..4f)
    private val strafeRadius by floatValue("strafe_radius", 2.5f, 1f..6f)

    private var lastAttackTime = 0L
    private var tpCooldown = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val now = System.currentTimeMillis()
            val delay = 1000L / cpsValue

            if ((now - lastAttackTime) >= delay) {
                val targets = searchForTargets()
                if (targets.isEmpty()) return

                for (target in targets) {
                    if (tpAuraEnabled && now - tpCooldown >= tpSpeed) {
                        teleportTo(target, distanceToKeep, tpYOffset)
                        tpCooldown = now
                    }

                    if (criticalHits) triggerCriticalHit()

                    session.localPlayer.attack(target)

                    if (strafe) strafeAroundTarget(target)

                    lastAttackTime = now
                }
            }
        }
    }

    private fun teleportTo(entity: Entity, keepDistance: Float, yOffset: Int) {
        val pos = entity.vec3Position
        val player = session.localPlayer.vec3Position

        val targetYawRad = Math.toRadians(entity.vec3Rotation.y.toDouble()).toFloat()
        val behindDir = Vector3f.from(
            sin(targetYawRad),
            0f,
            -cos(targetYawRad)
        ).normalize()

        val tpPos = if (teleportBehind) {
            Vector3f.from(
                pos.x + behindDir.x * keepDistance,
                pos.y + yOffset,
                pos.z + behindDir.z * keepDistance
            )
        } else {
            val direct = Vector3f.from(pos.x - player.x, 0f, pos.z - player.z).normalize()
            Vector3f.from(
                pos.x - direct.x * keepDistance,
                pos.y + yOffset,
                pos.z - direct.z * keepDistance
            )
        }

        val movePacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = tpPos
            rotation = entity.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = false
            tick = session.localPlayer.tickExists
        }

        session.clientBound(movePacket)
    }

    private fun strafeAroundTarget(entity: Entity) {
        val pos = entity.vec3Position
        strafeAngle += strafeSpeed
        if (strafeAngle >= 360f) strafeAngle -= 360f

        val offsetX = strafeRadius * cos(strafeAngle)
        val offsetZ = strafeRadius * sin(strafeAngle)

        val newPos = pos.add(offsetX.toFloat(), 0f, offsetZ.toFloat())

        val strafePacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = newPos
            rotation = Vector3f.ZERO
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            tick = session.localPlayer.tickExists
        }

        session.clientBound(strafePacket)
    }

    private fun triggerCriticalHit() {
        val currentY = session.localPlayer.vec3Position.y
        val upPacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = session.localPlayer.vec3Position.add(0f, 0.1f, 0f)
            rotation = session.localPlayer.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = false
            tick = session.localPlayer.tickExists
        }
        session.clientBound(upPacket)
    }

    private fun searchForTargets(): List<Entity> {
        return session.level.entityMap.values.filter {
            it.distance(session.localPlayer) < rangeValue && it.isTarget()
        }
    }

    private fun Entity.isTarget(): Boolean {
        return when (this) {
            is LocalPlayer -> false
            is Player -> playersOnly && !isBot()
            is EntityUnknown -> mobsOnly && isMob()
            else -> false
        }
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        return session.level.playerMap[this.uuid]?.name.isNullOrBlank()
    }

    private fun EntityUnknown.isMob(): Boolean {
        return this.identifier in MobList.mobTypes
    }
}
