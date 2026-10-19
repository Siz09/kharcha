package com.sijan.kharcha.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import com.sijan.kharcha.data.storage.ReceiptStorage
import com.sijan.kharcha.data.storage.ReceiptStorageImpl
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.parser.EsewaParser
import com.sijan.kharcha.domain.parser.ReceiptOcrScanner
import com.sijan.kharcha.domain.parser.ReceiptProcessingPipeline
import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfirmScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var storage: ReceiptStorage
    private lateinit var pipeline: ReceiptProcessingPipeline

    private val sampleEsewaOcr = """
        eSewa
        Payment Successful
        Amount: Rs. 1,500.00
        To: Nepal Electricity Authority
        Transaction ID: TXN_ESEWA_998877
        Date: 2026-03-15 14:30:00
        Service Fee: Rs. 5.00
    """.trimIndent()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao(), db.billDao())
        storage = ReceiptStorageImpl(context)
        pipeline = ReceiptProcessingPipeline(setOf(EsewaParser()))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun createTestImageFile(): Uri {
        val testFile = File(context.cacheDir, "test_receipt_${System.currentTimeMillis()}.jpg")
        FileOutputStream(testFile).use { it.write("fake_image_content".toByteArray()) }
        return Uri.fromFile(testFile)
    }

    @Test
    fun processImagePopulatesFieldsAndEnablesSave() = runTest(testDispatcher) {
        val imageUri = createTestImageFile()
        val fakeScanner = object : ReceiptOcrScanner {
            override suspend fun scanImage(uri: Uri): Result<OcrResult> {
                return Result.success(OcrResult(fullText = sampleEsewaOcr))
            }
        }

        val viewModel = ConfirmScanViewModel(
            ledgerRepository = repository,
            receiptStorage = storage,
            pipeline = pipeline,
            ocrScanner = fakeScanner,
            savedStateHandle = SavedStateHandle(),
            context = context
        )

        // Await categories loaded
        val loadedCategoriesState = viewModel.uiState.first { it.categories.isNotEmpty() }
        assertNotNull(loadedCategoriesState.selectedCategoryId)

        viewModel.processImage(imageUri)
        val state = viewModel.uiState.first { !it.isLoading && it.amountText.isNotEmpty() }

        assertFalse(state.isLoading)
        assertEquals("1500", state.amountText)
        assertEquals(ValidationStatus.VALID, state.amountStatus)
        assertEquals("TXN_ESEWA_998877", state.transactionId)
        assertEquals("Nepal Electricity Authority", state.merchant)
        assertNotNull(state.receiptPath)
        assertNotNull(state.rawOcrText)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun duplicateTransactionBlocksSaveUntilDismissed() = runTest(testDispatcher) {
        val imageUri = createTestImageFile()
        val fakeScanner = object : ReceiptOcrScanner {
            override suspend fun scanImage(uri: Uri): Result<OcrResult> {
                return Result.success(OcrResult(fullText = sampleEsewaOcr))
            }
        }

        val viewModel = ConfirmScanViewModel(
            ledgerRepository = repository,
            receiptStorage = storage,
            pipeline = pipeline,
            ocrScanner = fakeScanner,
            savedStateHandle = SavedStateHandle(),
            context = context
        )

        val loadedCategoriesState = viewModel.uiState.first { it.categories.isNotEmpty() }
        val catId = loadedCategoriesState.selectedCategoryId!!

        val existingEntry = LedgerEntry(
            amountPaisa = 150000L,
            type = EntryType.EXPENSE,
            categoryId = catId,
            date = System.currentTimeMillis(),
            transactionId = "TXN_ESEWA_998877"
        )
        val existingId = repository.saveEntry(existingEntry).getOrThrow()

        viewModel.processImage(imageUri)
        val state = viewModel.uiState.first { !it.isLoading && it.amountText.isNotEmpty() }

        assertNotNull(state.duplicateEntry)
        assertEquals(existingId, state.duplicateEntry?.id)
        assertFalse(state.isDuplicateDismissed)
        assertFalse(state.isSaveEnabled) // Save must be blocked by duplicate warning

        // Test View Existing navigation event
        val navDeferred = async { viewModel.events.first() }
        viewModel.onViewExistingDuplicate()
        val navEvent = navDeferred.await()
        assertTrue(navEvent is ConfirmScanEvent.NavigateToEntry)
        assertEquals(existingId, (navEvent as ConfirmScanEvent.NavigateToEntry).entryId)

        // User taps "Save anyway"
        viewModel.onDismissDuplicateWarning()
        val warningDismissedState = viewModel.uiState.first { it.isDuplicateDismissed }
        assertTrue(warningDismissedState.isDuplicateDismissed)
        assertTrue(warningDismissedState.isSaveEnabled)

        // Save entry
        val saveDeferred = async { viewModel.events.first() }
        viewModel.saveEntry()
        val saveEvent = saveDeferred.await()
        assertEquals(ConfirmScanEvent.SaveSuccess, saveEvent)

        val allEntries = repository.getAllEntries().first()
        // Duplicate was NOT auto-merged! Both entries exist:
        assertEquals(2, allEntries.size)
    }

    @Test
    fun saveEntryStoresRawOcrTextAndReceiptPath() = runTest(testDispatcher) {
        val imageUri = createTestImageFile()
        val fakeScanner = object : ReceiptOcrScanner {
            override suspend fun scanImage(uri: Uri): Result<OcrResult> {
                return Result.success(OcrResult(fullText = sampleEsewaOcr))
            }
        }

        val viewModel = ConfirmScanViewModel(
            ledgerRepository = repository,
            receiptStorage = storage,
            pipeline = pipeline,
            ocrScanner = fakeScanner,
            savedStateHandle = SavedStateHandle(),
            context = context
        )

        viewModel.uiState.first { it.categories.isNotEmpty() }

        viewModel.processImage(imageUri)
        viewModel.uiState.first { !it.isLoading && it.amountText.isNotEmpty() }

        val saveDeferred = async { viewModel.events.first() }
        viewModel.saveEntry()
        val saveEvent = saveDeferred.await()
        assertEquals(ConfirmScanEvent.SaveSuccess, saveEvent)

        val entries = repository.getAllEntries().first()
        assertEquals(1, entries.size)
        val saved = entries[0]
        assertEquals(150000L, saved.amountPaisa)
        assertEquals("receipt:esewa", saved.source)
        assertEquals(sampleEsewaOcr, saved.rawOcrText)
        assertNotNull(saved.receiptPath)
        assertTrue(File(saved.receiptPath!!).exists())
    }
}
