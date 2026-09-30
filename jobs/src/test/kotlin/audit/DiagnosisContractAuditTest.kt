package fr.shikkanime.jobs.audit

import fr.shikkanime.jobs.diagnostics.*
import fr.shikkanime.jobs.impl.FetchLatestEpisodesJob
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.jobs.platforms.impl.AnimationDigitalNetworkPlatform
import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.ZonedDateTimeSerializer
import fr.shikkanime.models.Platform
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.time.ZonedDateTime

/**
 * Audit of the surfaces the new diagnosis code introduces. Reflection only, no production
 * behaviour is exercised: the point is to pin the public shape of the API and the design rules
 * AGENTS.md imposes, which a behavioural test would not catch.
 */
class DiagnosisContractAuditTest {

    @Test
    fun `every rejection reason must map to a known stage`() {
        val stages = RejectionStage.entries.toSet()
        val unmapped = RejectionReason.entries.filterNot { it.stage in stages }
        check(unmapped.isEmpty()) { "reasons with an unknown stage: $unmapped" }
    }

    @Test
    fun `the taxonomy must stay small enough to be a taxonomy`() {
        check(RejectionReason.entries.size <= 16) {
            "a rejection list of ${RejectionReason.entries.size} reasons is no longer grouped, it is a log"
        }
    }

    @Test
    fun `the registry must keep at most one run per platform`() {
        val registry = IngestionRunRegistry()
        val now = kotlinx.datetime.LocalDateTime(2026, 1, 10, 8, 0)
        repeat(50) { i ->
            val platform = Platform.entries[i % Platform.entries.size]
            registry.record(
                IngestionRun(platform, emptyList(), now, error = "run $i")
            )
        }
        check(registry.latestRuns().size <= Platform.entries.size) {
            "registry grew to ${registry.latestRuns().size} entries for ${Platform.entries.size} platforms"
        }
    }

    @Test
    fun `no diagnosis type may depend on ktor or a database`() {
        val forbidden = listOf("io.ktor", "org.jetbrains.exposed", "java.sql")
        val diagnosisTypes = listOf(
            IngestionRun::class.java,
            IngestionVerdict::class.java,
            IngestionRunRegistry::class.java,
            RejectionReason::class.java,
            RejectionStage::class.java,
            PlatformItem::class.java
        )
        diagnosisTypes.forEach { type ->
            val fields = generateSequence(type) { it.superclass }
                .flatMap { it.declaredFields.asSequence() }
                .map { it.type.name }
                .toList()
            val offenders = fields.filter { f -> forbidden.any { f.startsWith(it) } }
            check(offenders.isEmpty()) { "${type.simpleName} depends on $offenders" }
        }
    }

    @Test
    fun `the platform port must stay independent of the database`() {
        val forbidden = listOf("io.ktor", "org.jetbrains.exposed", "java.sql")
        val fields = StreamingPlatform::class.java.declaredFields
            .map { it.type.name }
            .toList()
        val offenders = fields.filter { f -> forbidden.any { f.startsWith(it) } }
        check(offenders.isEmpty()) { "StreamingPlatform depends on $offenders" }
    }

    @Test
    fun `the job must receive its collaborators by constructor`() {
        val constructors = FetchLatestEpisodesJob::class.java.declaredConstructors
        check(constructors.size == 1) { "expected one constructor, found ${constructors.size}" }
        val parameters = constructors.first().parameterCount
        check(parameters == 2) { "the job should take its platforms and registry, got $parameters parameters" }
    }

    @Test
    fun `the registry must be a koin singleton and not a global object`() {
        val isSingleton = IngestionRunRegistry::class.java.annotations.any {
            it.annotationClass.simpleName == "Single"
        }
        check(isSingleton) { "IngestionRunRegistry must be a Koin @Single, not a manual singleton" }
        check(!Modifier.isStatic(IngestionRunRegistry::class.java.methods.first().modifiers)) {
            "no static state expected on the registry"
        }
    }

    @Test
    fun `a report must never leak a stack trace or a raw exception`() {
        val run = IngestionRun(
            platform = Platform.ANIMATION_DIGITAL_NETWORK,
            verdicts = emptyList(),
            fetchedAt = kotlinx.datetime.LocalDateTime(2026, 1, 10, 8, 0),
            error = "java.lang.IllegalStateException: HTTP 500"
        )
        val report = run.describe()
        check("\tat " !in report) { "the report must not carry a stack trace:\n$report" }
    }

    @Test
    fun `the ADN platform must keep implementing the port`() {
        check(StreamingPlatform::class.java.isAssignableFrom(AnimationDigitalNetworkPlatform::class.java)) {
            "the ADN platform must implement StreamingPlatform"
        }
        check(Platform.ANIMATION_DIGITAL_NETWORK.name == "ANIMATION_DIGITAL_NETWORK") {
            "the platform enum name is part of the report format"
        }
    }

    @Test
    fun `the smart http client must stay a concrete injected dependency`() {
        val constructors = AnimationDigitalNetworkPlatform::class.java.declaredConstructors
        val params = constructors.first().parameterTypes.toList()
        check(SmartHttpClient::class.java in params) { "the client must stay constructor-injected" }
    }
}
