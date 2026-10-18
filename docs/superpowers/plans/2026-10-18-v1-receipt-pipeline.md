# Kharcha v1: Milestone 4 — Receipt Capture & Parser Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the on-device receipt processing pipeline: OCR data models, numeral & text Normalizer, financial Validator, ReceiptStorage for app-private images, EsewaParser implementation, and fixture accuracy test suite.

**Architecture:** Pure Kotlin domain layer for parsing and validation (no Android dependencies, ultra-fast unit testing), Repository & Storage services in data layer, DI via Hilt.

**Spec:** `docs/TRD.md` Section 4 & 7, `docs/DATA_LAYER_STRUCTURE.md` Section 5 & 7, `AGENTS.md` (Rule 1, 4, 7, 11).

## Global Constraints
- Money is strictly `Long` in paisa. No `Double` or `Float` in parsing, normalization, or domain models.
- Dates stored as UTC epoch milliseconds (`Long`).
- Parser rules for eSewa mark unknown fields with `TODO(parser-spec)` per Rule 11.
- All receipt images stored in app-private directory (`files/receipts/`) with random UUID filenames.
- 100% offline — zero network permissions or external logging.

---

### Task 1: Domain OCR Models, Normalizer, and Validator

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/model/OcrResult.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/model/ParseResult.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/model/ValidationStatus.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/Normalizer.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/Validator.kt`
- Test: `app/src/test/java/com/sijan/kharcha/domain/parser/NormalizerTest.kt`
- Test: `app/src/test/java/com/sijan/kharcha/domain/parser/ValidatorTest.kt`

**Interfaces:**
- Produces: `Normalizer.toAsciiDigits(text: String): String`
- Produces: `Normalizer.cleanNumericString(text: String): String`
- Produces: `Normalizer.parsePaisa(amountText: String): Long?`
- Produces: `Validator.validateAmount(paisa: Long?): ParsedField<Long>`
- Produces: `Validator.validateDate(epochMillis: Long?): ParsedField<Long>`
- Produces: `Validator.validateTransactionId(id: String?): ParsedField<String>`

- [x] **Step 1: Write failing unit tests for `NormalizerTest` and `ValidatorTest` (RED)**
- [x] **Step 2: Implement OCR and parse domain models (`OcrResult`, `ParseResult`, `ParsedField`)**
- [x] **Step 3: Implement `Normalizer` (Devanagari numerals, currency stripping, lookalikes)**
- [x] **Step 4: Implement `Validator` (strict paisa > 0, date sanity, transaction pattern)**
- [x] **Step 5: Run tests to confirm green (GREEN)**
- [x] **Step 6: Commit Task 1**

---

### Task 2: Receipt Image Storage Service

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/storage/ReceiptStorage.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/storage/ReceiptStorageImpl.kt`
- Create: `app/src/main/java/com/sijan/kharcha/di/StorageModule.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/storage/ReceiptStorageTest.kt`

**Interfaces:**
- Produces: `ReceiptStorage.saveImage(inputStream: InputStream): Result<String>` (returns absolute file path)
- Produces: `ReceiptStorage.getImageUri(filePath: String): Uri` (via FileProvider)
- Produces: `ReceiptStorage.deleteImage(filePath: String): Boolean`

- [x] **Step 1: Write failing unit test for `ReceiptStorageTest` (save, retrieve URI, delete) (RED)**
- [x] **Step 2: Implement `ReceiptStorage` and `ReceiptStorageImpl` in app-private files directory**
- [x] **Step 3: Wire Hilt bindings in `StorageModule`**
- [x] **Step 4: Run test to confirm green (GREEN)**
- [x] **Step 5: Commit Task 2**

---

### Task 3: ReceiptParser Interface, EsewaParser & Pipeline

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/ReceiptParser.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/EsewaParser.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/parser/ReceiptProcessingPipeline.kt`
- Create: `app/src/main/java/com/sijan/kharcha/di/ParserModule.kt`
- Test: `app/src/test/java/com/sijan/kharcha/domain/parser/EsewaParserTest.kt`

**Interfaces:**
- Produces: `ReceiptParser` interface (`canParse`, `parse`).
- Produces: `EsewaParser` extracting amount, txn ID, date, note, and fee from eSewa OCR text.
- Produces: `ReceiptProcessingPipeline` orchestrating parser selection and fallback.

- [x] **Step 1: Write failing unit test for `EsewaParserTest` (RED)**
- [x] **Step 2: Implement `ReceiptParser`, `EsewaParser`, and `ReceiptProcessingPipeline`**
- [x] **Step 3: Register in `ParserModule`**
- [x] **Step 4: Run test to confirm green (GREEN)**
- [x] **Step 5: Commit Task 3**

---

### Task 4: Fixture Corpus & Accuracy Evaluation

**Files:**
- Create: `app/src/test/resources/fixtures/esewa_sample_1.txt`
- Create: `app/src/test/resources/fixtures/esewa_sample_1.json`
- Create: `app/src/test/java/com/sijan/kharcha/domain/parser/ReceiptParserFixtureTest.kt`

**Interfaces:**
- Produces: Automated fixture test suite verifying 99% accuracy on amount, date, txn ID.

- [x] **Step 1: Create redacted fixture sample matching eSewa receipt layout**
- [x] **Step 2: Implement `ReceiptParserFixtureTest` comparing extracted vs expected JSON**
- [x] **Step 3: Verify zero silent error behavior on malformed input**
- [x] **Step 4: Run full verification gate (`scripts/check.sh`)**
- [x] **Step 5: Commit Task 4 & Push to `origin main`**
