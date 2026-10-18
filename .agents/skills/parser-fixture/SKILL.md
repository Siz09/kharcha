---
name: parser-fixture
description: Use this skill when adding or updating receipt test fixtures, evaluating OCR parser accuracy, or testing eSewa receipt parsing rules.
---

# Receipt Parser Fixture Workflow

Follow this procedure whenever real receipt samples are provided or parser accuracy needs evaluation.

## Fixture Structure
Fixtures live in `app/src/test/resources/fixtures/`:
- Raw text file: `<fixture_name>.txt` (contains raw OCR output with line breaks and coordinates if present)
- Expected JSON: `<fixture_name>.json` (contains ground truth values):
```json
{
  "source": "esewa",
  "expectedAmountPaisa": 150000,
  "expectedDateEpochMillis": 1718000000000,
  "expectedTransactionId": "1234567890",
  "expectedMerchant": "Sample Store",
  "expectedFeePaisa": 0
}
```

## Step-by-Step Fixture Process

1. **Add Fixture File**:
   - Sanitize all PII (names, phone numbers, account balances) in the sample text.
   - Place raw OCR output into `app/src/test/resources/fixtures/<name>.txt`.
   - Place ground truth fields into `app/src/test/resources/fixtures/<name>.json`.

2. **Run Parser Test Suite**:
   - Execute the test runner:
     `./gradlew testDebugUnitTest --tests "com.sijan.kharcha.domain.parser.*"`

3. **Calculate and Report Accuracy**:
   - Compare extracted fields against expected fields:
     - Amount accuracy (paisa match): target 99% for screenshots.
     - Date accuracy: target 99%.
     - Transaction ID accuracy: target 99%.
     - Merchant/Note accuracy: target 95%.
   - If any field fails, adjust normalizer/parser rules without regressing existing fixtures.

4. **Verify Zero Silent Errors**:
   - Verify that an unrecognized field triggers validation error flags rather than silently recording 0 or an arbitrary amount.
