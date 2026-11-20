package fr.shikkanime.database.entities

import fr.shikkanime.models.AnimeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

internal const val ANIME_METADATA_LINK_TABLE_NAME = "anime_metadata_link"

internal object AnimeMetadataLinkTable : ShikkTable(ANIME_METADATA_LINK_TABLE_NAME) {
    val anime = reference(
        ANIME_TABLE_ID,
        AnimeTable,
        fkName = ANIME_METADATA_LINK_TABLE_NAME + "_" + ANIME_TABLE_NAME + FK
    )
    val platform = enumerationByName<MetadataPlatform>("platform", 30)
    val platformId = varchar("platform_id", 255)

    init {
        // The unique key is the triple, like anime_platform: one stored anime can point at
        // several entries of the same metadata platform (one per season of a franchise).
        uniqueIndex(
            "${ANIME_TABLE_ID}_platform_platform_id$UQ",
            anime,
            platform,
            platformId
        )
        index("metadata_platform_platform_id", false, platform, platformId)
    }
}

internal class AnimeMetadataLinkEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, AnimeMetadataLinkTable),
    ModelConvertible<AnimeMetadataLink> {
    companion object : UuidEntityClass<AnimeMetadataLinkEntity>(AnimeMetadataLinkTable)

    var anime by AnimeMetadataLinkTable.anime
    var platform by AnimeMetadataLinkTable.platform
    var platformId by AnimeMetadataLinkTable.platformId

    override fun toModel(): AnimeMetadataLink =
        AnimeMetadataLink(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            animeId = anime.value,
            platform = platform,
            platformId = platformId
        )
}
