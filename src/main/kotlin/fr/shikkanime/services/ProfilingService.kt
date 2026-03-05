package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.RouteMetric
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import jdk.jfr.Configuration
import jdk.jfr.Recording
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level

class ProfilingService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var globalRecording: Recording? = null
    private val profilesFolder = File(Constant.dataFolder, "profiles")
    private val metricBuffer = ConcurrentLinkedQueue<RouteMetric>()

    @Inject private lateinit var routeMetricService: RouteMetricService

    init {
        if (!profilesFolder.exists()) {
            profilesFolder.mkdirs()
        }
    }

    fun startGlobalRecording() {
        if (globalRecording != null) return

        try {
            val config = Configuration.getConfiguration("default")
            globalRecording = Recording(config).apply {
                name = "ContinuousRecording"
                isToDisk = true
                maxSize = 250L * 1024 * 1024 // 250 MB
                start()
            }
            logger.info("JFR Global Recording started.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to start JFR Global Recording", e)
        }
    }

    fun dumpAndRestart() {
        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dumpFile = File(profilesFolder, "profile-$timestamp.jfr")

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

    fun addRouteMetric(method: String, path: String, duration: Long) {
        metricBuffer.add(RouteMetric(method = method, path = path, duration = duration))
    }

    fun flushMetrics() {
        val metrics = mutableListOf<RouteMetric>()
        while (true) {
            val metric = metricBuffer.poll() ?: break
            metrics.add(metric)
        }

        if (metrics.isNotEmpty()) {
            try {
                routeMetricService.saveAll(metrics)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to flush route metrics", e)
            }
        }
    }

    fun getJfrFiles(): List<File> {
        return profilesFolder.listFiles()?.filter { it.extension == "jfr" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getJfrFile(name: String): File? {
        val file = File(profilesFolder, name)
        return if (file.exists() && file.parentFile == profilesFolder) file else null
    }

    fun cleanOldReports(days: Int = 7) {
        val limit = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        profilesFolder.listFiles()?.filter { it.extension == "jfr" && it.lastModified() < limit }?.forEach { it.delete() }
    }
}
