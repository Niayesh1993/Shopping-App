package com.zozi.shoppingapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductTest {

    @Test
    fun `given a price without a list price when reading the discount then there is none`() {
        // Given
        val price = Price(salesIncVat = 1279.0)

        // When
        val percentage = price.discountPercentage

        // Then
        assertFalse(price.hasDiscount)
        assertNull(percentage)
    }

    @Test
    fun `given a list price above the sales price when reading the discount then it is a whole percentage`() {
        // Given
        val price = Price(salesIncVat = 575.0, listIncVat = 626.0)

        // When
        val percentage = price.discountPercentage

        // Then
        assertTrue(price.hasDiscount)
        assertEquals(8, percentage)
    }

    @Test
    fun `given a list price that is not higher when reading the discount then there is none`() {
        // Given
        val equal = Price(salesIncVat = 575.0, listIncVat = 575.0)
        val lower = Price(salesIncVat = 575.0, listIncVat = 500.0)

        // When / Then: a list price that is not above the sales price is not a discount
        assertFalse(equal.hasDiscount)
        assertFalse(lower.hasDiscount)
    }

    @Test
    fun `given a rating when reading hasReviews then it follows the review count`() {
        // Given
        val unreviewed = Rating(average = 0.0, count = 0)
        val reviewed = Rating(average = 9.2, count = 209)

        // When / Then
        assertFalse(unreviewed.hasReviews)
        assertTrue(reviewed.hasReviews)
    }

    @Test
    fun `given a page before the last when reading hasNextPage then more results are offered`() {
        // Given / When / Then: of three pages, only the third stops the pagination
        assertTrue(page(currentPage = 1, pageCount = 3).hasNextPage)
        assertTrue(page(currentPage = 2, pageCount = 3).hasNextPage)
        assertFalse(page(currentPage = 3, pageCount = 3).hasNextPage)
    }

    @Test
    fun `given the empty page when reading it then it holds no products and no further pages`() {
        // Given
        val page = ProductPage.EMPTY

        // When / Then
        assertTrue(page.products.isEmpty())
        assertFalse(page.hasNextPage)
    }

    private fun page(currentPage: Int, pageCount: Int) = ProductPage(
        products = emptyList(),
        currentPage = currentPage,
        pageCount = pageCount,
        totalResults = 70,
    )
}
