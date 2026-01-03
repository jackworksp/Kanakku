package com.example.kanakku.data.database

import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.ParsedTransaction

/**
 * Extension functions for mapping between database entities and domain models.
 *
 * These mappers provide bidirectional conversion between:
 * - TransactionEntity (database layer)
 * - ParsedTransaction (domain layer)
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
