package org.beobma.prccore.resource

enum class ResourceType(
    val displayName: String,
    val customModelData: Int
) {
    Lithium("리튬", 5),
    Magnesium("마그네슘", 1),
    Nickel("니켈", 8),
    Platinum("백금", 7),
    Aluminum("알루미늄", 2),
    Gold("금", 6),
    Copper("구리", 4),
    Iron("철", 3),
    Titanium("티타늄", 9);
}
