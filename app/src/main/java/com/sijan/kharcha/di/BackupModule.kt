package com.sijan.kharcha.di

import com.sijan.kharcha.data.backup.BackupService
import com.sijan.kharcha.data.backup.BackupServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupService(
        backupServiceImpl: BackupServiceImpl
    ): BackupService
}
