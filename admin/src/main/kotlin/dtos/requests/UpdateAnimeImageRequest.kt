package fr.shikkanime.admin.dtos.requests

import fr.shikkanime.validator.NotBlank
import fr.shikkanime.validator.NotNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Request replacing the artwork of an anime.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateAnimeImageRequest(
    @ProtoNumber(1)
    @NotNull("Url cannot be null.")
    @NotBlank("Url cannot be blank.")
    val url: String
)
