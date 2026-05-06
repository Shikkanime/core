package fr.shikkanime.factories

interface IGenericFactory<E, D> {
    suspend fun toDto(entity: E): D

    suspend fun toEntity(dto: D): E {
        TODO("Not yet implemented")
    }
}