package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.analytics.MarketShareDto
import fr.shikkanime.entities.miscellaneous.MarketShare
import fr.shikkanime.factories.IGenericFactory

class MarketShareFactory : IGenericFactory<MarketShare, MarketShareDto> {
    @Inject private lateinit var simulcastFactory: SimulcastFactory
    @Inject private lateinit var platformFactory: PlatformFactory

    override fun toDto(entity: MarketShare) = MarketShareDto(
        simulcast = simulcastFactory.toDto(entity.simulcast),
        platform = platformFactory.toDto(entity.platform),
        value = entity.value
    )
}