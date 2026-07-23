package fr.shikkanime

import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.repositories.EpisodeVariantRepository
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.indexers.GroupedIndexer
import fr.shikkanime.utils.indexers.NewGroupIndexer
import kotlin.system.exitProcess
import kotlin.time.measureTimedValue

suspend fun main() {
    val epochs = 1_000
    val episodeVariantRepository = Constant.injector.getInstance(EpisodeVariantRepository::class.java)
    episodeVariantRepository.preIndex()
    val page = 15
    val pageSize = 9
    val predicate: (NewGroupIndexer.Element) -> Boolean = { it.variants.any { variant -> variant.langType == LangType.VOICE } }
    val comparator = compareByDescending<NewGroupIndexer.Element> { it.releaseDateTime }
        .thenByDescending { it.animeSlug }
        .thenByDescending { it.episodeType.ordinal }

    val timedValue1 = measureTimedValue {
        for (i in 1..epochs) {
            NewGroupIndexer.getPaginatedElements(
                page = page,
                limit = pageSize,
                predicate = predicate,
                comparator = comparator
            )
        }

        NewGroupIndexer.getPaginatedElements(
            page = page,
            limit = pageSize,
            predicate = predicate,
            comparator = comparator
        )
    }

    println(timedValue1.duration)
    println(timedValue1.value)

    val timedValue3 = measureTimedValue {
        for (i in 1..epochs) {
            GroupedIndexer.pageableRecords(
                page,
                pageSize,
                comparator = compareByDescending<GroupedIndexer.GroupedRecord> { it.releaseMillis }
                    .thenByDescending { it.key.animeSlug }
                    .thenByDescending { it.key.episodeType.ordinal }
            )
        }

        GroupedIndexer.pageableRecords(
            page,
            pageSize,
            comparator = compareByDescending<GroupedIndexer.GroupedRecord> { it.releaseMillis }
                .thenByDescending { it.key.animeSlug }
                .thenByDescending { it.key.episodeType.ordinal }
        )
    }

    println(timedValue3.duration)
    println(timedValue3.value)

    exitProcess(0)
}