package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "anime_platform",
    indexes = [
        Index(
            name = "idx_anime_platform_platform_platform_id",
            columnList = "platform, platform_id"
        ),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class AnimePlatform(
    uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var anime: Anime? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val platform: Platform? = null,
    @Column(nullable = false, name = "platform_id")
    var platformId: String? = null,
    @Column(nullable = true, name = "last_validate_date_time")
    var lastValidateDateTime: ZonedDateTime? = null,
) : ShikkEntity(uuid)
