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
}
