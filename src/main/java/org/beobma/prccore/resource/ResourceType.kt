package org.beobma.prccore.resource

import kr.eme.prcShop.api.PRCItem
import kr.eme.prcShop.api.PRCItems

enum class ResourceType(
    val prcItem: PRCItem
) {
    Lithium(PRCItems.LITHIUM),
    Magnesium(PRCItems.MAGNESIUM),
    Nickel(PRCItems.NICKEL),
    Platinum(PRCItems.PLATINUM),
    Aluminum(PRCItems.ALUMINUM),
    Gold(PRCItems.GOLD),
    Copper(PRCItems.COPPER),
    Iron(PRCItems.IRON),
    Titanium(PRCItems.TITANIUM);
}
