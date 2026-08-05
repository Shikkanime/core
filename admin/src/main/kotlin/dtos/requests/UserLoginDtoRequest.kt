package fr.shikkanime.admin.dtos.requests

import fr.shikkanime.validator.NotBlank
import fr.shikkanime.validator.NotNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserLoginDtoRequest(
    @ProtoNumber(1)
    @NotNull("Username cannot be null.")
    @NotBlank("Username cannot be blank.")
    val username: String,
    @ProtoNumber(2)
    @NotNull("Password cannot be null.")
    @NotBlank("Password cannot be blank.")
    val password: String
)