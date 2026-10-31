package com.oathly.app.domain

import com.oathly.app.data.Officials
import com.oathly.app.data.Person

/**
 * Answers that depend on elections, appointments or where the applicant lives.
 * [display] is what the app shows, [accepted] is what the answer checker allows.
 * When the data needed is missing, [accepted] is empty and the app falls back to
 * self-checking with a link to the official test updates page.
 */
data class DynamicAnswer(
    val display: List<String>,
    val accepted: List<String>,
    val note: String? = null,
    val needsSetup: Boolean = false,
)

object DynamicAnswers {

    private fun names(p: Person?): List<String> =
        if (p == null) emptyList() else listOf(p.name, p.short).distinct()

    fun resolve(key: String, officials: Officials?, stateCode: String?, repName: String?): DynamicAnswer {
        if (officials == null) return unknown("Answer data is still loading.")
        return when (key) {
            "president" -> person(officials.president, "President")
            "vice_president" -> person(officials.vicePresident, "Vice President")
            "speaker" -> person(officials.speaker, "Speaker of the House")
            "chief_justice" -> person(officials.chiefJustice, "Chief Justice")
            "president_party" -> officials.president?.party?.let { party ->
                val label = if (party.endsWith("Party")) party else "$party (Party)"
                DynamicAnswer(listOf(label), listOf(label))
            } ?: unknown("Party of the President is not available right now.")
            "justices_count" -> {
                val label = "${numberWord(officials.justices)} (${officials.justices})"
                DynamicAnswer(listOf(label), listOf(label))
            }
            "senators" -> senators(officials, stateCode)
            "representative" -> representative(officials, stateCode, repName)
            "governor" -> governor(officials, stateCode)
            "capital" -> capital(officials, stateCode)
            else -> unknown(null)
        }
    }

    private fun unknown(note: String?) = DynamicAnswer(
        display = listOf("Check uscis.gov/citizenship/testupdates"),
        accepted = emptyList(),
        note = note,
    )

    private fun needsState() = DynamicAnswer(
        display = listOf("Choose your state in Settings to see this answer."),
        accepted = emptyList(),
        needsSetup = true,
    )

    private fun person(p: Person?, role: String): DynamicAnswer =
        if (p == null) unknown("The current $role could not be loaded.")
        else DynamicAnswer(listOf(p.name), names(p))

    private fun senators(o: Officials, code: String?): DynamicAnswer {
        val place = o.place(code) ?: return needsState()
        if (!place.isState) {
            val who = if (place.isDistrict) "D.C." else place.name
            return DynamicAnswer(
                display = listOf("$who has no U.S. senators."),
                accepted = listOf("$who has no (U.S.) senators", "no (U.S.) senators", "none"),
            )
        }
        if (place.senators.isEmpty()) return unknown("Senators for ${place.name} could not be loaded.")
        return DynamicAnswer(
            display = place.senators.map { it.name },
            accepted = place.senators.flatMap { names(it) },
            note = "Either senator is a correct answer.",
        )
    }

    private fun representative(o: Officials, code: String?, repName: String?): DynamicAnswer {
        val place = o.place(code) ?: return needsState()
        if (!place.isState) {
            val d = place.delegate
            val who = if (place.isDistrict) "D.C." else place.name
            return DynamicAnswer(
                display = listOfNotNull(d?.let { "${it.name} (${it.title ?: "Delegate"})" },
                    "Also correct: $who has no (voting) representatives in Congress."),
                accepted = names(d) + listOf("no (voting) representatives (in Congress)", "no voting representative"),
            )
        }
        val mine = place.representatives.firstOrNull { it.name == repName }
        if (mine != null) {
            val district = if (mine.district == 0) "at-large" else "District ${mine.district}"
            return DynamicAnswer(listOf("${mine.name} ($district)"), names(mine))
        }
        if (place.representatives.size == 1) {
            val only = place.representatives.first()
            return DynamicAnswer(listOf("${only.name} (at-large)"), names(only))
        }
        return DynamicAnswer(
            display = listOf("Choose your representative in Settings. ${place.name} has ${place.representatives.size} districts."),
            accepted = place.representatives.flatMap { names(it) },
            needsSetup = true,
        )
    }

    private fun governor(o: Officials, code: String?): DynamicAnswer {
        val place = o.place(code) ?: return needsState()
        if (place.isDistrict) {
            return DynamicAnswer(
                display = listOf("D.C. does not have a governor."),
                accepted = listOf("D.C. does not have a governor", "does not have a governor", "no governor"),
            )
        }
        val g = place.governor ?: return unknown("The governor of ${place.name} is being updated. Check your state's official website.")
        return DynamicAnswer(listOf(g.name), names(g))
    }

    private fun capital(o: Officials, code: String?): DynamicAnswer {
        val place = o.place(code) ?: return needsState()
        if (place.isDistrict || place.capital == null) {
            return DynamicAnswer(
                display = listOf("D.C. is not a state and does not have a capital."),
                accepted = listOf("not a state", "does not have a capital", "no capital"),
            )
        }
        return DynamicAnswer(listOf(place.capital), listOf(place.capital) + place.capitalAlt)
    }

    private fun numberWord(n: Int): String = when (n) {
        7 -> "Seven"; 8 -> "Eight"; 9 -> "Nine"; 10 -> "Ten"; 11 -> "Eleven"; else -> n.toString()
    }
}
