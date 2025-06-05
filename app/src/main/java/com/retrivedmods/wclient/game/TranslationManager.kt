package com.retrivedmods.wclient.game

object TranslationManager {

    private val map = HashMap<String, Map<String, String>>()

    init {
        map["en"] = ru() // Теперь и английская локализация на русском
        map["zh"] = ru() // И китайская на русском, если нужно оставить только русский везде
    }

    // Русские переводы для всех ключей
    private fun ru() = buildMap {
        put("fly", "Полёт")
        put("no_clip", "Прохождение сквозь блоки")
        put("zoom", "Увеличение")
        put("air_jump", "Прыжок в воздухе")
        put("speed", "Скорость")
        put("full_bright", "Максимальная яркость")
        put("haste", "Ускорение")
        put("jetpack", "Реактивный ранец")
        put("levitation", "Левитация")
        put("high_jump", "Высокий прыжок")
        put("slow_falling", "Медленное падение")
        put("anti_knockback", "Без отдачи")
        put("poseidon", "Посейдон")
        put("regeneration", "Регенерация")
        put("bhop", "БХОП")
        put("sprint", "Бег")
        put("no_hurt_camera", "Без тряски камеры")
        put("anti_afk", "Анти-АФК")
        put("auto_walk", "Автоходьба")
        put("desync", "Десинхронизация")
        put("position_logger", "Логгер позиций")
        put("killaura", "Киллаура")
        put("motion_fly", "Движущийся полёт")
        put("free_camera", "Свободная камера")
        put("player_tracer", "Трекер игроков")
        put("critic", "Криты")
        put("nausea", "Тошнота")
        put("health_boost", "Увеличение здоровья")
        put("jump_boost", "Увеличение прыжка")
        put("resistance", "Сопротивление")
        put("fire_resist", "Огнестойкость")
        put("swiftness", "Быстрота")
        put("instant_health", "Мгновенное здоровье")
        put("strength", "Сила")
        put("instant_damage", "Мгновенный урон")
        put("anti_crystal", "Анти-кристалл")
        put("bad_omen", "Плохое предзнаменование")
        put("conduit_power", "Сила проводника")
        put("darkness", "Тьма")
        put("fatal_poison", "Смертельный яд")
        put("hunger", "Голод")
        put("poison", "Яд")
        put("village_omen", "Герой деревни")
        put("weakness", "Слабость")
        put("wither", "Иссушение")
        put("night_vision", "Ночное зрение")
        put("invisibility", "Невидимость")
        put("saturation", "Насыщение")
        put("absorption", "Поглощение")
        put("blindness", "Слепота")
        put("time_shift", "Изменение времени")
        put("weather_controller", "Контроллер погоды")
        put("crash", "Краш")

        // Опции модулей
        put("times", "Количество")
        put("flySpeed", "Скорость полёта")
        put("range", "Радиус")
        put("cps", "Кликов в секунду")
        put("amplifier", "Усиление")
        put("nightVision", "Ночное зрение")
        put("scanRadius", "Радиус сканирования")
        put("jumpHeight", "Высота прыжка")
        put("verticalUpSpeed", "Скорость вверх")
        put("verticalDownSpeed", "Скорость вниз")
        put("motionInterval", "Интервал движения")
        put("glideSpeed", "Скорость скольжения")
        put("vanillaFly", "Обычный полёт")
        put("repeat", "Повтор")
        put("delay", "Задержка")
        put("enabled", "Включено")
        put("disabled", "Выключено")
        put("players_only", "Только игроки")
        put("mobs_only", "Аура мобов")
        put("time", "Время")
        put("keep_distance", "Дистанция")
        put("tp_speed", "Скорость телепорта")
        put("packets", "Пакеты")
        put("strafe", "Стрейф")
        put("tp_aura", "Аура телепорта")
        put("teleport_behind", "Телепорт за спину")
        put("strafe_angle", "Угол стрейфа")
        put("strafe_speed", "Скорость стрейфа")
        put("strafe_radius", "Радиус стрейфа")
        put("clear", "Ясно")
        put("rain", "Дождь")
        put("thunderstorm", "Гроза")
        put("intensity", "Интенсивность")
        put("interval", "Интервал")
    }

    fun getTranslationMap(language: String): Map<String, String> {
        // Всегда возвращаем только русскую карту
        return map["en"]!!
    }

}
