package org.beobma.prccore.plant


import kotlinx.serialization.Serializable
import org.beobma.prccore.data.LocationSerializer
import org.beobma.prccore.util.Season
import org.bukkit.Location

@Serializable
open class Plant(
    val name: String,
    var remainingGrowthDays: Int,
    val growthDays: Int,
    val harvestAmountMin: Int,
    val harvestAmountMax: Int,
    val growableSeasons: List<Season>,
    @Serializable(with = LocationSerializer::class)
    var farmlandLocation: Location? = null,
    val plantStatus: PlantStatus = PlantStatus(),
    var uuidString: String? = null,
    var quality: Int? = null
)