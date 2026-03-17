package org.beobma.prccore.plant

import kotlinx.serialization.Serializable
import org.beobma.prccore.tool.CapsuleType

@Serializable
data class PlantStatus(
    var isPlant: Boolean = false,
    var isHarvestComplete: Boolean = false,
    var isWeeds: Boolean = false,
    var wateredToday: Boolean = false,
    var weedsCount: Int = 0,
    var capsuleType: CapsuleType = CapsuleType.None,
    var isDeadGrass: Boolean = false
)
