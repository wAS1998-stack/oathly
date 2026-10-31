package com.oathly.app.data

/** Which citizenship test the user is studying for. */
enum class Country(val code: String, val title: String) {
    US("US", "U.S. citizenship"),
    AU("AU", "Australian citizenship");

    companion object {
        fun from(code: String?): Country? = entries.firstOrNull { it.code == code }
    }
}

/** One USCIS civics question. Answers for [dynamicKey] questions come from [Officials]. */
data class UsQuestion(
    val id: String,
    val number: Int,
    val category: String,
    val categoryTitle: String,
    val subcategory: String,
    val text: String,
    val answers: List<String>,
    val need: Int,
    val senior: Boolean,
    val dynamicKey: String? = null,
    val dynamicNote: String? = null,
    val explanation: String? = null,
)

data class UsRules(
    val asked: Int,
    val passAt: Int,
    val failAt: Int,
    val seniorAsked: Int,
    val seniorPassAt: Int,
) {
    val seniorFailAt: Int get() = seniorAsked - seniorPassAt + 1
}

data class UsBank(
    val version: String,
    val sourceUrl: String,
    val updatesUrl: String,
    val rules: UsRules,
    val questions: List<UsQuestion>,
) {
    val categories: List<Pair<String, String>>
        get() = questions.map { it.category to it.categoryTitle }.distinct()
}

/** One Australian practice question (multiple choice, three options). */
data class AuQuestion(
    val id: String,
    val part: Int,
    val partTitle: String,
    val text: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val values: Boolean,
)

data class AuBank(
    val sourceTitle: String,
    val sourceUrl: String,
    val licence: String,
    val attribution: String,
    val questionsPerTest: Int,
    val valuesPerTest: Int,
    val passPercent: Int,
    val valuesRule: String,
    val timeLimitMinutes: Int?,
    val questions: List<AuQuestion>,
) {
    val parts: List<Pair<Int, String>>
        get() = questions.map { it.part to it.partTitle }.distinct().sortedBy { it.first }
}

data class Person(
    val name: String,
    val short: String,
    val party: String? = null,
    val district: Int? = null,
    val title: String? = null,
)

data class Place(
    val code: String,
    val name: String,
    val type: String,
    val capital: String?,
    val capitalAlt: List<String>,
    val governor: Person?,
    val senators: List<Person>,
    val representatives: List<Person>,
    val delegate: Person?,
) {
    val isState: Boolean get() = type == "state"
    val isDistrict: Boolean get() = type == "district"
}

data class Officials(
    val generatedAt: String,
    val president: Person?,
    val vicePresident: Person?,
    val speaker: Person?,
    val chiefJustice: Person?,
    val justices: Int,
    val places: List<Place>,
) {
    fun place(code: String?): Place? = places.firstOrNull { it.code == code }
}
