package com.zozi.shoppingapp.testing

import com.zozi.shoppingapp.di.RepositoryModule
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.coEvery
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Swaps the network-backed repository for a mock, so the instrumented tests exercise the real
 * ViewModel, use case and DI graph without touching the network.
 *
 * `@Provides` rather than `@Binds`: a mock is built by MockK, not by a constructor Hilt can call.
 * `@Singleton` keeps it the same instance the test injects and stubs; Hilt rebuilds the component
 * per test method, so every test starts with a clean mock.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideProductRepository(): ProductRepository = mockk {
        coEvery { searchProducts(query = null, page = any()) } returns
            Outcome.Success(LAUNCH_CATALOGUE)
    }

    /**
     * What the screen browses when it opens. The activity starts before a `@Before` method can stub
     * anything, so the mock has to answer that first query-less request out of the box. Tests that
     * need a different catalogue re-stub it themselves.
     */
    val LAUNCH_CATALOGUE: ProductPage = productPage(
        products = listOf(
            product(id = 101, name = "Sony WH-1000XM5 Zwart"),
            product(id = 102, name = "Garmin Forerunner 265"),
        ),
        currentPage = 1,
        pageCount = 1,
        totalResults = 2,
    )
}
