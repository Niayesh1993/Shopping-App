package com.zozi.shoppingapp.testing

import com.zozi.shoppingapp.di.RepositoryModule
import com.zozi.shoppingapp.domain.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
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
    fun provideProductRepository(): ProductRepository = mockk()
}
