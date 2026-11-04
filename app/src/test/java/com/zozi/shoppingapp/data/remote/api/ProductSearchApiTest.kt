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
            .addConverterFactory(
                Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(ProductSearchApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends the query and page as request parameters`() = runTest {
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        api.searchProducts(query = "apple tv", page = 2)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(
            "/mobile-assignment/search?query=apple%20tv&page=2",
            request.path,
        )
    }

    @Test
    fun `deserialises the assignment payload into domain products`() = runTest {
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        val page = api.searchProducts(query = "apple", page = 3).toDomain()

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
        assertEquals(Availability.IN_STOCK, iPhoneX.availability)
        assertTrue(iPhoneX.hasNextDayDelivery)
        assertNull(iPhoneX.promotion)
    }

    @Test
    fun `reads integral review averages and fractional prices`() = runTest {
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        val adapter = api.searchProducts(query = "apple", page = 1)
            .toDomain()
            .products
            .single { it.id == 232406L }

        assertEquals(9.0, adapter.rating!!.average, DELTA)
        assertEquals(24.99, adapter.price.salesIncVat, DELTA)
    }

    @Test
    fun `reads discounted products and promotion icons`() = runTest {
        server.enqueue(jsonResponse(readTestResource(SAMPLE_RESPONSE)))

        val products = api.searchProducts(query = "apple", page = 1).toDomain().products

        val discounted = products.single { it.id == 733904L }
        assertTrue(discounted.price.hasDiscount)
        assertEquals(626.0, discounted.price.listIncVat!!, DELTA)
        assertEquals(8, discounted.price.discountPercentage)
        assertEquals(PromotionType.ACTION_PRICE, discounted.promotion!!.type)

        val choice = products.single { it.id == 340051L }
        assertEquals(PromotionType.COOLBLUES_CHOICE, choice.promotion!!.type)
        assertEquals("Apple gebruikers", choice.promotion!!.text)

        val limited = products.single { it.id == 648326L }
        assertEquals(Availability.LIMITED_STOCK, limited.availability)
    }

    @Test
    fun `tolerates a payload without the products field`() = runTest {
        server.enqueue(jsonResponse("""{"currentPage":1,"pageCount":0,"totalResults":0}"""))

        val page = api.searchProducts(query = "nothing", page = 1).toDomain()

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
    }
}
