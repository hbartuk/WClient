package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sqrt

// Модуль для отслеживания игроков рядом с вами
class PlayerTracerModule : Module("player_tracer", ModuleCategory.Misc) {

    // Сохраняет информацию об игроках по их entityId
    private val playersInfo = mutableMapOf<Long, PlayerInfo>()
    // Позиция вашего игрока
    private var playerPosition = Vector3f.from(0f, 0f, 0f)
    // Предыдущие позиции и время для расчёта скорости
    private val previousPositions = mutableMapOf<Long, Vector3f>()
    private val previousTimestamps = mutableMapOf<Long, Long>()
    // Радиус сканирования (в блоках)
    private val scanRadius = intValue("scanRadius", 500, 100..100000)

    // Класс для хранения информации об игроке
    data class PlayerInfo(
        val entityId: Long,
        val name: String,
        val xuid: String,
        val platformChatId: String,
        val buildPlatform: Int,
        val skin: SerializedSkin
    )

    // Рассчитывает скорость игрока
    private fun calculateVelocity(
        entityId: Long,
        currentPosition: Vector3f,
        currentTime: Long
    ): Vector3f? {
        val previousPosition = previousPositions[entityId]
        val previousTimestamp = previousTimestamps[entityId]

        return if (previousPosition != null && previousTimestamp != null) {
            val timeDelta = currentTime - previousTimestamp
            if (timeDelta > 0) {
                Vector3f.from(
                    (currentPosition.x - previousPosition.x) / timeDelta,
                    (currentPosition.y - previousPosition.y) / timeDelta,
                    (currentPosition.z - previousPosition.z) / timeDelta
                )
            } else null
        } else null
    }

    // Обновляет последнюю известную позицию и время для игрока
    private fun updatePositionAndTimestamp(entityId: Long, currentPosition: Vector3f) {
        val currentTime = System.currentTimeMillis()
        previousPositions[entityId] = currentPosition
        previousTimestamps[entityId] = currentTime
    }

    // Отправляет сообщение с подробной информацией о игроке
    private fun sendMessage(
        playerInfo: PlayerInfo,
        entityPosition: Vector3f,
        distance: Float,
        direction: String,
        velocity: Vector3f?,
        actionState: String?
    ) {
        val lastKnownPosition =
            previousPositions[playerInfo.entityId]?.roundUpCoordinates() ?: "N/A"

        val textPacket = TextPacket().apply {
            type = TextPacket.Type.RAW
            isNeedsTranslation = false
            message = """
        §l§b[CutieAI]§r §eГеймертаг игрока: §a${playerInfo.name} §e| §eEntity ID: §c${playerInfo.entityId} §e| §eПозиция: §a${entityPosition.roundUpCoordinates()} §e| §eДистанция: §c${
                ceil(distance)
            } §e| §eНаправление: §d$direction
        §l§b[CutieAI]§r §7Дополнительно: §fXbox UID: §7${playerInfo.xuid} §e| §7Разница по высоте: §f${
                ceil(entityPosition.y - playerPosition.y)
            } блоков §e| §7Последняя известная позиция: §f$lastKnownPosition
    """.trimIndent()
            xuid = ""
            sourceName = ""
        }
        session.clientBound(textPacket)
    }

    // Основная обработка входящих пакетов
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        // Обрабатывает список игроков
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                playersInfo[entry.entityId] = PlayerInfo(
                    entityId = entry.entityId,
                    name = entry.name,
                    xuid = entry.xuid,
                    platformChatId = entry.platformChatId,
                    buildPlatform = entry.buildPlatform,
                    skin = entry.skin
                )
            }
        }

        // Получает позицию вашего игрока
        if (packet is PlayerAuthInputPacket) {
            playerPosition = packet.position
        }

        // Получает позицию других игроков
        if (packet is MoveEntityAbsolutePacket) {
            val entityId = packet.runtimeEntityId
            val entityPosition = packet.position
            val currentTime = System.currentTimeMillis()

            updatePositionAndTimestamp(entityId, entityPosition)
            val velocity = calculateVelocity(entityId, entityPosition, currentTime)
            val storedPlayerInfo = playersInfo[entityId]
            if (storedPlayerInfo != null) {
                val distance = calculateDistance(playerPosition, entityPosition)
                if (distance <= scanRadius.value.toFloat()) {
                    val direction = getCompassDirection(playerPosition, entityPosition)
                    sendMessage(
                        storedPlayerInfo,
                        entityPosition,
                        distance,
                        direction,
                        velocity,
                        null
                    )
                }
            }
        }
    }

    // Вычисляет расстояние между двумя точками
    private fun calculateDistance(from: Vector3f, to: Vector3f): Float {
        val dx = from.x - to.x
        val dy = from.y - to.y
        val dz = from.z - to.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    // Округляет координаты вверх и возвращает строку
    private fun Vector3f.roundUpCoordinates(): String {
        val roundedX = ceil(this.x).toInt()
        val roundedY = ceil(this.y).toInt()
        val roundedZ = ceil(this.z).toInt()
        return "$roundedX, $roundedY, $roundedZ"
    }

    // Определяет направление до игрока (16 направлений компаса)
    private fun getCompassDirection(from: Vector3f, to: Vector3f): String {
        val dx = to.x - from.x
        val dz = to.z - from.z
        val angle = (atan2(dz, dx) * (180 / PI) + 360) % 360
        return when {
            angle >= 348.75 || angle < 11.25 -> "С"
            angle >= 11.25 && angle < 33.75 -> "ССВ"
            angle >= 33.75 && angle < 56.25 -> "СВ"
            angle >= 56.25 && angle < 78.75 -> "ВСВ"
            angle >= 78.75 && angle < 101.25 -> "В"
            angle >= 101.25 && angle < 123.75 -> "ВЮВ"
            angle >= 123.75 && angle < 146.25 -> "ЮВ"
            angle >= 146.25 && angle < 168.75 -> "ЮЮВ"
            angle >= 168.75 && angle < 191.25 -> "Ю"
            angle >= 191.25 && angle < 213.75 -> "ЮЮЗ"
            angle >= 213.75 && angle < 236.25 -> "ЮЗ"
            angle >= 236.25 && angle < 258.75 -> "ЗЮЗ"
            angle >= 258.75 && angle < 281.25 -> "З"
            angle >= 281.25 && angle < 303.75 -> "ЗСЗ"
            angle >= 303.75 && angle < 326.25 -> "СЗ"
            else -> "ССЗ"
        }
    }
}
