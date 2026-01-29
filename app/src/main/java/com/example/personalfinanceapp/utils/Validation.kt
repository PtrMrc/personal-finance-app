package com.example.personalfinanceapp.utils

object Validation {

    fun validateTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Error("A megnevezés nem lehet üres")
            title.length < 2 -> ValidationResult.Error("A megnevezés túl rövid")
            title.length > 100 -> ValidationResult.Error("A megnevezés túl hosszú (max 100 karakter)")
            else -> ValidationResult.Success
        }
    }

    fun validateAmount(amountText: String): ValidationResult {
        return when {
            amountText.isBlank() -> ValidationResult.Error("Az összeg nem lehet üres")
            else -> {
                val amount = amountText.toDoubleOrNull()
                when {
                    amount == null -> ValidationResult.Error("Érvénytelen összeg")
                    amount <= 0 -> ValidationResult.Error("Az összeg pozitív szám kell legyen")
                    amount > 999999999 -> ValidationResult.Error("Az összeg túl nagy")
                    else -> ValidationResult.Success
                }
            }
        }
    }

    fun validateDay(dayText: String): ValidationResult {
        return when {
            dayText.isBlank() -> ValidationResult.Error("A nap nem lehet üres")
            else -> {
                val day = dayText.toIntOrNull()
                when {
                    day == null -> ValidationResult.Error("Érvénytelen nap")
                    day !in 1..31 -> ValidationResult.Error("A nap 1 és 31 között kell legyen")
                    else -> ValidationResult.Success
                }
            }
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}