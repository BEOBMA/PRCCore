package org.beobma.prccore.mine

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.beobma.prccore.data.LocationSerializer
import org.beobma.prccore.entity.Enemy
import org.beobma.prccore.resource.Resource
import org.bukkit.Location
import org.bukkit.entity.Player

@Serializable
data class Mine(
    val floor: Int,
    val mineTemplate: MineTemplate,
    val mineType: MineType,
    val resources: MutableList<Resource> = mutableListOf(),
    val enemys: MutableList<Enemy> = mutableListOf(),
    val players: MutableList<Player> = mutableListOf(),
    @Serializable(with = LocationSerializer::class)
    var startBlockLocation: Location? = null,
    var startBlockUUID: String? = null,
    var startBlockMarker: String? = null,
    @Serializable(with = LocationSerializer::class)
    var exitBlockLocation: Location? = null,
    var exitBlockUUID: String? = null,
    var exitBlockMarker: String? = null,
    @Transient
    var visualsSpawned: Boolean = false
)