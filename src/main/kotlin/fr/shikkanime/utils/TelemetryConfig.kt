package fr.shikkanime.utils

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler

object TelemetryConfig {
    private var isInitialized = false
    private lateinit var openTelemetry: OpenTelemetry

    fun initialize() {
        if (isInitialized) {
            return
        }

        val resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), Constant.NAME,
                AttributeKey.stringKey("service.version"), this::class.java.`package`.implementationVersion ?: "unknown"
            )))

        val spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:4317")
            .build()

        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setResource(resource)
            .setSampler(Sampler.alwaysOn())
            .build()

        openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()

        GlobalOpenTelemetry.resetForTest()
        GlobalOpenTelemetry.set(openTelemetry)

        isInitialized = true
    }

    fun getTracer(name: String): Tracer? {
        if (!isInitialized) {
            return null
        }

        return openTelemetry.getTracer(name)
    }

    private fun getName(): String {
        val line = Exception().stackTraceToString().split("\n").drop(1).firstOrNull { !it.contains("TelemetryConfig") } ?: ""
        val split = line.substringBefore("(").split(".")
        val className = split.dropLast(1).last()
        val methodName = split.last().substringBefore("$")
        return "$className.$methodName"
    }

    fun <T> Tracer.traceWithAttributes(name: String? = null, fn: (Span) -> T): T {
        val span = this.spanBuilder(if (name.isNullOrBlank()) getName() else name).setParent(Context.current()).startSpan()
        val scope = span.makeCurrent()

        return try {
            fn(span)
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.localizedMessage)
            throw e
        } finally {
            scope.close()
            span.end()
        }
    }

    // Surcharge pour compatibilité avec le code existant
    fun <T> Tracer?.trace(name: String? = null, fn: () -> T): T = if (this != null) traceWithAttributes(name) { _ -> fn() } else fn()
}