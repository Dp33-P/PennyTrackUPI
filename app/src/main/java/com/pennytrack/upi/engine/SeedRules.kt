package com.pennytrack.upi.engine

import com.pennytrack.upi.data.model.Categories
import com.pennytrack.upi.data.model.TransactionKind

data class CategoryRule(
    val category: String,
    val kind: TransactionKind,
    val keywords: List<String>
)

object SeedRules {
    val categoryRules = listOf(
        CategoryRule(
            Categories.FOOD,
            TransactionKind.DAILY_SPEND,
            listOf(
                "swiggy", "zomato", "eatsure", "dominos", "pizza hut", "kfc",
                "mcdonald", "burger king", "subway", "starbucks", "chai", "cafe",
                "restaurant", "bakery", "food", "eatclub", "faasos", "behrouz"
            )
        ),
        CategoryRule(
            Categories.GROCERY,
            TransactionKind.DAILY_SPEND,
            listOf(
                "blinkit", "zepto", "bigbasket", "dmart", "jiomart", "reliance fresh",
                "more supermarket", "spencers", "grofers", "kirana", "grocery",
                "supermarket", "ration", "milkbasket", "fresh"
            )
        ),
        CategoryRule(
            Categories.MEDICINE,
            TransactionKind.DAILY_SPEND,
            listOf(
                "apollo", "medplus", "pharmeasy", "tata 1mg", "netmeds", "practo",
                "pharmacy", "medical", "clinic", "hospital", "diagnostic", "pathlab",
                "health", "chemist"
            )
        ),
        CategoryRule(
            Categories.CLOTHES,
            TransactionKind.DAILY_SPEND,
            listOf(
                "myntra", "ajio", "zudio", "pantaloons", "max fashion", "lifestyle",
                "westside", "uniqlo", "h&m", "clothing", "fashion", "apparel"
            )
        ),
        CategoryRule(
            Categories.SHOPPING,
            TransactionKind.DAILY_SPEND,
            listOf(
                "amazon", "flipkart", "meesho", "snapdeal", "nykaa", "firstcry",
                "croma", "vijay sales", "reliance digital", "shopping", "store"
            )
        ),
        CategoryRule(
            Categories.TRAVEL,
            TransactionKind.DAILY_SPEND,
            listOf(
                "uber", "ola", "rapido", "nammayatri", "namma yatri", "irctc",
                "makemytrip", "redbus", "metro", "bus", "railway", "cab", "taxi"
            )
        ),
        CategoryRule(
            Categories.FUEL,
            TransactionKind.DAILY_SPEND,
            listOf(
                "indian oil", "iocl", "hp petrol", "hindustan petroleum",
                "bharat petroleum", "bpcl", "petrol", "diesel", "shell", "fuel"
            )
        ),
        CategoryRule(
            Categories.MOBILE_RECHARGE,
            TransactionKind.FIXED_BILL,
            listOf("airtel", "jio", "vodafone", "idea", "vi ", "bsnl", "prepaid", "recharge")
        ),
        CategoryRule(
            Categories.DTH,
            TransactionKind.FIXED_BILL,
            listOf("tata play", "tatasky", "dish tv", "d2h", "sun direct", "dth")
        ),
        CategoryRule(
            Categories.BROADBAND,
            TransactionKind.FIXED_BILL,
            listOf("broadband", "fiber", "fibre", "act fibernet", "jiofiber", "airtel xstream", "internet bill")
        ),
        CategoryRule(
            Categories.ELECTRICITY,
            TransactionKind.FIXED_BILL,
            listOf(
                "electricity", "power bill", "discom", "bescom", "mahavitaran",
                "tneb", "adani electricity", "tata power", "mseb", "uppcl", "dhbvn"
            )
        ),
        CategoryRule(
            Categories.GAS_BOOKING,
            TransactionKind.FIXED_BILL,
            listOf("indane", "hp gas", "bharat gas", "lpg", "gas booking", "cylinder")
        ),
        CategoryRule(
            Categories.WATER_BILL,
            TransactionKind.FIXED_BILL,
            listOf("water bill", "bwssb", "jal board", "municipal water")
        ),
        CategoryRule(
            Categories.CREDIT_CARD_BILL,
            TransactionKind.FIXED_BILL,
            listOf("credit card", "card bill", "cc payment", "cred", "onecard", "sbi card")
        ),
        CategoryRule(
            Categories.EMI,
            TransactionKind.EMI_LOAN,
            listOf(
                "emi", "loan", "bajaj finance", "hdb finance", "hdfc loan",
                "tata capital", "idfc first", "home credit", "tvs credit",
                "poonawalla", "finance limited", "nbfc"
            )
        ),
        CategoryRule(
            Categories.RENT,
            TransactionKind.FIXED_BILL,
            listOf("rent", "landlord", "house rent", "room rent", "flat rent", "pg rent")
        ),
        CategoryRule(
            Categories.INSURANCE,
            TransactionKind.FIXED_BILL,
            listOf("lic", "insurance", "policybazaar", "hdfc ergo", "icici lombard", "star health", "policy")
        ),
        CategoryRule(
            Categories.SUBSCRIPTION,
            TransactionKind.FIXED_BILL,
            listOf(
                "netflix", "prime video", "amazon prime", "hotstar", "disney",
                "spotify", "youtube premium", "zee5", "sony liv", "jio cinema",
                "google play", "app store", "subscription"
            )
        ),
        CategoryRule(
            Categories.EDUCATION,
            TransactionKind.DAILY_SPEND,
            listOf("school", "college", "tuition", "course", "byju", "unacademy", "udemy", "coursera", "exam fee")
        )
    )

    val knownMerchants = categoryRules.flatMap { rule ->
        rule.keywords.map { keyword ->
            KnownMerchant(
                key = TextNormalizer.key(keyword) ?: keyword,
                displayName = TextNormalizer.displayName(keyword),
                category = rule.category,
                kind = rule.kind,
                alias = keyword
            )
        }
    }.distinctBy { it.key }
}

data class KnownMerchant(
    val key: String,
    val displayName: String,
    val category: String,
    val kind: TransactionKind,
    val alias: String
)
