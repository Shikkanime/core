package fr.shikkanime.services

import com.sun.management.HotSpotDiagnosticMXBean
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import jdk.jfr.Configuration
import jdk.jfr.Recording
import java.io.File
import java.lang.management.ManagementFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ProfilingService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var globalRecording: Recording? = null

    companion object {
        private const val MAX_SIZE = 250L * 1024 * 1024 // 250 MB
    }

    fun startGlobalRecording() {
        if (globalRecording != null) return

        try {
            val config = Configuration.getConfiguration("default")
            globalRecording = Recording(config).apply {
                name = "ContinuousRecording"
                isToDisk = true
                maxSize = MAX_SIZE
                start()
            }
            logger.info("JFR Global Recording started.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to start JFR Global Recording", e)
        }
    }

    fun dumpHeap(live: Boolean = true) {
        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val dumpFile = File(Constant.profilesFolder, "heapdump-$timestamp.hprof")

        try {
            val server = ManagementFactory.getPlatformMBeanServer()
            val mxBean = ManagementFactory.newPlatformMXBeanProxy(
                server,
                "com.sun.management:type=HotSpotDiagnostic",
                HotSpotDiagnosticMXBean::class.java
            )

            logger.info("Starting heap dump to ${dumpFile.absolutePath}...")
            mxBean.dumpHeap(dumpFile.absolutePath, live)
            logger.info("Heap dump completed successfully.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to dump heap", e)
        }
    }

    fun dumpAndRestart() {
        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dumpFile = File(Constant.profilesFolder, "profile-$timestamp.jfr")

        try {
            globalRecording?.let {
                it.stop()
                it.dump(dumpFile.toPath())
                it.close()
                logger.info("JFR Profile dumped to ${dumpFile.absolutePath}")
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to dump JFR Recording", e)
        } finally {
            globalRecording = null
            startGlobalRecording()
        }
    }

    fun getDumpFiles(): List<File> {
        return Constant.profilesFolder.listFiles()?.filter { it.extension in listOf("jfr", "hprof") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getDumpFile(name: String): File? {
        return getDumpFiles().find { it.name == name }
    }

    fun cleanOldReports(days: Int = 7) {
        val limit = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        Constant.profilesFolder.listFiles()
            ?.filter { it.extension in listOf("jfr", "hprof") && it.lastModified() < limit }
            ?.forEach {
                if (!it.delete()) {
                    logger.warning("Failed to delete dump report: ${it.absolutePath}")
                }
            }
    }
}
