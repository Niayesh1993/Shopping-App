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
    fun `maps every field of a complete product`() {
        val dto = completeDto()

        val product = dto.toDomain()!!

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
    fun `drops products without an id`() {
        assertNull(completeDto().copy(productId = null).toDomain())
    }

    @Test
    fun `drops products without a usable name`() {
        assertNull(completeDto().copy(productName = null).toDomain())
        assertNull(completeDto().copy(productName = "   ").toDomain())
    }

    @Test
    fun `drops products without a sales price`() {
        assertNull(completeDto().copy(salesPriceIncVat = null).toDomain())
    }

    @Test
    fun `carries the availability state through without interpreting it`() {
        fun availabilityFor(state: Int?) =
            completeDto().copy(availabilityState = state).toDomain()!!.availability

        // The API assigns no meaning to these codes, so the mapper must not invent one.
        assertEquals(Availability(1), availabilityFor(1))
        assertEquals(Availability(2), availabilityFor(2))
        assertEquals(Availability(3), availabilityFor(3))
        assertEquals(Availability(99), availabilityFor(99))
        assertEquals(Availability.UNKNOWN, availabilityFor(null))
    }

    @Test
    fun `treats a missing availability state as unknown`() {
        assertFalse(completeDto().copy(availabilityState = null).toDomain()!!.availability.isKnown)
        assertTrue(completeDto().copy(availabilityState = 2).toDomain()!!.availability.isKnown)
    }

    @Test
    fun `maps promotion types from the api vocabulary`() {
        fun promotionTypeFor(type: String?) = completeDto()
            .copy(promoIcon = PromoIconDto(text = "label", type = type))
            .toDomain()!!
            .promotion!!
            .type

        assertEquals(PromotionType.COOLBLUES_CHOICE, promotionTypeFor("coolblues-choice"))
        assertEquals(PromotionType.ACTION_PRICE, promotionTypeFor("action-price"))
        assertEquals(PromotionType.OTHER, promotionTypeFor("something-new"))
        assertEquals(PromotionType.OTHER, promotionTypeFor(null))
    }

    @Test
    fun `ignores a promotion without text`() {
        val dto = completeDto().copy(promoIcon = PromoIconDto(text = " ", type = "action-price"))

        assertNull(dto.toDomain()!!.promotion)
    }

    @Test
    fun `has no rating when the api omits the review summary`() {
        val dto = completeDto().copy(reviewInformation = ReviewInformationDto(reviewSummary = null))

        assertNull(dto.toDomain()!!.rating)
    }

    @Test
    fun `defaults a missing review count to zero`() {
        val dto = completeDto().copy(
            reviewInformation = ReviewInformationDto(
                reviewSummary = ReviewSummaryDto(reviewAverage = 8.0, reviewCount = null),
            ),
        )

        val rating = dto.toDomain()!!.rating!!
        assertEquals(0, rating.count)
        assertFalse(rating.hasReviews)
    }

    @Test
    fun `strips blank usps and collapses whitespace in text fields`() {
        val dto = completeDto().copy(
            productName = "Apple Watch Series 3 42mm Zilver\nAluminium",
            usps = listOf("Waterdicht tot 50 meter", "   ", ""),
        )

        val product = dto.toDomain()!!
        assertEquals("Apple Watch Series 3 42mm Zilver Aluminium", product.name)
        assertEquals(listOf("Waterdicht tot 50 meter"), product.usps)
    }

    @Test
    fun `maps page metadata and skips unmappable products`() {
        val response = SearchResponseDto(
            products = listOf(completeDto(), completeDto().copy(productId = null)),
            currentPage = 3,
            pageSize = 24,
            totalResults = 70,
            pageCount = 3,
        )

        val page = response.toDomain()

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
