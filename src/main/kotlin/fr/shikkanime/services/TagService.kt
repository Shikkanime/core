package fr.shikkanime.services

import fr.shikkanime.entities.Tag
import fr.shikkanime.repositories.TagRepository

class TagService : AbstractService<Tag, TagRepository>() {
    suspend fun findByName(name: String) = repository.findByName(name)

    suspend fun findOrSave(name: String) = repository.findByName(name) ?: save(Tag(name = name))
}