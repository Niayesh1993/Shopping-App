package com.zozi.shoppingapp.data.mapper

import com.zozi.shoppingapp.data.remote.dto.ProductDto
import com.zozi.shoppingapp.data.remote.dto.PromoIconDto
import com.zozi.shoppingapp.data.remote.dto.ReviewInformationDto
import com.zozi.shoppingapp.data.remote.dto.SearchResponseDto
import com.zozi.shoppingapp.domain.model.Availability
import com.zozi.shoppingapp.domain.model.Price
import com.zozi.shoppingapp.domain.model.Product
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.model.Promotion
import com.zozi.shoppingapp.domain.model.PromotionType
import com.zozi.shoppingapp.domain.model.Rating

fun SearchResponseDto.toDomain(): ProductPage = ProductPage(
    products = products.mapNotNull(ProductDto::toDomain),
    currentPage = currentPage,
    pageCount = pageCount,
    totalResults = totalResults,
)

/** Returns null for records the UI could not meaningfully render (no id, name or price). */
fun ProductDto.toDomain(): Product? {
    val id = productId ?: return null
    val name = productName?.takeIf { it.isNotBlank() } ?: return null
    val salesPrice = salesPriceIncVat ?: return null

    return Product(
        id = id,
        name = name.normaliseWhitespace(),
        imageUrl = productImage?.takeIf { it.isNotBlank() },
        usps = usps.filter { it.isNotBlank() }.map { it.normaliseWhitespace() },
        price = Price(salesIncVat = salesPrice, listIncVat = listPriceIncVat),
        rating = reviewInformation.toDomain(),
        availability = availabilityState.toAvailability(),
        hasNextDayDelivery = nextDayDelivery,
        promotion = promoIcon.toDomain(),
    )
}

private fun ReviewInformationDto?.toDomain(): Rating? {
    val summary = this?.reviewSummary ?: return null
    val average = summary.reviewAverage ?: return null
    return Rating(average = average, count = summary.reviewCount ?: 0)
}

private fun PromoIconDto?.toDomain(): Promotion? {
    val text = this?.text?.takeIf { it.isNotBlank() } ?: return null
    return Promotion(text = text.normaliseWhitespace(), type = type.toPromotionType())
}

private fun String?.toPromotionType(): PromotionType = when (this) {
    "coolblues-choice" -> PromotionType.COOLBLUES_CHOICE
    "action-price" -> PromotionType.ACTION_PRICE
    else -> PromotionType.OTHER
}

// The assignment payload exposes availability only as an opaque integer; 2 and 3 are the
// values present in the sample, the rest are mapped defensively.
private fun Int?.toAvailability(): Availability = when (this) {
    1 -> Availability.OUT_OF_STOCK
    2 -> Availability.IN_STOCK
    3 -> Availability.LIMITED_STOCK
    else -> Availability.UNKNOWN
}

private fun String.normaliseWhitespace(): String = replace(WHITESPACE_RUN, " ").trim()

private val WHITESPACE_RUN = Regex("\\s+")
