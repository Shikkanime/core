package fr.shikkanime.services

import fr.shikkanime.entities.Tag
import fr.shikkanime.repositories.TagRepository

class TagService : AbstractService<Tag, TagRepository>() {
    fun findByName(name: String) = repository.findByName(name)

    fun findOrSave(name: String) = repository.findByName(name) ?: save(Tag(name = name))
}