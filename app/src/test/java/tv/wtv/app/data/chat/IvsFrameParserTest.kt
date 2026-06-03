package tv.wtv.app.data.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tv.wtv.app.data.model.ChatMessage

class IvsFrameParserTest {

    private fun frame(
        type: String = "EVENT",
        eventName: String = "MESSAGE",
        messageId: String = "msg-1",
        content: String = "hello",
        senderUserId: String = "user-1",
        senderNickname: String = "Alice",
    ): String {
        val data = """{"messageId":"$messageId","content":"$content","sender":{"userId":"$senderUserId","nickname":"$senderNickname","tags":[]}}"""
        val escapedData = data.replace("\"", "\\\"")
        return """{"Type":"$type","EventName":"$eventName","Attributes":{"data":"$escapedData"},"SendTime":"2026-01-01T00:00:00Z"}"""
    }

    @Test
    fun `valid MESSAGE event returns ChatMessage`() {
        assertEquals(ChatMessage("msg-1", "Alice", "hello"), parseIvsFrame(frame()))
    }

    @Test
    fun `non-EVENT Type returns null`() {
        assertNull(parseIvsFrame(frame(type = "UNKNOWN")))
    }

    @Test
    fun `non-MESSAGE EventName returns null`() {
        assertNull(parseIvsFrame(frame(eventName = "VIEWER_JOIN")))
    }

    @Test
    fun `missing Attributes data returns null`() {
        assertNull(parseIvsFrame("""{"Type":"EVENT","EventName":"MESSAGE","Attributes":{}}"""))
    }

    @Test
    fun `missing messageId in data returns null`() {
        val data = """{"content":"hi","sender":{"userId":"u1","nickname":"Bob","tags":[]}}"""
        val escaped = data.replace("\"", "\\\"")
        assertNull(parseIvsFrame("""{"Type":"EVENT","EventName":"MESSAGE","Attributes":{"data":"$escaped"}}"""))
    }

    @Test
    fun `missing sender in data returns null`() {
        val data = """{"messageId":"m1","content":"hi"}"""
        val escaped = data.replace("\"", "\\\"")
        assertNull(parseIvsFrame("""{"Type":"EVENT","EventName":"MESSAGE","Attributes":{"data":"$escaped"}}"""))
    }

    @Test
    fun `missing nickname in sender returns null`() {
        val data = """{"messageId":"m1","content":"hi","sender":{"userId":"u1","tags":[]}}"""
        val escaped = data.replace("\"", "\\\"")
        assertNull(parseIvsFrame("""{"Type":"EVENT","EventName":"MESSAGE","Attributes":{"data":"$escaped"}}"""))
    }

    @Test
    fun `invalid JSON returns null`() {
        assertNull(parseIvsFrame("not-json"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(parseIvsFrame(""))
    }

    @Test
    fun `content with special characters is preserved`() {
        val result = parseIvsFrame(frame(content = "PogChamp LUL Kappa"))
        assertEquals("PogChamp LUL Kappa", result?.content)
    }

    @Test
    fun `cyrillic content is preserved`() {
        val result = parseIvsFrame(frame(content = "вау"))
        assertEquals("вау", result?.content)
    }
}