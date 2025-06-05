package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket

class AutoDisconnectModule : Module("AutoDisconnect", ModuleCategory.Misc) {

    private var hasDisconnected = false

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || hasDisconnected) return

        val playerHealth = session.localPlayer.health

        // Check if player's health is 4 or below
        if (playerHealth <= 4f) {
            val disconnectPacket = DisconnectPacket().apply {
                kickMessage = "Disconnected by §cWClient§r AutoDisconnect Module: Low Health"
            }
            session.clientBound(disconnectPacket)
            // Optionally also send to server if needed:
            // session.serverBound(disconnectPacket)

            hasDisconnected = true
            isEnabled = false
        }
    }
}
