package com.zozi.shoppingapp.domain.model

import kotlin.math.roundToInt

data class Product(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val usps: List<String>,
    val price: Price,
    val rating: Rating?,
    val availability: Availability,
    val hasNextDayDelivery: Boolean,
    val promotion: Promotion?,
)

data class Price(
    val salesIncVat: Double,
    val listIncVat: Double? = null,
) {
    val hasDiscount: Boolean
        get() = listIncVat != null && listIncVat > salesIncVat

    val discountPercentage: Int?
        get() = listIncVat
            ?.takeIf { hasDiscount }
            ?.let { (((it - salesIncVat) / it) * 100).roundToInt() }
}

data class Rating(
    val average: Double,
    val count: Int,
) {
    val hasReviews: Boolean
        get() = count > 0
}

enum class Availability {
    IN_STOCK,
    LIMITED_STOCK,
    OUT_OF_STOCK,
    UNKNOWN,
}

data class Promotion(
    val text: String,
    val type: PromotionType,
)

enum class PromotionType {
    COOLBLUES_CHOICE,
    ACTION_PRICE,
    OTHER,
}
