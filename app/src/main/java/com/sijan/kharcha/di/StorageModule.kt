package com.sijan.kharcha.di

import com.sijan.kharcha.data.storage.ReceiptStorage
import com.sijan.kharcha.data.storage.ReceiptStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindReceiptStorage(
        impl: ReceiptStorageImpl
    ): ReceiptStorage
}
