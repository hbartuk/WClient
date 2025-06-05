package com.retrivedmods.wclient.game.module.visual

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import com.retrivedmods.wclient.game.entity.Player

class DamageTextModule : Module("DamageText", ModuleCategory.Visual) {

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        if (packet is EntityEventPacket && packet.type == EntityEventType.HURT) {
            val entityId = packet.runtimeEntityId

            // Не показываем сообщение о своем уроне
            if (entityId == session.localPlayer.runtimeEntityId) return

            // Получаем сущность по id
            val entity = session.level.entityMap[entityId]

            // Если это игрок — выводим сообщение в чат
            if (entity is Player) {
                val playerName = entity.username
                val stateText = "$playerName был(а) ранен!"
                val status = "§c$stateText"
                val message = "§l§c[WClient] §8»§r $status"
                session.displayClientMessage(message)
            }
        }
    }
}
