package app.fieldwatch.data

import android.content.Context
import app.fieldwatch.BuildConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OfflinePackPhase { WAITING, DOWNLOADING, INSTALLING, READY, PAUSED }

data class OfflinePackState(
    val phase: OfflinePackPhase = OfflinePackPhase.WAITING,
    val percent: Int = 0,
    val ready: Boolean = false,
)

/** A resumable, one-time Pittsburgh USGS Topo pack for the dedicated Shield car display. */
class PittsburghOfflinePack(private val context: Context) {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<OfflinePackState> = _state.asStateFlow()

    suspend fun installUntilReady() {
        if (installed()) {
            _state.value = OfflinePackState(OfflinePackPhase.READY, 100, ready = true)
            return
        }
        while (currentCoroutineContext().isActive) {
            try {
                installOnce()
                _state.value = OfflinePackState(OfflinePackPhase.READY, 100, ready = true)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = OfflinePackState(OfflinePackPhase.PAUSED, _state.value.percent)
                delay(RETRY_MS)
            }
        }
    }

    private fun initialState(): OfflinePackState =
        if (installed()) OfflinePackState(OfflinePackPhase.READY, 100, ready = true)
        else OfflinePackState()

    private fun externalRoot(): File =
        context.getExternalFilesDir(null) ?: error("External app storage is unavailable")

    private fun mapRoot(): File = File(externalRoot(), PathTiles.TILE_ROOT_NAME)

    private fun marker(): File = File(mapRoot(), COMPLETE_MARKER)

    private fun installed(): Boolean =
        runCatching { marker().readText().trim() == EXPECTED_SHA256 }.getOrDefault(false)

    private suspend fun installOnce() {
        val root = mapRoot().apply { mkdirs() }
        val archive = File(root, PARTIAL_ARCHIVE)
        download(archive)
        if (sha256(archive) != EXPECTED_SHA256) {
            archive.delete()
            error("Offline map checksum mismatch")
        }
        extract(archive, root)
        marker().writeText(EXPECTED_SHA256)
        archive.delete()
    }

    private suspend fun download(target: File) {
        currentCoroutineContext().ensureActive()
        if (target.isFile && target.length() == EXPECTED_BYTES) {
            if (sha256(target) == EXPECTED_SHA256) return
            target.delete()
        } else if (target.length() > EXPECTED_BYTES) {
            target.delete()
        }
        val have = target.takeIf { it.isFile }?.length() ?: 0L
        val connection = (URL(DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Fieldwatch/${BuildConfig.VERSION_NAME} (https://github.com/mickeyslaven/Fieldwatch)",
            )
            if (have > 0L) setRequestProperty("Range", "bytes=$have-")
        }
        try {
            val code = connection.responseCode
            val append = have > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                error("Offline map download returned HTTP $code")
            }
            val base = if (append) have else 0L
            val remaining = connection.contentLengthLong.coerceAtLeast(0L)
            val total = (base + remaining).coerceAtLeast(1L)
            if (!append && target.isFile) target.delete()
            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(FileOutputStream(target, append)).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = base
                    var lastPercent = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        written += count
                        val percent = ((written * DOWNLOAD_SHARE) / total).toInt()
                            .coerceIn(0, DOWNLOAD_SHARE)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _state.value = OfflinePackState(OfflinePackPhase.DOWNLOADING, percent)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun extract(archive: File, root: File) {
        var entries = 0
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = zip.nextEntry ?: break
                val target = safePackEntry(root, entry.name) ?: error("Unsafe offline map entry")
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    val partial = File(target.parentFile, "${target.name}.part")
                    BufferedOutputStream(FileOutputStream(partial)).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = zip.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                    }
                    if (target.isFile) target.delete()
                    if (!partial.renameTo(target)) error("Could not install offline map tile")
                }
                zip.closeEntry()
                entries++
                val percent = DOWNLOAD_SHARE +
                    ((entries.coerceAtMost(EXPECTED_ENTRIES) * (100 - DOWNLOAD_SHARE)) / EXPECTED_ENTRIES)
                _state.value = OfflinePackState(OfflinePackPhase.INSTALLING, percent.coerceIn(0, 99))
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val EXPECTED_SHA256 = "51246e4b7f629307208771788a6fc07b63fff44276c842f49d999f3d9f44fa94"
        private const val EXPECTED_BYTES = 59_928_323L
        private const val EXPECTED_ENTRIES = 2_718
        private const val DOWNLOAD_SHARE = 82
        private const val RETRY_MS = 30_000L
        private const val COMPLETE_MARKER = ".pittsburgh-usgs-10km-v1.complete"
        private const val PARTIAL_ARCHIVE = ".pittsburgh-usgs-10km-v1.zip.part"
        private const val DOWNLOAD_URL =
            "https://github.com/mickeyslaven/Fieldwatch/releases/download/" +
                "v1.1.42-shield7.1-pittsburgh/Pittsburgh-USGS-10km-v1.zip"

        internal fun safePackEntry(root: File, name: String): File? {
            if (name.isBlank() || name.startsWith('/') || '\\' in name) return null
            val canonicalRoot = root.canonicalFile
            val target = File(canonicalRoot, name).canonicalFile
            val prefix = canonicalRoot.path + File.separator
            return target.takeIf { it.path.startsWith(prefix) }
        }
    }
}
