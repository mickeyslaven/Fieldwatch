package app.fieldwatch.data

import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
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

    @Test
    fun failureLabelsExplainTheNextAction() {
        assertEquals("NO INTERNET", PittsburghOfflinePack.failureLabel(UnknownHostException()))
        assertEquals("TIMEOUT", PittsburghOfflinePack.failureLabel(SocketTimeoutException()))
        assertEquals("TLS ERROR", PittsburghOfflinePack.failureLabel(SSLException("old trust store")))
        assertEquals("STORAGE FULL", PittsburghOfflinePack.failureLabel(IOException("No space left on device")))
        assertEquals("CHECKSUM", PittsburghOfflinePack.failureLabel(IllegalStateException("checksum mismatch")))
        assertEquals("HTTP 403", PittsburghOfflinePack.failureLabel(IllegalStateException("returned HTTP 403")))
        assertEquals("INSTALL ERROR", PittsburghOfflinePack.failureLabel(IllegalStateException("other")))
    }
}
