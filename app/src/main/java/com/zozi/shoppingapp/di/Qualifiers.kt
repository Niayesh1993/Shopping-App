package com.zozi.shoppingapp.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchBaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchApiKey
