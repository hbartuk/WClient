package com.retrivedmods.wclient.game.inventory

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket

class ContainerInventory(containerId: Int, val type: ContainerType) :
    AbstractInventory(containerId) {

    override var content = emptyArray<ItemData>()

    fun onPacketBound(packet: BedrockPacket) {
        if (packet is InventoryTransactionPacket) {
            packet.actions.filter {
                it is InventoryActionData &&
                        it.source.type == InventorySource.Type.CONTAINER &&
                        it.source.containerId == containerId
            }.forEach {
                content[it.slot] = it.toItem
            }
        }
        if (packet is InventoryContentPacket && packet.containerId == containerId) {
            content = packet.contents.toTypedArray()
        } else if (packet is InventorySlotPacket && packet.containerId == containerId) {
            content[packet.slot] = packet.item
        }
    }

    }
    override var content: Array<ItemData> = when (type) {
        ContainerType.CONTAINER -> Array(27) { ItemData.AIR }
        ContainerType.HOPPER -> Array(5) { ItemData.AIR }
        ContainerType.DISPENSER -> Array(9) { ItemData.AIR }
        ContainerType.FURNACE -> Array(3) { ItemData.AIR }
        else -> Array(0) { ItemData.AIR }
    }

    fun onPacketBound(packet: BedrockPacket) {
        when (packet) {
            is InventoryTransactionPacket -> {
                packet.actions.filter {
                    it.source.type == InventorySource.Type.CONTAINER &&
                            it.source.containerId == containerId
                }.forEach {
                    content[it.slot] = it.toItem
                }
            }

            is InventoryContentPacket -> {
                if (packet.containerId == containerId) {
                    content = packet.contents.toTypedArray()
                }
            }

            is InventorySlotPacket -> {
                if (packet.containerId == containerId) {
                    content[packet.slot] = packet.item
                }
            }
        }
    }
}
