package com.channels.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsTest {

    @Test fun `short by shorts url path`() {
        assertTrue(Shorts.isShort(300, "https://www.youtube.com/shorts/abc123"))
    }

    @Test fun `short by duration under threshold`() {
        assertTrue(Shorts.isShort(45, "https://www.youtube.com/watch?v=abc123"))
    }

    @Test fun `exactly at threshold is a short`() {
        assertTrue(Shorts.isShort(Shorts.SHORTS_MAX_SECONDS, "https://youtu.be/abc"))
    }

    @Test fun `one second over threshold is long-form`() {
        assertFalse(Shorts.isShort(Shorts.SHORTS_MAX_SECONDS + 1, "https://youtu.be/abc"))
    }

    @Test fun `normal long-form video is not a short`() {
        assertFalse(Shorts.isShort(1800, "https://www.youtube.com/watch?v=abc123"))
    }

    @Test fun `zero or unknown duration is not a short (e g live)`() {
        assertFalse(Shorts.isShort(0, "https://www.youtube.com/watch?v=live"))
        assertFalse(Shorts.isShort(-1, "https://www.youtube.com/watch?v=live"))
    }

    @Test fun `null url falls back to duration`() {
        assertTrue(Shorts.isShort(30, null))
        assertFalse(Shorts.isShort(600, null))
    }
}
