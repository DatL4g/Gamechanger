package dev.datlag.gamechanger.game.model.steam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryConfig(
    @SerialName("path") val path: String
)
