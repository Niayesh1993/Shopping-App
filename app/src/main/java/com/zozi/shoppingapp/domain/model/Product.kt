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

/**
 * The raw `availabilityState` from the API.
 *
 * The field is about availability, but the API gives no meaning to the individual codes, so the
 * app deliberately infers no stock level from them and carries the value through verbatim. If the
 * codes are ever specified, this is the one place that has to learn them.
 */
@JvmInline
value class Availability(val state: Int?) {

    val isKnown: Boolean
        get() = state != null

    companion object {
        val UNKNOWN = Availability(null)
    }
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
