package com.sijan.kharcha.data.backup

import com.sijan.kharcha.data.backup.model.BackupCategory
import com.sijan.kharcha.data.backup.model.BackupEntry
import com.sijan.kharcha.data.backup.model.BackupPayload
import com.sijan.kharcha.domain.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class JsonBackupSerializerTest {

    private val serializer = JsonBackupSerializer()

    @Test
    fun serializeAndDeserializeValidPayloadSucceeds() {
        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = 1718000000000L,
            categories = listOf(
                BackupCategory(id = 1L, name = "Food", type = EntryType.EXPENSE)
            ),
            entries = listOf(
                BackupEntry(
                    id = 10L,
                    amountPaisa = 15050L,
                    type = EntryType.EXPENSE,
                    categoryId = 1L,
                    note = "Lunch",
                    date = 1718000000000L,
                    source = "receipt",
                    transactionId = "TXN_12345"
                )
            )
        )

        val outputStream = ByteArrayOutputStream()
        val serializeResult = serializer.serialize(payload, outputStream)
        assertTrue(serializeResult.isSuccess)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val deserializeResult = serializer.deserialize(inputStream)
        assertTrue(deserializeResult.isSuccess)

        val restored = deserializeResult.getOrThrow()
        assertEquals(1, restored.schemaVersion)
        assertEquals(1, restored.categories.size)
        assertEquals("Food", restored.categories[0].name)
        assertEquals(15050L, restored.entries[0].amountPaisa)
        assertEquals("TXN_12345", restored.entries[0].transactionId)
    }

    @Test
    fun deserializeRejectsNewerSchemaVersion() {
        val futureJson = """
            {
                "schemaVersion": 99,
                "exportedAt": 1718000000000,
                "categories": [],
                "entries": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(futureJson.toByteArray())
        val result = serializer.deserialize(inputStream)

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup schema version") == true)
    }

    @Test
    fun deserializeRejectsInvalidOrNegativeAmount() {
        val invalidAmountJson = """
            {
                "schemaVersion": 1,
                "exportedAt": 1718000000000,
                "categories": [
                    { "id": 1, "name": "Food", "type": "EXPENSE" }
                ],
                "entries": [
                    {
                        "id": 1,
                        "amountPaisa": -500,
                        "type": "EXPENSE",
                        "categoryId": 1,
                        "note": "Invalid negative",
                        "date": 1000
                    }
                ]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(invalidAmountJson.toByteArray())
        val result = serializer.deserialize(inputStream)

        assertFalse(result.isSuccess)
    }

    @Test
    fun deserializeRejectsCorruptJson() {
        val corruptJson = "{ broken json"
        val inputStream = ByteArrayInputStream(corruptJson.toByteArray())
        val result = serializer.deserialize(inputStream)

        assertFalse(result.isSuccess)
    }

    @Test
    fun deserializeLegacyKharchaExportFormatSucceeds() {
        val legacyJson = """
            {
              "transactions": [
                {
                  "id": 108,
                  "type": "expense",
                  "amount": 320.00,
                  "category": "Food & Drinks",
                  "date": "2026-10-18",
                  "note": "momos",
                  "createdAt": 1792334769077,
                  "paymentMethod": "eSewa Wallet",
                  "remarks": "evening snack",
                  "transactionCode": "TXN_LEGACY_01"
                },
                {
                  "id": 109,
                  "type": "income",
                  "amount": 5000.50,
                  "category": "Salary",
                  "date": "2026-10-19",
                  "note": "Consulting",
                  "createdAt": 1792400000000,
                  "imagePath": "receipt_test.jpg"
                }
              ],
              "budgets": [],
              "savingsGoals": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(legacyJson.toByteArray())
        val result = serializer.deserialize(inputStream)

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertEquals(1, payload.schemaVersion)
        assertEquals(2, payload.entries.size)

        val entry1 = payload.entries[0]
        assertEquals(108L, entry1.id)
        assertEquals(32000L, entry1.amountPaisa)
        assertEquals(EntryType.EXPENSE, entry1.type)
        assertEquals("momos (evening snack)", entry1.note)
        assertEquals("TXN_LEGACY_01", entry1.transactionId)
        assertTrue(entry1.rawOcrText?.contains("paymentMethod: eSewa Wallet") == true)

        val entry2 = payload.entries[1]
        assertEquals(109L, entry2.id)
        assertEquals(500050L, entry2.amountPaisa)
        assertEquals(EntryType.INCOME, entry2.type)
        assertEquals("receipt_test.jpg", entry2.receiptPath)
        assertEquals("scan", entry2.source)
    }

    @Test
    fun deserializeRealLegacyExportFileSucceeds() {
        val file = java.io.File("finance_data_export_1792409222899.json")
        if (!file.exists()) return

        file.inputStream().use { stream ->
            val result = serializer.deserialize(stream)
            assertTrue(result.isSuccess)
            val payload = result.getOrThrow()
            assertEquals(106, payload.entries.size)
            assertTrue(payload.categories.isNotEmpty())
            assertTrue(payload.accounts.isNotEmpty())

            for (entry in payload.entries) {
                assertTrue(entry.amountPaisa > 0L)
                assertTrue(entry.id > 0L)
            }
        }
    }
}
