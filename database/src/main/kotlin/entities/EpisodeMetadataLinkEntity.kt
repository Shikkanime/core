package fr.shikkanime.database.entities

import fr.shikkanime.models.EpisodeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

internal const val EPISODE_METADATA_LINK_TABLE_NAME = "episode_metadata_link"

internal object EpisodeMetadataLinkTable : ShikkTable(EPISODE_METADATA_LINK_TABLE_NAME) {
    val episode = reference(
        EPISODE_TABLE_ID,
        EpisodeTable,
        fkName = EPISODE_METADATA_LINK_TABLE_NAME + "_" + EPISODE_TABLE_NAME + FK
    )
    val platform = enumerationByName<MetadataPlatform>("platform", 30)
    val platformId = varchar("platform_id", 255)
    val platformAnimeId = varchar("platform_anime_id", 255)

    init {
        // One stored episode links once per external entry: platform_id identifies the episode
        // on its metadata platform, platform_anime_id the external anime it belongs to.
        uniqueIndex(
            "${EPISODE_TABLE_ID}_platform_platform_id$UQ",
            episode,
            platform,
            platformId
        )
        index("episode_metadata_platform_id", false, platform, platformId)
    }
}

internal class EpisodeMetadataLinkEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, EpisodeMetadataLinkTable),
    ModelConvertible<EpisodeMetadataLink> {
    companion object : UuidEntityClass<EpisodeMetadataLinkEntity>(EpisodeMetadataLinkTable)

    var episode by EpisodeMetadataLinkTable.episode
    var platform by EpisodeMetadataLinkTable.platform
    var platformId by EpisodeMetadataLinkTable.platformId
    var platformAnimeId by EpisodeMetadataLinkTable.platformAnimeId

    override fun toModel(): EpisodeMetadataLink =
        EpisodeMetadataLink(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            episodeId = episode.value,
            platform = platform,
            platformId = platformId,
            platformAnimeId = platformAnimeId
        )
}
