package com.zozi.shoppingapp.data.remote.api

import com.zozi.shoppingapp.data.mapper.toDomain
import com.zozi.shoppingapp.domain.model.Availability
import com.zozi.shoppingapp.domain.model.PromotionType
import com.zozi.shoppingapp.testing.readTestResource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.HttpURLConnection

class ProductSearchApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ProductSearchApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/mobile-assignment/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProductSearchApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `given a query and a page when calling the endpoint then both are sent as request parameters`() = runTest {
        // Given
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        // When
        api.searchProducts(query = "apple tv", page = 2)

        // Then
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/mobile-assignment/search?query=apple%20tv&page=2", request.path)
    }

    @Test
    fun `given no query when calling the endpoint then the parameter is left out of the url`() = runTest {
        // Given
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        // When the screen asks to browse rather than to search
        api.searchProducts(query = null, page = 1)

        // Then an absent parameter is sent, not an empty one, so the endpoint sees no filter at all
        val url = server.takeRequest().requestUrl!!
        assertEquals("/mobile-assignment/search?page=1", url.encodedPath + "?" + url.encodedQuery)
        assertEquals(setOf("page"), url.queryParameterNames)
    }

    @Test
    fun `given a search when calling the endpoint then no parameters beyond query and page are sent`() = runTest {
        // Given
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        // When
        api.searchProducts(query = "apple", page = 1)

        // Then the endpoint accepts nothing else, so sending more would be guesswork
        val url = server.takeRequest().requestUrl!!
        assertEquals(setOf("query", "page"), url.queryParameterNames)
        assertEquals("apple", url.queryParameter("query"))
        assertEquals("1", url.queryParameter("page"))
    }

    @Test
    fun `given the assignment payload when deserialising it then it becomes domain products`() = runTest {
        // Given
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        // When
        val page = api.searchProducts(query = "apple", page = 3).toDomain()

        // Then
        assertEquals(5, page.products.size)
        assertEquals(3, page.currentPage)
        assertEquals(3, page.pageCount)
        assertEquals(70, page.totalResults)

        val iPhoneX = page.products.first()
        assertEquals(793652L, iPhoneX.id)
        assertEquals("Apple iPhone X 256GB Zilver", iPhoneX.name)
        assertEquals(1279.0, iPhoneX.price.salesIncVat, DELTA)
        assertEquals(9.2, iPhoneX.rating!!.average, DELTA)
        assertEquals(209, iPhoneX.rating!!.count)
        assertEquals(3, iPhoneX.usps.size)
        assertEquals(Availability(2), iPhoneX.availability)
        assertTrue(iPhoneX.hasNextDayDelivery)
        assertNull(iPhoneX.promotion)
    }

    @Test
    fun `given an integral average and a fractional price when deserialising then both read as doubles`() = runTest {
        // Given
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        // When
        val adapter = api.searchProducts(query = "apple", page = 1)
            .toDomain()
            .products
            .single { it.id == 232406L }

        // Then
        assertEquals(9.0, adapter.rating!!.average, DELTA)
        assertEquals(24.99, adapter.price.salesIncVat, DELTA)
    }

    @Test
    fun `given discounts and promo icons in the payload when deserialising then they reach the products`() = runTest {
        // Given
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        // When
        val products = api.searchProducts(query = "apple", page = 1).toDomain().products

        // Then
        val discounted = products.single { it.id == 733904L }
        assertTrue(discounted.price.hasDiscount)
        assertEquals(626.0, discounted.price.listIncVat!!, DELTA)
        assertEquals(8, discounted.price.discountPercentage)
        assertEquals(PromotionType.ACTION_PRICE, discounted.promotion!!.type)

        val choice = products.single { it.id == 340051L }
        assertEquals(PromotionType.COOLBLUES_CHOICE, choice.promotion!!.type)
        assertEquals("Apple gebruikers", choice.promotion!!.text)

        val limited = products.single { it.id == 648326L }
        assertEquals(Availability(3), limited.availability)
    }

    @Test
    fun `given a payload without the products field when deserialising then the page is simply empty`() = runTest {
        // Given
        server.enqueue(jsonResponse("""{"currentPage":1,"pageCount":0,"totalResults":0}"""))

        // When
        val page = api.searchProducts(query = "nothing", page = 1).toDomain()

        // Then
        assertTrue(page.products.isEmpty())
        assertEquals(0, page.totalResults)
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val SAMPLE_RESPONSE = "search_response.json"
        const val DELTA = 0.001

        /** Mirrors the production converter, so the payload is parsed exactly as the app does. */
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}
