package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.random.Random

class AdvanceDisablerModule : Module("AdvancedDisabler", ModuleCategory.Misc) {

    private val lifeboatBypass by boolValue("Lifeboat Bypass", true)
    private val hiveBypass by boolValue("Hive Bypass", true)
    private val cubecraftBypass by boolValue("Cubecraft Bypass", true)
    private val spoofTeleportLag by boolValue("TP Lag Spoof", true)
    private val packetFlood by boolValue("Packet Flood", false)
    private val floodRate by intValue("Flood Rate", 5, 1..20)
    private val antiKick by boolValue("Anti Kick", true)
    private val blinkPackets by boolValue("Blink Mode", false)

    private var packetCounter = 0
    private var blinkBuffer = mutableListOf<PlayerAuthInputPacket>()

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet

        if (packet is MovePlayerPacket) {

            if (lifeboatBypass || hiveBypass || cubecraftBypass) {
                if (Random.Default.nextInt(4) == 0) {
                    packet.isOnGround = true
                    packet.position = packet.position.add(0f, 0.015f + Random.Default.nextFloat() * 0.01f, 0f)
                }
            }

            if (spoofTeleportLag && Random.Default.nextInt(3) == 0) {
                try {
                    val field = packet.javaClass.getDeclaredField("teleportationCause")
                    field.isAccessible = true
                    field.set(packet, 0)
                } catch (_: Exception) {
                }
            }

            if (antiKick && packet.position.y <= 0.5f) {
                packet.position = packet.position.add(0f, 1.5f + Random.Default.nextFloat(), 0f)
                packet.isOnGround = true
            }
        }

        if (packet is PlayerAuthInputPacket) {

            if (blinkPackets) {
                blinkBuffer.add(packet)
                if (blinkBuffer.size >= 10 + Random.Default.nextInt(5)) {
                    blinkBuffer.forEach { spoof ->
                        session.clientBound(spoof)
                    }
                    blinkBuffer.clear()
                }
                return
            }

            if (packetFlood) {
                packetCounter++
                if (packetCounter >= floodRate) {
                    packetCounter = 0
                    repeat(3) {
                        val spoof = PlayerAuthInputPacket().apply {
                            position = packet.position
                            delta = packet.delta
                            inputMode = packet.inputMode
                            tick = packet.tick + Random.Default.nextInt(1, 6)
                        }
                        session.clientBound(spoof)
                    }
                }
            }

            if (Random.Default.nextBoolean()) {
                packet.delta = packet.delta.add(0.001f * (Random.Default.nextFloat() - 0.5f),
                    0f,
                    0.001f * (Random.Default.nextFloat() - 0.5f))
            }
        }
    }

    fun onDisable() {
        blinkBuffer.forEach {
            session.clientBound(it)
        }
        blinkBuffer.clear()
    }
}
