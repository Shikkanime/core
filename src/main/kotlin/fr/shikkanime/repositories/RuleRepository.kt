package fr.shikkanime.repositories

import fr.shikkanime.entities.Rule

class RuleRepository : AbstractRepository<Rule>() {
    override fun getEntityClass() = Rule::class.java
}