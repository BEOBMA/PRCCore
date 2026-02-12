package org.beobma.prccore.plant.list

import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season

class WeedPlant : Plant("잡초", 9999, 9999, 1, 1, listOf(Season.Spring, Season.Summer, Season.Autumn, Season.Winter))