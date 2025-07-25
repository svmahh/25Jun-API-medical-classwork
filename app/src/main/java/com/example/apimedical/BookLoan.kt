package com.example.apimedical

data class Loan(
    val LoanID:Int,
    val amount: String,
    val memberID: String,
    val message: String
)

data class LoanPost(
    val amount: String,
    val memberID: String,
    val message: String
)