package com.example.kanakku.data.category

import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.ParsedTransaction

class CategoryManager {

    private val manualOverrides = mutableMapOf<Long, Category>()

    fun categorizeTransaction(transaction: ParsedTransaction): Category {
        manualOverrides[transaction.smsId]?.let { return it }

        val searchText = buildString {
            append(transaction.merchant?.lowercase() ?: "")
            append(" ")
            append(transaction.rawSms.lowercase())
        }

        for (category in DefaultCategories.ALL) {
            if (category.keywords.any { keyword -> searchText.contains(keyword.lowercase()) }) {
                return category
            }
        }

        return DefaultCategories.OTHER
    }

    fun setManualOverride(smsId: Long, category: Category) {
        manualOverrides[smsId] = category
    }

    fun removeManualOverride(smsId: Long) {
        manualOverrides.remove(smsId)
    }

    fun getManualOverride(smsId: Long): Category? = manualOverrides[smsId]

    fun hasManualOverride(smsId: Long): Boolean = manualOverrides.containsKey(smsId)

    fun categorizeAll(transactions: List<ParsedTransaction>): Map<Long, Category> {
        return transactions.associate { it.smsId to categorizeTransaction(it) }
    }
}
