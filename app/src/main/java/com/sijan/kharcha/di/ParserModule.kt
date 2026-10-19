package com.sijan.kharcha.di

import com.sijan.kharcha.data.ocr.MlKitReceiptOcrScanner
import com.sijan.kharcha.domain.parser.EsewaParser
import com.sijan.kharcha.domain.parser.ReceiptOcrScanner
import com.sijan.kharcha.domain.parser.ReceiptParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @IntoSet
    abstract fun bindEsewaParser(
        parser: EsewaParser
    ): ReceiptParser

    @Binds
    @Singleton
    abstract fun bindReceiptOcrScanner(
        scanner: MlKitReceiptOcrScanner
    ): ReceiptOcrScanner
}
