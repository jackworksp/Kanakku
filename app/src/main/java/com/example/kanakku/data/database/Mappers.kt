package com.example.kanakku.data.database

import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.Budget
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.RecurringTransaction

/**
 * Extension functions for mapping between database entities and domain models.
 *
 * These mappers provide bidirectional conversion between:
 * - TransactionEntity (database layer) <-> ParsedTransaction (domain layer)
 * - BudgetEntity (database layer) <-> Budget (domain layer)
 *
 * The mapping is 1:1 with no data loss or transformation.
 */

/**
 * Converts a ParsedTransaction domain model to a TransactionEntity for database storage.
 *
 * @return TransactionEntity with all fields mapped from the domain model
 */
fun ParsedTransaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        smsId = this.smsId,
        amount = this.amount,
        type = this.type,
        merchant = this.merchant,
        accountNumber = this.accountNumber,
        referenceNumber = this.referenceNumber,
        date = this.date,
        rawSms = this.rawSms,
        senderAddress = this.senderAddress,
        balanceAfter = this.balanceAfter,
        location = this.location,
        upiId = this.upiId,
        paymentMethod = this.paymentMethod
    )
}

/**
 * Converts a TransactionEntity from the database to a ParsedTransaction domain model.
 *
 * @return ParsedTransaction with all fields mapped from the database entity
 */
fun TransactionEntity.toDomain(): ParsedTransaction {
    return ParsedTransaction(
        smsId = this.smsId,
        amount = this.amount,
        type = this.type,
        merchant = this.merchant,
        accountNumber = this.accountNumber,
        referenceNumber = this.referenceNumber,
        date = this.date,
        rawSms = this.rawSms,
        senderAddress = this.senderAddress,
        balanceAfter = this.balanceAfter,
        location = this.location,
        upiId = this.upiId,
        paymentMethod = this.paymentMethod
    )
}

/**
 * Converts a Budget domain model to a BudgetEntity for database storage.
 *
 * @return BudgetEntity with all fields mapped from the domain model
 */
fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = this.id,
        categoryId = this.categoryId,
        amount = this.amount,
        month = this.month,
        year = this.year,
        createdAt = this.createdAt
    )
}

/**
 * Converts a BudgetEntity from the database to a Budget domain model.
 *
 * @return Budget with all fields mapped from the database entity
 */
fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = this.id,
        categoryId = this.categoryId,
        amount = this.amount,
        month = this.month,
        year = this.year,
        createdAt = this.createdAt
    )
}

/**
 * Converts a RecurringTransaction domain model to a RecurringTransactionEntity for database storage.
 *
 * @return RecurringTransactionEntity with all fields mapped from the domain model
 */
fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = this.id,
        merchantPattern = this.merchantPattern,
        amount = this.amount,
        frequency = this.frequency,
        averageInterval = this.averageInterval,
        lastOccurrence = this.lastOccurrence,
        nextExpected = this.nextExpected,
        transactionIds = this.transactionIds.joinToString(","),
        isUserConfirmed = this.isUserConfirmed,
        type = this.type
    )
}

/**
 * Converts a RecurringTransactionEntity from the database to a RecurringTransaction domain model.
 *
 * @return RecurringTransaction with all fields mapped from the database entity
 */
fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    return RecurringTransaction(
        id = this.id,
        merchantPattern = this.merchantPattern,
        amount = this.amount,
        frequency = this.frequency,
        averageInterval = this.averageInterval,
        lastOccurrence = this.lastOccurrence,
        nextExpected = this.nextExpected,
        transactionIds = if (this.transactionIds.isBlank()) {
            emptyList()
        } else {
            this.transactionIds.split(",").mapNotNull { it.toLongOrNull() }
        },
        isUserConfirmed = this.isUserConfirmed,
        type = this.type
    )
}
