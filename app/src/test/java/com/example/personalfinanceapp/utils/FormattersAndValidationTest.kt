package com.example.personalfinanceapp.utils

import com.example.personalfinanceapp.data.Frequency
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Formatters.kt — pure functions, no Android context needed.
 */
class FormattersTest {

    // ─── extractAmount ────────────────────────────────────────────────────────

    @Test
    fun `extractAmount returns number from string with text`() {
        assertEquals(3000.0, extractAmount("Pizza 3000"), 0.0)
    }

    @Test
    fun `extractAmount returns first number when multiple numbers present`() {
        assertEquals(12.0, extractAmount("12 tojás 500 Ft"), 0.0)
    }

    @Test
    fun `extractAmount returns 0 when no number present`() {
        assertEquals(0.0, extractAmount("semmi szám itt"), 0.0)
    }

    @Test
    fun `extractAmount returns 0 for empty string`() {
        assertEquals(0.0, extractAmount(""), 0.0)
    }

    @Test
    fun `extractAmount handles string that is only a number`() {
        assertEquals(9999.0, extractAmount("9999"), 0.0)
    }

    // ─── formatAmount ─────────────────────────────────────────────────────────

    @Test
    fun `formatAmount formats large number with Hungarian grouping`() {
        val result = formatAmount(12345.0)
        // Hungarian locale uses space as thousands separator → "12 345"
        assertTrue(
            "Expected grouped number, got: '$result'",
            result.contains("12") && result.contains("345")
        )
    }

    @Test
    fun `formatAmount formats zero`() {
        val result = formatAmount(0.0)
        assertEquals("0", result)
    }

    @Test
    fun `formatAmount shows no decimal places`() {
        val result = formatAmount(1500.99)
        // Should round and have no fraction
        assertFalse("Should not contain decimal point: '$result'", result.contains("."))
        assertFalse("Should not contain comma fraction: '$result'", result.contains(",99"))
    }

    @Test
    fun `formatAmount handles small number`() {
        val result = formatAmount(500.0)
        assertEquals("500", result)
    }

    // ─── mapFrequency ─────────────────────────────────────────────────────────

    @Test
    fun `mapFrequency returns Hungarian label for MONTHLY`() {
        assertEquals("Havonta", mapFrequency(Frequency.MONTHLY))
    }

    @Test
    fun `mapFrequency returns Hungarian label for QUARTERLY`() {
        assertEquals("Negyedévente", mapFrequency(Frequency.QUARTERLY))
    }

    @Test
    fun `mapFrequency returns Hungarian label for YEARLY`() {
        assertEquals("Évente", mapFrequency(Frequency.YEARLY))
    }

    @Test
    fun `mapFrequency covers all Frequency enum values`() {
        // If a new Frequency is added, this test will catch the missing mapping
        for (freq in Frequency.values()) {
            val result = mapFrequency(freq)
            assertTrue(
                "mapFrequency returned blank for $freq",
                result.isNotBlank()
            )
        }
    }

    // ─── formatDate ───────────────────────────────────────────────────────────

    @Test
    fun `formatDate returns Ma for today`() {
        val now = System.currentTimeMillis()
        assertEquals("Ma", formatDate(now))
    }

    @Test
    fun `formatDate returns Tegnap for yesterday`() {
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        assertEquals("Tegnap", formatDate(yesterday))
    }

    @Test
    fun `formatDate returns days ago string for 3 days ago`() {
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)
        assertEquals("3 napja", formatDate(threeDaysAgo))
    }

    @Test
    fun `formatDate returns month-day format for old dates`() {
        val oldDate = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val result = formatDate(oldDate)
        // Should not be "Ma", "Tegnap", or "X napja" — should be a date string
        assertFalse(result == "Ma")
        assertFalse(result == "Tegnap")
        assertFalse(result.endsWith("napja"))
        assertTrue("Expected a non-empty date string, got: '$result'", result.isNotBlank())
    }
}

/**
 * Unit tests for Validation.kt — pure functions, no Android context needed.
 */
class ValidationTest {

    // ─── validateTitle ────────────────────────────────────────────────────────

    @Test
    fun `validateTitle succeeds for normal title`() {
        val result = Validation.validateTitle("Netflix előfizetés")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateTitle fails for blank title`() {
        val result = Validation.validateTitle("   ")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateTitle fails for empty string`() {
        val result = Validation.validateTitle("")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateTitle fails for single character`() {
        val result = Validation.validateTitle("A")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateTitle succeeds for exactly 2 characters`() {
        val result = Validation.validateTitle("AB")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateTitle fails for title longer than 100 characters`() {
        val longTitle = "A".repeat(101)
        val result = Validation.validateTitle(longTitle)
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateTitle succeeds for exactly 100 characters`() {
        val maxTitle = "A".repeat(100)
        val result = Validation.validateTitle(maxTitle)
        assertTrue(result is ValidationResult.Success)
    }

    // ─── validateAmount ───────────────────────────────────────────────────────

    @Test
    fun `validateAmount succeeds for valid positive amount`() {
        val result = Validation.validateAmount("1500")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateAmount succeeds for decimal amount`() {
        val result = Validation.validateAmount("999.99")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateAmount fails for blank input`() {
        val result = Validation.validateAmount("  ")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateAmount fails for zero`() {
        val result = Validation.validateAmount("0")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateAmount fails for negative number`() {
        val result = Validation.validateAmount("-100")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateAmount fails for non-numeric text`() {
        val result = Validation.validateAmount("ezer forint")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateAmount fails for amount exceeding maximum`() {
        val result = Validation.validateAmount("1000000000")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateAmount succeeds for maximum allowed amount`() {
        val result = Validation.validateAmount("999999999")
        assertTrue(result is ValidationResult.Success)
    }

    // ─── validateDay ──────────────────────────────────────────────────────────

    @Test
    fun `validateDay succeeds for valid day`() {
        val result = Validation.validateDay("15")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateDay succeeds for boundary day 1`() {
        val result = Validation.validateDay("1")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateDay succeeds for boundary day 31`() {
        val result = Validation.validateDay("31")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateDay fails for day 0`() {
        val result = Validation.validateDay("0")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateDay fails for day 32`() {
        val result = Validation.validateDay("32")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateDay fails for blank input`() {
        val result = Validation.validateDay("")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateDay fails for non-numeric input`() {
        val result = Validation.validateDay("tíz")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateDay fails for negative number`() {
        val result = Validation.validateDay("-1")
        assertTrue(result is ValidationResult.Error)
    }
}