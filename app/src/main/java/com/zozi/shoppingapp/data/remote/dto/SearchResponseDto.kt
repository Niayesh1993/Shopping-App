package com.zozi.shoppingapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val products: List<ProductDto> = emptyList(),
    val currentPage: Int = 1,
    val totalResults: Int = 0,
    val pageCount: Int = 0,
)

@Serializable
data class ProductDto(
    val productId: Long? = null,
    val productName: String? = null,
    val reviewInformation: ReviewInformationDto? = null,
    @SerialName("USPs") val usps: List<String> = emptyList(),
    val availabilityState: Int? = null,
    val salesPriceIncVat: Double? = null,
    val listPriceIncVat: Double? = null,
    val productImage: String? = null,
    val promoIcon: PromoIconDto? = null,
    val nextDayDelivery: Boolean = false,
)

@Serializable
data class ReviewInformationDto(
    val reviewSummary: ReviewSummaryDto? = null,
)

@Serializable
data class ReviewSummaryDto(
    val reviewAverage: Double? = null,
    val reviewCount: Int? = null,
)

@Serializable
data class PromoIconDto(
    val text: String? = null,
    val type: String? = null,
)
