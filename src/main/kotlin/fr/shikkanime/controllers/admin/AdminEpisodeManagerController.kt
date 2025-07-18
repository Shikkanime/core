package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import fr.shikkanime.wrappers.impl.caches.AniDBCachedWrapper
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

@Controller("/admin/episode-manager")
class AdminEpisodeManagerController {
    @Inject private lateinit var episodeVariantService: EpisodeVariantService

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodeManager(): Response {
        val files = File(Constant.exportsFolder, "/").listFiles()
            ?.filter { it.name.endsWith(".xlsx") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        return Response.template(
            Link.EPISODE_MANAGER,
            mutableMapOf(
                "files" to files
            )
        )
    }

    @Path("/download")
    @Get
    @AdminSessionAuthenticated
    private fun getDownload(@QueryParam file: String): Response {
        val requestedFile = File(Constant.exportsFolder, file)

        if (!requestedFile.run { exists() && name.endsWith(".xlsx") }) {
            val errorMessage = URLEncoder.encode("File not found", StandardCharsets.UTF_8)
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=$errorMessage")
        }

        return Response.multipart(FileManager.readFileAsByteArray(requestedFile), ContentType.Application.OctetStream)
    }

    @Path("/import")
    @Post
    @AdminSessionAuthenticated
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
        val file = File(Constant.exportsFolder, "imported_${System.currentTimeMillis()}.xlsx").apply {
            outputStream().buffered().use { it.write(bytes) }
        }

        val episodes = XSSFWorkbook(file.inputStream()).use { workbook ->
            workbook.sheetIterator().asSequence().flatMap { sheet ->
                sheet.rowIterator().asSequence().mapIndexedNotNull { index, row ->
                    if (index == 0) return@mapIndexedNotNull null
                    try {
                        AbstractPlatform.Episode(
                            CountryCode.valueOf(row.getCell(0)?.stringCellValue?.uppercase() ?: return@mapIndexedNotNull null),
                            row.getCell(1)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(2)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(3)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(4)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(5)?.stringCellValue,
                            ZonedDateTime.ofInstant(row.getCell(6)?.dateCellValue?.toInstant() ?: return@mapIndexedNotNull null, Constant.utcZoneId),
                            EpisodeType.valueOf(row.getCell(7)?.stringCellValue ?: return@mapIndexedNotNull null),
                            row.getCell(8)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(9)?.numericCellValue?.toInt() ?: return@mapIndexedNotNull null,
                            row.getCell(10)?.numericCellValue?.toInt() ?: return@mapIndexedNotNull null,
                            row.getCell(11)?.numericCellValue?.toLong() ?: return@mapIndexedNotNull null,
                            row.getCell(12)?.stringCellValue,
                            row.getCell(13)?.stringCellValue,
                            row.getCell(14)?.stringCellValue ?: return@mapIndexedNotNull null,
                            Platform.valueOf(row.getCell(15)?.stringCellValue ?: return@mapIndexedNotNull null),
                            row.getCell(16)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(17)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(18)?.stringCellValue ?: return@mapIndexedNotNull null,
                            row.getCell(19)?.stringCellValue == "true",
                            row.getCell(20)?.stringCellValue == "true"
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            }.toList()
        }

        episodes.forEach { episodeVariantService.save(it) }

        return Response.redirect("${Link.EPISODE_MANAGER.href}?success=${URLEncoder.encode("File imported successfully", StandardCharsets.UTF_8)}")
    }

    @Path("/anidb-update")
    @Post
    @AdminSessionAuthenticated
    private fun postAnidbUpdate(@BodyParam parts: MultiPartData): Response {
        var animeName: String? = null
        var fileContent: ByteArray? = null

        runBlocking {
            parts.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "animeName") {
                            animeName = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "anidbFile") {
                            fileContent = part.provider().readRemaining().readByteArray()
                        }
                    }
                    else -> {
                        // Do nothing
                    }
                }

                part.dispose()
            }
        }

        if (animeName.isNullOrBlank() || fileContent == null) {
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("Anime name and file are required.", StandardCharsets.UTF_8)}")
        }

        val workbook = XSSFWorkbook(fileContent.inputStream())

        val rows = workbook.sheetIterator().asSequence().flatMap { sheet ->
            sheet.rowIterator().asSequence().filter { row ->
                row.getCell(2)?.stringCellValue.equals(animeName, false)
            }
        }.toList()

        if (rows.isEmpty()) {
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("No rows found for anime '$animeName'", StandardCharsets.UTF_8)}")
        }

        val countries = rows.mapNotNull { row ->
            row.getCell(0)?.stringCellValue?.uppercase()?.let { CountryCode.valueOf(it) }
        }.distinct()

        val animeId = countries.flatMap { runBlocking { AniDBCachedWrapper.getAnimeTitles(it.locale) }.toList() }
            .firstOrNull { (_, titles) -> titles.any { it.equals(animeName, true) } }?.first

        if (animeId == null) {
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("Anime '$animeName' not found on AniDB.", StandardCharsets.UTF_8)}")
        }

        val animeEpisodes = runBlocking { AniDBCachedWrapper.getEpisodesByAnime("shikkanime", animeId) }

        rows.forEach { row ->
            val number = row.getCell(10).numericCellValue.toInt()
            val airdateCell = row.getCell(6)
            animeEpisodes.firstOrNull { it.number == number.toString() }?.let { episode ->
                airdateCell?.apply {
                    setCellValue(episode.airdate.atStartOfDay())
                    cellStyle = workbook.createCellStyle().apply {
                        dataFormat = workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss")
                    }
                }
            }
        }

        val outputFile = File(Constant.exportsFolder, "updated_${animeName.replace(" ", "_")}_${System.currentTimeMillis()}.xlsx").apply {
            outputStream().use { workbook.write(it) }
        }

        workbook.close()

        return Response.multipart(FileManager.readFileAsByteArray(outputFile), ContentType.Application.OctetStream)
    }
}
