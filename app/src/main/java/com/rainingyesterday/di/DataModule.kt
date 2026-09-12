package com.rainingyesterday.di

import android.content.Context
import com.rainingyesterday.data.content.AssetContentRepository
import com.rainingyesterday.data.save.PrefsSaveRepository
import com.rainingyesterday.domain.repository.ContentRepository
import com.rainingyesterday.domain.repository.SaveRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 数据层装配（docs/01 §3 di/）。 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}

/** 仓库绑定：接口 → assets 实现。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindContentRepository(impl: AssetContentRepository): ContentRepository

    @Binds
    abstract fun bindSaveRepository(impl: PrefsSaveRepository): SaveRepository
}