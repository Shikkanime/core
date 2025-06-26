package fr.shikkanime.utils

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64

object EncryptionManager {
    private val salt = Constant.NAME.toByteArray(StandardCharsets.UTF_8)
    private const val ITERATIONS = 2
    private const val MEM_LIMIT = 66536
    private const val HASH_LENGTH = 32
    private const val PARALLELISM = 1
    private val builder = getBuilder()

    private fun getBuilder(): Argon2Parameters.Builder {
        return Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ITERATIONS)
            .withMemoryAsKB(MEM_LIMIT)
            .withParallelism(PARALLELISM)
            .withSalt(salt)
    }

    fun generate(password: String): ByteArray {
        val generate = Argon2BytesGenerator()
        generate.init(builder.build())
        val result = ByteArray(HASH_LENGTH)
        generate.generateBytes(password.toByteArray(StandardCharsets.UTF_8), result, 0, result.size)
        return result
    }

    fun toSHA512(source: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hash = digest.digest(source.toByteArray(StandardCharsets.UTF_8))
        return hash.fold(StringUtils.EMPTY_STRING) { str, it -> str + "%02x".format(it) }
    }

    fun toBase64(source: ByteArray) = Base64.encode(source)

    fun fromBase64(source: String) = Base64.decode(source)

    fun toGzip(source: String): String {
        val bytes = source.toByteArray(StandardCharsets.UTF_8)
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { it.write(bytes) }
        return toBase64(outputStream.toByteArray())
    }

    fun fromGzip(source: String): String {
        val bytes = fromBase64(source)
        val outputStream = ByteArrayOutputStream()
        bytes.inputStream().use { GZIPInputStream(it).copyTo(outputStream) }
        return outputStream.toString(StandardCharsets.UTF_8)
    }
}