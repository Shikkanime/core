package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Rule
import fr.shikkanime.repositories.RuleRepository

class RuleService : AbstractService<Rule, RuleRepository>() {
    @Inject
    private lateinit var ruleRepository: RuleRepository

    override fun getRepository() = ruleRepository
}