package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

@Controller("$ADMIN/episode-manager")
@AdminSessionAuthenticated
class AdminEpisodeManagerController {
    private val logger = LoggerFactory.getLogger(javaClass)
    @Inject private lateinit var episodeVariantService: EpisodeVariantService

    @Path
    @Get
    private fun getEpisodeManager(): Response {
        val files = Constant.exportsFolder.listFiles()
            .filter { it.extension == "xlsx" }
            .sortedByDescending { it.lastModified() }

        return Response.template(
            Link.EPISODE_MANAGER,
            mutableMapOf(
                "files" to files
            )
        )
    }

    @Path("/download")
    @Get
    private fun getDownload(@QueryParam file: String): Response {
        val requestedFile = File(Constant.exportsFolder, file)

        if (!requestedFile.run { exists() && name.endsWith(".xlsx") }) {
            val errorMessage = URLEncoder.encode("File not found", StandardCharsets.UTF_8)
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=$errorMessage")
        }

        return Response.multipart(requestedFile.readBytes(), ContentType.Application.OctetStream)
    }

    @Path("/delete")
    @Get
    private fun getDelete(@QueryParam file: String): Response {
        val requestedFile = File(Constant.exportsFolder, file)

        if (requestedFile.run { exists() && name.endsWith(".xlsx") } && !requestedFile.delete())
            logger.warning("Could not delete file: ${requestedFile.absolutePath}")

        return Response.redirect(Link.EPISODE_MANAGER.href)
    }

    @Path("/import")
    @Post
    private fun postImport(@BodyParam multiPartData: MultiPartData): Response {
        var bytes: ByteArray? = null

        runBlocking {
            multiPartData.forEachPart { part ->
                if (part is PartData.FileItem) {
                    bytes = part.provider().readRemaining().readByteArray()
                }

                part.dispose()
            }
        }

        requireNotNull(bytes) { "No file provided" }
        val file = File.createTempFile("import_", ".xlsx").apply { writeBytes(bytes) }

        val episodes = XSSFWorkbook(file.inputStream()).use { workbook ->
            workbook.sheetIterator().asSequence().flatMap { sheet ->
                sheet.rowIterator().asSequence().drop(1).mapNotNull { row ->
                    var cell = 0
                    try {
                        AbstractPlatform.Episode(
                            CountryCode.valueOf(row.getCell(cell++)?.stringCellValue?.uppercase() ?: return@mapNotNull null),
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            mapOf(
                                ImageType.THUMBNAIL to (row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null),
                                ImageType.BANNER to (row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null),
                                ImageType.CAROUSEL to (row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null),
                                ImageType.TITLE to (row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null),
                            ),
                            row.getCell(cell++)?.stringCellValue,
                            ZonedDateTime.ofInstant(row.getCell(cell++)?.dateCellValue?.toInstant() ?: return@mapNotNull null, Constant.utcZoneId),
                            EpisodeType.valueOf(row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null),
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            row.getCell(cell++)?.numericCellValue?.toInt() ?: return@mapNotNull null,
                            row.getCell(cell++)?.numericCellValue?.toInt() ?: return@mapNotNull null,
                            row.getCell(cell++)?.numericCellValue?.toLong() ?: return@mapNotNull null,
                            row.getCell(cell++)?.stringCellValue,
                            row.getCell(cell++)?.stringCellValue,
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            Platform.valueOf(row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null),
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            row.getCell(cell++)?.stringCellValue ?: return@mapNotNull null,
                            row.getCell(cell++)?.stringCellValue == "true",
                            row.getCell(cell)?.stringCellValue == "true"
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            }.toList()
        }

        if (!file.delete())
            logger.warning("Could not delete file: ${file.absolutePath}")

        episodes.forEach { episodeVariantService.save(it) }

        InvalidationService.invalidate(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Simulcast::class.java
        )

        return Response.redirect("${Link.EPISODE_MANAGER.href}?success=${URLEncoder.encode("File imported successfully", StandardCharsets.UTF_8)}")
    }
}