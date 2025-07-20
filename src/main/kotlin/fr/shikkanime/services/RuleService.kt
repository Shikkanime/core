package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.RuleRepository

class RuleService : AbstractService<Rule, RuleRepository>() {
    @Inject private lateinit var ruleRepository: RuleRepository
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = ruleRepository

    override fun save(entity: Rule): Rule {
        val rule = super.save(entity)
        traceActionService.createTraceAction(rule, TraceAction.Action.CREATE)
        return rule
    }

    override fun update(entity: Rule): Rule {
        val rule = super.update(entity)
        traceActionService.createTraceAction(rule, TraceAction.Action.UPDATE)
        return rule
    }

    override fun delete(entity: Rule) {
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }
}