package com.example.calorietracker.data.ai

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal object AiResponseParser {
    fun parse(content: String): AiResponse {
        val trimmed = stripCodeFence(content.trim())
        val jsonText = extractJson(trimmed) ?: return AiResponse(emptyList(), trimmed)
        val root = runCatching { JsonParser.parseString(jsonText) }.getOrNull()
            ?: return AiResponse(emptyList(), trimmed)

        return when {
            root.isJsonArray -> {
                val items = parseArray(root.asJsonArray)
                AiResponse(items, if (items.isEmpty()) trimmed else "已识别 ${items.size} 条记录")
            }
            root.isJsonObject -> parseObject(root.asJsonObject, trimmed)
            else -> AiResponse(emptyList(), trimmed)
        }
    }

    private fun parseObject(root: JsonObject, fallbackMessage: String): AiResponse {
        val message = root.stringValue("message", "summary", "reply", "text")
        val candidates = listOf(
            "items" to null,
            "records" to null,
            "foods" to "food",
            "exercises" to "exercise",
            "data" to null
        )

        for ((key, defaultType) in candidates) {
            val value = root.get(key) ?: continue
            when {
                value.isJsonArray -> {
                    val items = parseArray(value.asJsonArray, defaultType)
                    return AiResponse(items, resolvedMessage(message, items, fallbackMessage))
                }
                value.isJsonObject -> {
                    val nested = parseObject(value.asJsonObject, message ?: fallbackMessage)
                    if (nested.items.isNotEmpty()) return nested
                }
            }
        }

        val single = parseItem(root, null)
        val items = listOfNotNull(single)
        return AiResponse(items, resolvedMessage(message, items, fallbackMessage))
    }

    private fun resolvedMessage(
        explicitMessage: String?,
        items: List<AiResponseItem>,
        fallbackMessage: String
    ): String = explicitMessage
        ?: if (items.isNotEmpty()) "已识别 ${items.size} 条记录" else fallbackMessage

    private fun parseArray(array: JsonArray, defaultType: String? = null): List<AiResponseItem> =
        array.mapNotNull { element ->
            element.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let { parseItem(it, defaultType) }
        }

    private fun parseItem(item: JsonObject, defaultType: String?): AiResponseItem? {
        val name = item.stringValue("name", "foodName", "exerciseName", "title")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val rawType = item.stringValue("type", "category")?.lowercase()
        val type = when {
            rawType == "exercise" || rawType?.contains("运动") == true -> "exercise"
            rawType == "food" || rawType?.contains("食") == true -> "food"
            defaultType != null -> defaultType
            else -> "food"
        }
        val calories = item.doubleValue("calories", "kcal", "calorie", "energy")
            .takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

        return AiResponseItem(
            name = name,
            calories = calories,
            carbs = item.doubleValue("carbs", "carbohydrates", "carb").coerceAtLeast(0.0),
            protein = item.doubleValue("protein").coerceAtLeast(0.0),
            fat = item.doubleValue("fat").coerceAtLeast(0.0),
            type = type,
            time = item.stringValue("time"),
            notes = item.stringValue("notes", "note", "portion", "details")
        )
    }

    private fun JsonObject.stringValue(vararg keys: String): String? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (value.isJsonNull || !value.isJsonPrimitive) continue
            val text = runCatching { value.asString }.getOrNull()
            if (!text.isNullOrBlank()) return text
        }
        return null
    }

    private fun JsonObject.doubleValue(vararg keys: String): Double {
        for (key in keys) {
            val value = get(key) ?: continue
            if (value.isJsonNull || !value.isJsonPrimitive) continue
            val number = runCatching { value.asDouble }.getOrNull()
                ?: runCatching { value.asString.toDouble() }.getOrNull()
            if (number != null && number.isFinite()) return number
        }
        return 0.0
    }

    private fun stripCodeFence(value: String): String {
        if (!value.startsWith("```")) return value
        val firstLineEnd = value.indexOf('\n')
        val withoutStart = if (firstLineEnd >= 0) value.substring(firstLineEnd + 1) else value
        val end = withoutStart.lastIndexOf("```")
        return (if (end >= 0) withoutStart.substring(0, end) else withoutStart).trim()
    }

    private fun extractJson(value: String): String? {
        val objectStart = value.indexOf('{').takeIf { it >= 0 }
        val arrayStart = value.indexOf('[').takeIf { it >= 0 }
        val start = listOfNotNull(objectStart, arrayStart).minOrNull() ?: return null
        val close = if (value[start] == '{') '}' else ']'
        val end = value.lastIndexOf(close)
        return if (end >= start) value.substring(start, end + 1) else null
    }
}
