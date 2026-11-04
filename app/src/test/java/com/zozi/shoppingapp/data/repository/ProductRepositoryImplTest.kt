package com.zozi.shoppingapp.data.repository

import com.zozi.shoppingapp.data.remote.api.ProductSearchApi
import com.zozi.shoppingapp.data.remote.dto.ProductDto
import com.zozi.shoppingapp.data.remote.dto.SearchResponseDto
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.result.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

class ProductRepositoryImplTest {

    private val api: ProductSearchApi = mockk()
    private val repository = ProductRepositoryImpl(
        api = api,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `returns mapped products on success`() = runTest {
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

        val outcome = repository.searchProducts("apple", 1)

        val page = (outcome as Outcome.Success).data
        assertEquals(1, page.products.size)
        assertEquals("Apple TV 4K 64GB", page.products.first().name)
        assertEquals(70, page.totalResults)
        assertEquals(true, page.hasNextPage)
    }

    @Test
    fun `forwards the query and page to the api`() = runTest {
        coEvery { api.searchProducts(any(), any()) } returns SearchResponseDto()

        repository.searchProducts("apple watch", 4)

        coVerify(exactly = 1) { api.searchProducts(query = "apple watch", page = 4) }
    }

    @Test
    fun `translates transport failures into domain errors`() = runTest {
        coEvery { api.searchProducts(any(), any()) } throws UnknownHostException()

        val outcome = repository.searchProducts("apple", 1)

        assertEquals(Outcome.Failure(AppError.NoConnection), outcome)
    }

    @Test
    fun `translates http failures into domain errors`() = runTest {
        coEvery { api.searchProducts(any(), any()) } throws HttpException(
            Response.error<Unit>(503, "".toResponseBody("application/json".toMediaType())),
        )

        val outcome = repository.searchProducts("apple", 1)

        assertEquals(Outcome.Failure(AppError.Server(503)), outcome)
    }

    @Test
    fun `does not leak exceptions to the caller`() = runTest {
        coEvery { api.searchProducts(any(), any()) } throws IllegalStateException("boom")

        val outcome = repository.searchProducts("apple", 1)

        assertEquals(Outcome.Failure(AppError.Unknown("boom")), outcome)
    }
}
