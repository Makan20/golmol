package com.example.nargesapp.data.model

data class Bank(
    val key: String,
    val name: String,
    val assetPath: String,
    val brandColor: Long,
    val bins: List<String> = emptyList()
)

object BankCatalog {

    val banks = listOf(
        Bank("melli",      "بانک ملی",      "banks/bank-melli.svg",      0xFF0E7C61, listOf("603799")),
        Bank("sepah",      "بانک سپه",      "banks/bank-sepah.svg",      0xFF7A2A82, listOf("589210")),
        Bank("tejarat",    "بانک تجارت",    "banks/bank-tejarat.svg",    0xFF7B5EA7, listOf("627353")),
        Bank("pasargad",   "بانک پاسارگاد", "banks/bank-pasargad.svg",   0xFFF7941E, listOf("502229")),
        Bank("parsian",    "بانک پارسیان",  "banks/bank-parsian.svg",    0xFFF58220, listOf("622106", "627884")),
        Bank("saman",      "بانک سامان",    "banks/bank-saman.svg",      0xFF00A19A, listOf("621986")),
        Bank("refah",      "بانک رفاه",     "banks/bank-refah.svg",      0xFFC8102E, listOf("589463")),
        Bank("saderat",    "بانک صادرات",   "banks/bank-saderat.svg",    0xFF1B5EAB, listOf("603769")),
        Bank("keshavarzi", "بانک کشاورزی",  "banks/bank-keshavarzi.svg", 0xFF2E7D32, listOf("603770", "639217")),
        Bank("dey",        "بانک دی",       "banks/bank-dey.svg",        0xFF7A4A9E, listOf("502938")),
        Bank("blubank",    "بلو بانک",      "banks/bank-blubank.svg",    0xFFD9A404, listOf("636949")),
        Bank("maskan",     "بانک مسکن",     "banks/bank-maskan.svg",     0xFFE87722, listOf("628023")),
        Bank("mellat",     "بانک ملت",      "banks/bank-mellat.svg",     0xFFCC2233, listOf("610433", "991975")),
        Bank("shahr",      "بانک شهر",      "banks/bank-shahr.svg",      0xFF9E1B32, listOf("502806", "504706")),
        Bank("sina",       "بانک سینا",     "banks/bank-sina.svg",       0xFF0066A4, listOf("639346"))
    )

    fun byKey(key: String?): Bank? = banks.firstOrNull { it.key == key }

    fun fromCardNumber(cardNumber: String): Bank? {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length < 6) return null
        return banks.firstOrNull { b -> b.bins.any { digits.startsWith(it) } }
    }
}
