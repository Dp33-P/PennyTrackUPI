package com.pennytrack.upi.data.model

enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class TransactionKind {
    DAILY_SPEND,
    FIXED_BILL,
    EMI_LOAN,
    CASH_SPEND,
    TRANSFER,
    REFUND,
    INCOME
}

enum class TransactionSource {
    SMS,
    NOTIFICATION,
    CASH,
    IMPORT
}

object Categories {
    const val FOOD = "Food"
    const val GROCERY = "Grocery"
    const val MEDICINE = "Medicine"
    const val RENT = "Rent"
    const val CLOTHES = "Clothes"
    const val TRAVEL = "Travel"
    const val SHOPPING = "Shopping"
    const val MOBILE_RECHARGE = "Mobile Recharge"
    const val DTH = "DTH"
    const val BROADBAND = "Broadband"
    const val ELECTRICITY = "Electricity"
    const val GAS_BOOKING = "Gas Booking"
    const val WATER_BILL = "Water Bill"
    const val CREDIT_CARD_BILL = "Credit Card Bill"
    const val EMI = "EMI"
    const val INSURANCE = "Insurance"
    const val SUBSCRIPTION = "Subscription"
    const val EDUCATION = "Education"
    const val FUEL = "Fuel"
    const val CASH = "Cash"
    const val TRANSFER = "Transfer"
    const val REFUND = "Refund"
    const val INCOME = "Income"
    const val OTHER = "Other"

    val all = listOf(
        FOOD,
        GROCERY,
        MEDICINE,
        RENT,
        CLOTHES,
        TRAVEL,
        SHOPPING,
        MOBILE_RECHARGE,
        DTH,
        BROADBAND,
        ELECTRICITY,
        GAS_BOOKING,
        WATER_BILL,
        CREDIT_CARD_BILL,
        EMI,
        INSURANCE,
        SUBSCRIPTION,
        EDUCATION,
        FUEL,
        CASH,
        TRANSFER,
        REFUND,
        INCOME,
        OTHER
    )
}
