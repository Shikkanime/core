package fr.shikkanime.entities.views

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Synchronize
import org.hibernate.annotations.View
import java.sql.Types
import java.time.ZonedDateTime
import java.util.*

@Entity
@Immutable
@Table(
    name = "grouped_episode_view",
    indexes = [
        Index(name = "idx_grouped_episode_view_anime_uuid", columnList = "anime_uuid"),
        Index(name = "idx_grouped_episode_view_order", columnList = "min_release_date_time desc, min_season desc, episode_type desc, min_number desc"),
    ]
)
@View(
    query = """
        SELECT a.uuid AS uuid,
                   MIN(ev.release_date_time) AS min_release_date_time,
                   MAX(em.last_update_date_time) AS max_last_update_date_time,
                   MIN(em.season) AS min_season,
                   MAX(em.season) AS max_season,
                   em.episode_type AS episode_type,
                   MIN(em.number) AS min_number,
                   MAX(em.number) AS max_number,
                   ARRAY_AGG(DISTINCT ev.platform ORDER BY ev.platform) AS platforms,
                   ARRAY_AGG(DISTINCT ev.audio_locale ORDER BY ev.audio_locale) AS audio_locales,
                   ARRAY_AGG(DISTINCT ev.url ORDER BY ev.url) AS urls,
                   ARRAY_AGG(em.uuid ORDER BY em.season,em.episode_type,em.number) AS episode_mapping_uuids,
                   CASE WHEN COUNT(DISTINCT em.uuid)=1 THEN MIN(em.title) ELSE NULL END AS title,
                   CASE WHEN COUNT(DISTINCT em.uuid)=1 THEN MIN(em.description) ELSE NULL END AS description,
                   CASE WHEN COUNT(DISTINCT em.uuid)=1 THEN MIN(em.duration) ELSE NULL END AS duration
            FROM anime a
                     JOIN episode_mapping em ON a.uuid=em.anime_uuid
                     JOIN episode_variant ev ON em.uuid=ev.mapping_uuid
            GROUP BY a.uuid,
                     em.episode_type,
                     DATE_TRUNC('hour',ev.release_date_time)
    """
)
@Synchronize(value = ["anime", "episode_mapping", "episode_variant"])
class GroupedEpisodeView(
    uuid: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid", insertable = false, updatable = false)
    val anime: Anime? = null,
    @Column(name = "min_release_date_time", insertable = false, updatable = false)
    val minReleaseDateTime: ZonedDateTime? = null,
    @Column(name = "max_last_update_date_time", insertable = false, updatable = false)
    val maxLastUpdateDateTime: ZonedDateTime? = null,
    @Column(name = "min_season", insertable = false, updatable = false)
    val minSeason: Int? = null,
    @Column(name = "max_season", insertable = false, updatable = false)
    val maxSeason: Int? = null,
    @Column(name = "episode_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    val episodeType: EpisodeType? = null,
    @Column(name = "min_number", insertable = false, updatable = false)
    val minNumber: Int? = null,
    @Column(name = "max_number", insertable = false, updatable = false)
    val maxNumber: Int? = null,
    @Convert(converter = PlatformArrayConverter::class)
    @Column(name = "platforms", insertable = false, updatable = false, columnDefinition = "varchar[]")
    @Enumerated(EnumType.STRING)
    val platforms: Set<Platform>? = null,
    @Convert(converter = AudioLocaleArrayConverter::class)
    @Column(name = "audio_locales", insertable = false, updatable = false, columnDefinition = "varchar[]")
    val audioLocales: Set<String>? = null,
    @Convert(converter = UrlArrayConverter::class)
    @Column(name = "urls", insertable = false, updatable = false, columnDefinition = "varchar[]")
    val urls: Set<String>? = null,
    @JdbcTypeCode(Types.ARRAY)
    @Column(name = "episode_mapping_uuids", insertable = false, updatable = false, columnDefinition = "uuid[]")
    val episodeMappingUuids: Set<UUID>? = null,
    @Column(name = "title", insertable = false, updatable = false)
    val title: String? = null,
    @Column(name = "description", insertable = false, updatable = false)
    val description: String? = null,
    @Column(name = "duration", insertable = false, updatable = false)
    val duration: Long? = null
) : ShikkEntity(uuid)

@Converter
private class PlatformArrayConverter : AttributeConverter<Set<Platform>, Array<String>> {
    override fun convertToDatabaseColumn(attribute: Set<Platform>?): Array<String>? {
        return attribute?.map { it.name }?.toTypedArray()
    }

    override fun convertToEntityAttribute(dbData: Array<String>?): Set<Platform>? {
        return dbData?.map { Platform.valueOf(it) }?.toSet()
    }
}

@Converter
private class AudioLocaleArrayConverter : AttributeConverter<Set<String>, Array<String>> {
    override fun convertToDatabaseColumn(attribute: Set<String>?): Array<String>? {
        return attribute?.toTypedArray()
    }

    override fun convertToEntityAttribute(dbData: Array<String>?): Set<String>? {
        return dbData?.toSet()
    }
}

@Converter
private class UrlArrayConverter : AttributeConverter<Set<String>, Array<String>> {
    override fun convertToDatabaseColumn(attribute: Set<String>?): Array<String>? {
        return attribute?.toTypedArray()
    }

    override fun convertToEntityAttribute(dbData: Array<String>?): Set<String>? {
        return dbData?.toSet()
    }
}
