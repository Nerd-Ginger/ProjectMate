package com.nerdginger.projectmate.core

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildInfoTest {
    @Test
    fun `schema version is one`() {
        assertEquals(1, BuildInfo.SCHEMA_VERSION)
    }
}
