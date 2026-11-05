package com.zozi.shoppingapp.data.mapper

import com.zozi.shoppingapp.data.remote.dto.ProductDto
import com.zozi.shoppingapp.data.remote.dto.PromoIconDto
import com.zozi.shoppingapp.data.remote.dto.ReviewInformationDto
import com.zozi.shoppingapp.data.remote.dto.ReviewSummaryDto
import com.zozi.shoppingapp.data.remote.dto.SearchResponseDto
import com.zozi.shoppingapp.domain.model.Availability
import com.zozi.shoppingapp.domain.model.PromotionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMapperTest {

    @Test
    fun `given a complete dto when mapping it then every field reaches the domain model`() {
        // Given
        val dto = completeDto()

        // When
        val product = dto.toDomain()!!

        // Then
        assertEquals(793652L, product.id)
        assertEquals("Apple iPhone X 256GB Zilver", product.name)
        assertEquals("https://image.coolblue.nl/300x750/products/984921", product.imageUrl)
        assertEquals(listOf("256 GB opslagcapaciteit", "iOS 11"), product.usps)
        assertEquals(1279.0, product.price.salesIncVat, DELTA)
        assertEquals(9.2, product.rating!!.average, DELTA)
        assertEquals(209, product.rating!!.count)
        assertEquals(Availability(2), product.availability)
        assertTrue(product.hasNextDayDelivery)
    }

    @Test
    fun `given a dto without an id when mapping it then the product is dropped`() {
        // Given
        val dto = completeDto().copy(productId = null)

        // When
        val product = dto.toDomain()

        // Then
        assertNull(product)
    }

    @Test
    fun `given a dto without a usable name when mapping it then the product is dropped`() {
        // Given / When / Then: both a missing and a blank name are unusable
        assertNull(completeDto().copy(productName = null).toDomain())
        assertNull(completeDto().copy(productName = "   ").toDomain())
    }

    @Test
    fun `given a dto without a sales price when mapping it then the product is dropped`() {
        // Given
        val dto = completeDto().copy(salesPriceIncVat = null)

        // When
        val product = dto.toDomain()

        // Then
        assertNull(product)
    }

    @Test
    fun `given any availability code when mapping it then it is carried through uninterpreted`() {
        // Given
        fun availabilityFor(state: Int?) =
            completeDto().copy(availabilityState = state).toDomain()!!.availability

        // When / Then: the API assigns no meaning to these codes, so the mapper must not invent one
        assertEquals(Availability(1), availabilityFor(1))
        assertEquals(Availability(2), availabilityFor(2))
        assertEquals(Availability(3), availabilityFor(3))
        assertEquals(Availability(99), availabilityFor(99))
        assertEquals(Availability.UNKNOWN, availabilityFor(null))
    }

    @Test
    fun `given a promo icon when mapping it then the type comes from the api vocabulary`() {
        // Given
        fun promotionTypeFor(type: String?) = completeDto()
            .copy(promoIcon = PromoIconDto(text = "label", type = type))
            .toDomain()!!
            .promotion!!
            .type

        // When / Then: anything outside the known vocabulary still renders, as OTHER
        assertEquals(PromotionType.COOLBLUES_CHOICE, promotionTypeFor("coolblues-choice"))
        assertEquals(PromotionType.ACTION_PRICE, promotionTypeFor("action-price"))
        assertEquals(PromotionType.OTHER, promotionTypeFor("something-new"))
        assertEquals(PromotionType.OTHER, promotionTypeFor(null))
    }

    @Test
    fun `given a promo icon without text when mapping it then there is no promotion to show`() {
        // Given
        val dto = completeDto().copy(promoIcon = PromoIconDto(text = " ", type = "action-price"))

        // When
        val promotion = dto.toDomain()!!.promotion

        // Then
        assertNull(promotion)
    }

    @Test
    fun `given a dto without a review summary when mapping it then there is no rating`() {
        // Given
        val dto = completeDto().copy(reviewInformation = ReviewInformationDto(reviewSummary = null))

        // When
        val rating = dto.toDomain()!!.rating

        // Then
        assertNull(rating)
    }

    @Test
    fun `given a review summary without a count when mapping it then the count defaults to zero`() {
        // Given
        val dto = completeDto().copy(
            reviewInformation = ReviewInformationDto(
                reviewSummary = ReviewSummaryDto(reviewAverage = 8.0, reviewCount = null),
            ),
        )

        // When
        val rating = dto.toDomain()!!.rating!!

        // Then
        assertEquals(0, rating.count)
        assertFalse(rating.hasReviews)
    }

    @Test
    fun `given text with blanks and newlines when mapping it then whitespace is collapsed and blanks dropped`() {
        // Given
        val dto = completeDto().copy(
            productName = "Apple Watch Series 3 42mm Zilver\nAluminium",
            usps = listOf("Waterdicht tot 50 meter", "   ", ""),
        )

        // When
        val product = dto.toDomain()!!

        // Then
        assertEquals("Apple Watch Series 3 42mm Zilver Aluminium", product.name)
        assertEquals(listOf("Waterdicht tot 50 meter"), product.usps)
    }

    @Test
    fun `given a response with an unmappable product when mapping it then it is skipped and metadata kept`() {
        // Given
        val response = SearchResponseDto(
            products = listOf(completeDto(), completeDto().copy(productId = null)),
            currentPage = 3,
            totalResults = 70,
            pageCount = 3,
        )

        // When
        val page = response.toDomain()

        // Then one product survives, and the paging metadata is untouched by the drop
        assertEquals(1, page.products.size)
        assertEquals(3, page.currentPage)
        assertEquals(3, page.pageCount)
        assertEquals(70, page.totalResults)
        assertFalse(page.hasNextPage)
    }

    private fun completeDto() = ProductDto(
        productId = 793652,
        productName = "Apple iPhone X 256GB Zilver",
        reviewInformation = ReviewInformationDto(
            reviewSummary = ReviewSummaryDto(reviewAverage = 9.2, reviewCount = 209),
        ),
        usps = listOf("256 GB opslagcapaciteit", "iOS 11"),
        availabilityState = 2,
        salesPriceIncVat = 1279.0,
        productImage = "https://image.coolblue.nl/300x750/products/984921",
        nextDayDelivery = true,
    )

    private companion object {
        const val DELTA = 0.001
    }
}
