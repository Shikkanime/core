package fr.shikkanime.entities

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.EncryptionManager
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "episode_variant",
    indexes = [
        Index(
            name = "idx_episode_variant_mapping_uuid",
            columnList = "mapping_uuid"
        ),
        Index(
            name = "idx_episode_variant_release_date_mapping_uuid",
            columnList = "release_date_time, mapping_uuid"
        ),
        Index(
            name = "idx_episode_variant_group_hash",
            columnList = "group_hash"
        ),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class EpisodeVariant(
    uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var mapping: EpisodeMapping? = null,
    @Column(nullable = false, name = "release_date_time")
    var releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var platform: Platform? = null,
    @Column(nullable = false, name = "audio_locale")
    var audioLocale: String? = null,
    @Column(nullable = false, unique = true)
    var identifier: String? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var url: String? = null,
    @Column(nullable = false)
    var uncensored: Boolean = false,
    @Column(name = "group_hash")
    var groupHash: ByteArray? = getGroupHash(mapping?.anime?.uuid!!, mapping.episodeType!!, releaseDateTime),
) : ShikkEntity(uuid) {
    companion object {
        fun getGroupHash(animeUuid: UUID, episodeType: EpisodeType, releaseDateTime: ZonedDateTime) =
            EncryptionManager.toSHA512ByteArray("${animeUuid}${episodeType}${releaseDateTime.withMinute(0).withSecond(0).withNano(0)}")

        fun getGroupHash(episodeVariant: EpisodeVariant) =
            getGroupHash(episodeVariant.mapping?.anime?.uuid!!, episodeVariant.mapping?.episodeType!!, episodeVariant.releaseDateTime)
    }
}
