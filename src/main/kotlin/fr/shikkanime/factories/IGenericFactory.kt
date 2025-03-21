package fr.shikkanime.factories

interface IGenericFactory<E, D> {
    fun toDto(entity: E): D

    fun toEntity(dto: D): E {
        TODO("Not yet implemented")
    }
}