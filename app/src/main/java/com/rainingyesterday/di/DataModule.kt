package com.rainingyesterday.di

import com.rainingyesterday.data.content.AssetContentRepository
import com.rainingyesterday.domain.repository.ContentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 数据层装配：把仓库接口绑定到 assets 实现（docs/01 §3 di/）。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindContentRepository(impl: AssetContentRepository): ContentRepository
}