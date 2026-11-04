package app.fieldwatch.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PittsburghOfflinePackTest {
    @Test
    fun packEntriesStayInsideTheOfflineMapRoot() {
        val root = File("build/test-pack-root")
        val tile = PittsburghOfflinePack.safePackEntry(root, "usgs-topo/16_18200_24600.png")
        assertEquals(
            File(root, "usgs-topo/16_18200_24600.png").canonicalFile,
            tile,
        )
        assertNull(PittsburghOfflinePack.safePackEntry(root, "../escape"))
        assertNull(PittsburghOfflinePack.safePackEntry(root, "/absolute"))
        assertNull(PittsburghOfflinePack.safePackEntry(root, "usgs-topo\\escape"))
    }
}
