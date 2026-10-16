package fr.shikkanime.database.entities

import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.ModelConvertible
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val ANIME_PLATFORM_TABLE_NAME = "anime_platform"

internal object AnimePlatformTable : ShikkTable(ANIME_PLATFORM_TABLE_NAME) {
    val anime = reference(
        ANIME_TABLE_ID,
        AnimeTable,
        fkName = ANIME_PLATFORM_TABLE_NAME + "_" + ANIME_TABLE_NAME + FK
    )
    val platform = enumerationByName<Platform>("platform", 30)
    val platformId = varchar("platform_id", 255)

    init {
        // The unique key is the triple, never (platform, platform_id): several stored animes can
        // share one platform identifier (One Piece on Crunchyroll: same platform_id, one anime per
        // season), while the same anime can carry several platform identifiers (One Piece on ADN).
        uniqueIndex("${ANIME_TABLE_ID}_platform_platform_id$UQ", anime, platform, platformId)
        index("platform_platform_id", false, platform, platformId)
    }
}

internal class AnimePlatformEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, AnimePlatformTable),
    ModelConvertible<AnimePlatform> {
    companion object : UuidEntityClass<AnimePlatformEntity>(AnimePlatformTable)

    var anime by AnimePlatformTable.anime
    var platform by AnimePlatformTable.platform
    var platformId by AnimePlatformTable.platformId

    override fun toModel(): AnimePlatform =
        AnimePlatform(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            platform = platform,
            platformId = platformId
        )
}
