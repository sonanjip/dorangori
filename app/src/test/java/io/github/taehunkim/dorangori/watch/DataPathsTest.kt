package io.github.taehunkim.dorangori.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPathsTest {
    @Test fun acceptsOurRecordingNames() {
        assertEquals("dorangori_20261113_140709.m4a", DataPaths.nameOf("/rec/dorangori_20261113_140709.m4a"))
    }

    @Test fun rejectsOtherPathsAndTraversal() {
        assertNull(DataPaths.nameOf("/other/dorangori_20261113_140709.m4a"))
        assertNull(DataPaths.nameOf("/rec/../x.m4a"))
        assertNull(DataPaths.nameOf("/rec/dorangori_20261113_140709.m4a/../../x"))
        assertNull(DataPaths.nameOf("/rec/"))
        assertFalse(DataPaths.isValidName("dorangori_20261113_140709.m4a.part"))
        assertTrue(DataPaths.isValidName("dorangori_20261113_140709.m4a"))
    }
}
