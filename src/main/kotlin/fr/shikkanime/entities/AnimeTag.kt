package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.*

@Entity
@Table(
    name = "anime_tag",
    indexes = [
        Index(
            name = "idx_anime_tag_anime_uuid_tag_uuid",
            columnList = "anime_uuid, tag_uuid"
        ),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class AnimeTag(
    uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var anime: Anime? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var tag: Tag? = null,
    @Column(nullable = false, name = "is_adult")
    var isAdult: Boolean = false,
    @Column(nullable = false, name = "is_spoiler")
    var isSpoiler: Boolean = false,
) : ShikkEntity(uuid)
