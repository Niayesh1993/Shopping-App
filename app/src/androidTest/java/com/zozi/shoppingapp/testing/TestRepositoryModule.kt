package com.zozi.shoppingapp.testing

import com.zozi.shoppingapp.di.RepositoryModule
import com.zozi.shoppingapp.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
abstract class TestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: FakeProductRepository): ProductRepository
}
