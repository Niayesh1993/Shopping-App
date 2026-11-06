package com.zozi.shoppingapp.data.repository

import com.zozi.shoppingapp.data.remote.api.ProductSearchApi
import com.zozi.shoppingapp.data.remote.dto.ProductDto
import com.zozi.shoppingapp.data.remote.dto.SearchResponseDto
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.result.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryImplTest {

    private val api: ProductSearchApi = mockk()
    private val repository = ProductRepositoryImpl(
        api = api,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `given the api answers when searching then the response is mapped to a domain page`() = runTest {
        // Given
        coEvery { api.searchProducts(any(), any()) } returns SearchResponseDto(
            products = listOf(
                ProductDto(
                    productId = 1,
                    productName = "Apple TV 4K 64GB",
                    salesPriceIncVat = 219.0,
                    availabilityState = 2,
                ),
            ),
            currentPage = 1,
            totalResults = 70,
            pageCount = 3,
        )

        // When
        val outcome = repository.searchProducts("apple", 1)

        // Then
        val page = (outcome as Outcome.Success).data
        assertEquals(1, page.products.size)
        assertEquals("Apple TV 4K 64GB", page.products.first().name)
        assertEquals(70, page.totalResults)
        assertEquals(true, page.hasNextPage)
    }

    @Test
    fun `given a query and a page when searching then both are forwarded to the api unchanged`() = runTest {
        // Given
        coEvery { api.searchProducts(any(), any()) } returns SearchResponseDto()

        // When
        repository.searchProducts("apple watch", 4)

        // Then
        coVerify(exactly = 1) { api.searchProducts(query = "apple watch", page = 4) }
    }

    @Test
    fun `given no query when browsing then the api is called without one`() = runTest {
        // Given
        coEvery { api.searchProducts(any(), any()) } returns SearchResponseDto()

        // When
        repository.searchProducts(query = null, page = 1)

        // Then the repository forwards the absence rather than substituting an empty string
        coVerify(exactly = 1) { api.searchProducts(query = null, page = 1) }
    }

    @Test
    fun `given the api throws a transport failure when searching then it becomes a domain error`() = runTest {
        // Given
        coEvery { api.searchProducts(any(), any()) } throws UnknownHostException()

        // When
        val outcome = repository.searchProducts("apple", 1)

        // Then
        assertEquals(Outcome.Failure(AppError.NoConnection), outcome)
    }

    @Test
    fun `given the api throws an http failure when searching then it becomes a domain error`() = runTest {
        // Given
        coEvery { api.searchProducts(any(), any()) } throws HttpException(
            Response.error<Unit>(503, "".toResponseBody("application/json".toMediaType())),
        )

        // When
        val outcome = repository.searchProducts("apple", 1)

        // Then
        assertEquals(Outcome.Failure(AppError.Server(503)), outcome)
    }

    @Test
    fun `given the api throws anything else when searching then no exception reaches the caller`() = runTest {
        // Given
        coEvery { api.searchProducts(any(), any()) } throws IllegalStateException("boom")

        // When
        val outcome = repository.searchProducts("apple", 1)

        // Then
        assertEquals(Outcome.Failure(AppError.Unknown("boom")), outcome)
    }
}
