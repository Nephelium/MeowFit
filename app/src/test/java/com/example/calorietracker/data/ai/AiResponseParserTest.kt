package com.example.calorietracker.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseParserTest {
    @Test
    fun parsesMarkdownWrappedJsonAndNumericStrings() {
        val result = AiResponseParser.parse(
            """```json
                {"message":"已整理","items":[{"name":"鸡胸肉","calories":"165","carbs":0,"protein":"31","fat":3.6,"type":"food"}]}
                ```""".trimIndent()
        )

        assertEquals("已整理", result.summary)
        assertEquals(1, result.items.size)
        assertEquals(165.0, result.items.single().calories, 0.001)
        assertEquals(31.0, result.items.single().protein, 0.001)
    }

    @Test
    fun acceptsCommonAliasWithoutLosingCards() {
        val result = AiResponseParser.parse(
            """结果如下：{"reply":"可以添加","records":[{"foodName":"燕麦","kcal":180,"carbohydrates":30}]}"""
        )

        assertEquals("可以添加", result.summary)
        assertEquals("燕麦", result.items.single().name)
        assertEquals("food", result.items.single().type)
    }

    @Test
    fun keepsPlainTextAsConversationMessage() {
        val result = AiResponseParser.parse("请告诉我食物重量")
        assertTrue(result.items.isEmpty())
        assertEquals("请告诉我食物重量", result.summary)
    }

    @Test
    fun replacesRawJsonWithReadableFallbackWhenMessageIsMissing() {
        val result = AiResponseParser.parse(
            "识别完成：{\"items\":[{\"name\":\"酸奶\",\"calories\":98}]}"
        )

        assertEquals("已识别 1 条记录", result.summary)
        assertEquals("酸奶", result.items.single().name)
    }
}
