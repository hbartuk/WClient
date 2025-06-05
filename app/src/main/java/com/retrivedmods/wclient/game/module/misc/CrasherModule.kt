package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.packet.SubChunkRequestPacket
import kotlin.random.Random

class CrasherModule : Module("crash", ModuleCategory.Misc) {
    private val packetCount = 50
    private val lagRadius = 10

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        } else {
            val player = session.player
            val playerPos = player?.blockPosition ?: Vector3i.ZERO

            repeat(packetCount) {
                val randomPos = randomNearby(playerPos, lagRadius)
                val subChunkRequestPacket = SubChunkRequestPacket().apply {
                    dimension = 0
                    subChunkPosition = randomPos
                    positionOffsets = mutableListOf(randomPos)
                }
                session.clientBound(subChunkRequestPacket)
            }
        }
    }

    private fun randomNearby(center: Vector3i, radius: Int): Vector3i {
        val dx = Random.nextInt(-radius, radius + 1)
        val dz = Random.nextInt(-radius, radius + 1)
        // Y оставляем как у игрока
        return Vector3i.from(center.x() + dx, center.y(), center.z() + dz)
    }
}
