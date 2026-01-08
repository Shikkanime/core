package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Tag
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.TagRepository

class TagService : AbstractService<Tag, TagRepository>() {
    @Inject private lateinit var tagRepository: TagRepository
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = tagRepository

    fun findByName(name: String) = tagRepository.findByName(name)

    fun findOrSave(name: String) = tagRepository.findByName(name) ?: save(Tag(name = name))

    override fun save(entity: Tag): Tag {
        val tag = super.save(entity)
        traceActionService.createTraceAction(tag, TraceAction.Action.CREATE)
        return tag
    }
}