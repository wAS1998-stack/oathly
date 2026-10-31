package com.oathly.app.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oathly.app.OathlyApp
import com.oathly.app.data.AppSettings
import com.oathly.app.data.AuBank
import com.oathly.app.data.AuQuestion
import com.oathly.app.data.Country
import com.oathly.app.data.TestRecord
import com.oathly.app.data.ThemeMode
import com.oathly.app.data.UsBank
import com.oathly.app.data.UsQuestion
import com.oathly.app.domain.AnswerMatcher
import com.oathly.app.domain.AuTest
import com.oathly.app.domain.DynamicAnswer
import com.oathly.app.domain.DynamicAnswers
import com.oathly.app.domain.Interview
import com.oathly.app.domain.QuestionStat
import com.oathly.app.domain.Readiness
import com.oathly.app.domain.Srs
import com.oathly.app.platform.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object StudyMenu : Screen
    data object Study : Screen
    data object Interview : Screen
    data object AuPractice : Screen
    data object Results : Screen
    data object Officials : Screen
    data object Progress : Screen
    data object Settings : Screen
    data object PickState : Screen
    data object PickRep : Screen
    data object About : Screen
    data object Pro : Screen
}

/** A set of flashcards being studied. */
data class Deck(
    val title: String,
    val ids: List<String>,
    val index: Int = 0,
    val revealed: Boolean = false,
    val known: Int = 0,
    val missed: Int = 0,
) {
    val done: Boolean get() = index >= ids.size
    val currentId: String? get() = ids.getOrNull(index)
}

/** What the app shows after an interview answer. */
data class Feedback(
    val questionId: String,
    val given: String?,
    /** null = the user still has to say whether they knew it (self-check). */
    val correct: Boolean?,
    val answers: List<String>,
    val note: String?,
    val canOverride: Boolean,
)

data class ResultItem(val question: String, val correct: Boolean, val given: String?, val answer: String, val explanation: String?)

data class ResultSummary(
    val title: String,
    val passed: Boolean,
    val right: Int,
    val total: Int,
    val detail: String,
    val items: List<ResultItem>,
    val retry: Screen,
)

/** Options for a study deck shown on the study menu. */
data class DeckOption(val title: String, val subtitle: String, val ids: List<String>)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val graph = (app as OathlyApp).graph
    val settings = graph.settings.state
    val officials = graph.officials.state
    val stats = graph.progress.stats
    val tests = graph.progress.tests
    val days = graph.progress.days
    val isPro = graph.billing.isPro
    val proPrice = graph.billing.price
    val purchaseStatus = graph.billing.status
    val bannersAllowed = graph.ads.bannersAllowed

    var usBank by mutableStateOf<UsBank?>(null)
        private set
    var auBank by mutableStateOf<AuBank?>(null)
        private set
    var readiness by mutableStateOf<Double?>(null)
        private set

    val backStack = mutableStateListOf<Screen>(if (settings.value.onboarded) Screen.Home else Screen.Onboarding)
    val current: Screen get() = backStack.last()

    var deck by mutableStateOf<Deck?>(null)
        private set
    var interview by mutableStateOf<Interview?>(null)
        private set
    var feedback by mutableStateOf<Feedback?>(null)
        private set
    var auTest by mutableStateOf<AuTest?>(null)
        private set
    var result by mutableStateOf<ResultSummary?>(null)
        private set

    private var statBeforeLastAnswer: Pair<String, QuestionStat?>? = null
    private val rnd = Random(System.nanoTime())

    init {
        viewModelScope.launch {
            loadBanks(settings.value)
            readiness = computeReadiness(settings.value)
        }
        viewModelScope.launch {
            combine(stats, settings) { _, s -> s }.collectLatest { s ->
                delay(250)
                readiness = withContext(Dispatchers.Default) { computeReadiness(s) }
            }
        }
    }

    // ---------- navigation ----------
    fun navigate(screen: Screen) { backStack.add(screen) }
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        if (current != Screen.Interview && current != Screen.AuPractice) graph.speaker.stop()
        return true
    }
    private fun replaceTop(screen: Screen) {
        backStack.removeAt(backStack.lastIndex)
        backStack.add(screen)
    }
    private fun resetTo(screen: Screen) { backStack.clear(); backStack.add(screen) }

    // ---------- content ----------
    private suspend fun loadBanks(s: AppSettings) {
        when (s.country) {
            Country.US -> usBank = withContext(Dispatchers.Default) { graph.content.usBank(s.usVersion) }
            Country.AU -> auBank = withContext(Dispatchers.Default) { graph.content.auBank() }
            null -> Unit
        }
    }

    fun usQuestion(id: String): UsQuestion? = usBank?.questions?.firstOrNull { it.id == id }
    fun auQuestion(id: String): AuQuestion? = auBank?.questions?.firstOrNull { it.id == id }

    fun dynamicAnswer(q: UsQuestion): DynamicAnswer? = q.dynamicKey?.let {
        val s = settings.value
        DynamicAnswers.resolve(it, officials.value, s.stateCode, s.repName)
    }

    fun answersToShow(q: UsQuestion): List<String> = dynamicAnswer(q)?.display ?: q.answers

    private fun acceptedAnswers(q: UsQuestion): List<String> = dynamicAnswer(q)?.accepted ?: q.answers

    /** The question pool the user is studying (the 20 starred ones in 65/20 mode). */
    fun usPool(): List<UsQuestion> {
        val bank = usBank ?: return emptyList()
        return if (settings.value.senior) bank.questions.filter { it.senior } else bank.questions
    }

    private fun computeReadiness(s: AppSettings): Double? {
        val st = stats.value
        val seed = graph.progress.today()
        return when (s.country) {
            Country.US -> {
                val bank = usBank ?: return null
                val pool = usPool()
                val r = bank.rules
                val chances = pool.map { Srs.knowChance(st[it.id], 0.2) }
                if (s.senior) Readiness.usPassChance(chances, r.seniorAsked, r.seniorPassAt, r.seniorFailAt, seed)
                else Readiness.usPassChance(chances, r.asked, r.passAt, r.failAt, seed)
            }
            Country.AU -> {
                val bank = auBank ?: return null
                val values = bank.questions.filter { it.values }.map { Srs.knowChance(st[it.id], 0.4) }
                val other = bank.questions.filter { !it.values }.map { Srs.knowChance(st[it.id], 0.4) }
                val passMark = Math.ceil(bank.questionsPerTest * bank.passPercent / 100.0).toInt()
                Readiness.auPassChance(values, other, bank.valuesPerTest, bank.questionsPerTest - bank.valuesPerTest, passMark, seed)
            }
            null -> null
        }
    }

    // ---------- speech ----------
    fun speak(text: String) {
        val s = settings.value
        val locale = if (s.country == Country.AU) Locale("en", "AU") else Locale.US
        graph.speaker.speak(text, locale, s.speechRate)
    }
    fun stopSpeaking() = graph.speaker.stop()

    // ---------- onboarding & settings ----------
    fun finishOnboarding(country: Country, usVersion: String, senior: Boolean, stateCode: String?, repName: String?, reminder: Pair<Int, Int>?) {
        graph.settings.update {
            it.copy(
                onboarded = true, country = country, usVersion = usVersion, senior = senior,
                stateCode = stateCode, repName = repName,
                reminderOn = reminder != null,
                reminderHour = reminder?.first ?: it.reminderHour,
                reminderMinute = reminder?.second ?: it.reminderMinute,
            )
        }
        if (reminder != null) ReminderWorker.schedule(getApplication(), reminder.first, reminder.second)
        viewModelScope.launch {
            loadBanks(settings.value)
            readiness = computeReadiness(settings.value)
        }
        resetTo(Screen.Home)
    }

    fun setCountry(country: Country) {
        graph.settings.update { it.copy(country = country) }
        viewModelScope.launch { loadBanks(settings.value); readiness = computeReadiness(settings.value) }
    }
    fun setUsVersion(version: String) {
        graph.settings.update { it.copy(usVersion = version) }
        viewModelScope.launch { loadBanks(settings.value); readiness = computeReadiness(settings.value) }
    }
    fun setSenior(on: Boolean) = graph.settings.update { it.copy(senior = on) }
    fun setState(code: String) = graph.settings.update { it.copy(stateCode = code, repName = null) }
    fun setRep(name: String?) = graph.settings.update { it.copy(repName = name) }
    fun setAutoRead(on: Boolean) = graph.settings.update { it.copy(autoRead = on) }
    fun setSpeechRate(rate: Float) = graph.settings.update { it.copy(speechRate = rate) }
    fun setTheme(mode: ThemeMode) = graph.settings.update { it.copy(theme = mode) }
    fun setReminder(on: Boolean, hour: Int, minute: Int) {
        graph.settings.update { it.copy(reminderOn = on, reminderHour = hour, reminderMinute = minute) }
        if (on) ReminderWorker.schedule(getApplication(), hour, minute) else ReminderWorker.cancel(getApplication())
    }
    fun resetProgress() {
        val s = settings.value
        val prefix = if (s.country == Country.AU) "au" else "us${s.usVersion}"
        graph.progress.reset(prefix)
    }

    // ---------- study cards ----------
    fun deckOptions(): List<DeckOption> {
        val st = stats.value
        val today = graph.progress.today()
        val out = mutableListOf<DeckOption>()
        when (settings.value.country) {
            Country.US -> {
                val pool = usPool()
                val due = pool.filter { Srs.isDue(st[it.id], today) }
                val missed = pool.filter { st[it.id]?.let { s -> s.seen && !s.lastCorrect } == true }
                val fresh = pool.filter { st[it.id]?.seen != true }
                if (due.isNotEmpty()) out += DeckOption("Due for review", "${due.size} questions", due.map { it.id })
                if (missed.isNotEmpty()) out += DeckOption("Missed last time", "${missed.size} questions", missed.map { it.id })
                if (fresh.isNotEmpty()) out += DeckOption("New questions", "${fresh.size} not studied yet", fresh.map { it.id })
                out += DeckOption("All questions", "${pool.size} questions in order", pool.map { it.id })
                if (!settings.value.senior) {
                    usBank?.categories?.forEach { (key, title) ->
                        val ids = pool.filter { it.category == key }.map { it.id }
                        out += DeckOption(title, "${ids.size} questions", ids)
                    }
                    val starred = pool.filter { it.senior }
                    out += DeckOption("65/20 questions", "The ${starred.size} starred questions", starred.map { it.id })
                }
            }
            Country.AU -> {
                val bank = auBank ?: return out
                val due = bank.questions.filter { Srs.isDue(st[it.id], today) }
                val missed = bank.questions.filter { st[it.id]?.let { s -> s.seen && !s.lastCorrect } == true }
                if (due.isNotEmpty()) out += DeckOption("Due for review", "${due.size} questions", due.map { it.id })
                if (missed.isNotEmpty()) out += DeckOption("Missed last time", "${missed.size} questions", missed.map { it.id })
                bank.parts.forEach { (part, title) ->
                    val ids = bank.questions.filter { it.part == part }.map { it.id }
                    val label = if (part == 4) "$title (must get all right)" else title
                    out += DeckOption("Part $part: $label", "${ids.size} questions", ids)
                }
                out += DeckOption("All questions", "${bank.questions.size} questions", bank.questions.map { it.id })
            }
            null -> Unit
        }
        return out
    }

    fun startDeck(option: DeckOption, shuffle: Boolean) {
        val ids = if (shuffle) option.ids.shuffled(rnd) else option.ids
        deck = Deck(option.title, ids)
        navigate(Screen.Study)
    }

    fun revealCard() { deck = deck?.copy(revealed = true) }

    fun gradeCard(knew: Boolean) {
        val d = deck ?: return
        val id = d.currentId ?: return
        graph.progress.record(id, knew)
        stopSpeaking()
        deck = d.copy(
            index = d.index + 1, revealed = false,
            known = d.known + if (knew) 1 else 0, missed = d.missed + if (knew) 0 else 1,
        )
    }

    // ---------- US interview ----------
    fun startInterview() {
        val bank = usBank ?: return
        val st = stats.value
        interview = Interview.start(bank, settings.value.senior, rnd) { q -> 1.6 - Srs.knowChance(st[q.id], 0.2) }
        feedback = null
        if (current == Screen.Results) replaceTop(Screen.Interview) else navigate(Screen.Interview)
    }

    /** [alternatives]: what the user typed, or every guess the speech recognizer heard. */
    fun submitAnswer(alternatives: List<String>) {
        val iv = interview ?: return
        val q = iv.current ?: return
        if (feedback != null) return
        val accepted = acceptedAnswers(q)
        val given = alternatives.firstOrNull()?.trim().orEmpty()
        if (accepted.isEmpty() || given.isEmpty()) { revealAnswer(); return }
        val check = AnswerMatcher.checkAny(alternatives, accepted, q.need)
        statBeforeLastAnswer = q.id to stats.value[q.id]
        interview = iv.answer(check.correct, given)
        graph.progress.record(q.id, check.correct)
        feedback = Feedback(
            q.id, given, check.correct, answersToShow(q),
            note = needNote(q, check.matched),
            canOverride = !check.correct,
        )
    }

    private fun needNote(q: UsQuestion, matched: Int): String? = when {
        q.need > 1 && matched in 1 until q.need -> "You gave $matched of the ${q.need} answers needed."
        else -> dynamicAnswer(q)?.note
    }

    fun revealAnswer() {
        val q = interview?.current ?: return
        if (feedback != null) return
        feedback = Feedback(q.id, null, null, answersToShow(q), dynamicAnswer(q)?.note, canOverride = false)
    }

    fun selfGrade(knew: Boolean) {
        val iv = interview ?: return
        val f = feedback ?: return
        if (f.correct != null) return
        val q = iv.current ?: return
        interview = iv.answer(knew, null)
        graph.progress.record(q.id, knew)
        feedback = f.copy(correct = knew)
    }

    /** The strict checker said no, but the user's answer was fine. */
    fun overrideCorrect() {
        val f = feedback ?: return
        if (f.correct != false || !f.canOverride) return
        interview = interview?.overrideLast()
        statBeforeLastAnswer?.let { (id, before) ->
            if (id == f.questionId) graph.progress.replace(id, Srs.record(before ?: QuestionStat(), true, graph.progress.today()))
        }
        feedback = f.copy(correct = true, canOverride = false)
    }

    fun nextInterviewQuestion() {
        stopSpeaking()
        feedback = null
        val iv = interview ?: return
        if (iv.finished) finishInterview(iv)
    }

    fun quitTest() {
        interview = null; auTest = null; feedback = null
        stopSpeaking()
        back()
    }

    private fun finishInterview(iv: Interview) {
        val s = settings.value
        val version = usBank?.version ?: s.usVersion
        graph.progress.addTest(TestRecord(System.currentTimeMillis(), "us$version", iv.right, iv.answers.size, iv.passed))
        result = ResultSummary(
            title = if (iv.passed) "You passed!" else "Not this time",
            passed = iv.passed,
            right = iv.right,
            total = iv.answers.size,
            detail = if (iv.passed) "You answered ${iv.passAt} questions correctly, the officer would stop here."
            else "You missed ${iv.failAt}. The officer would stop here. Review the questions below and try again.",
            items = iv.answers.map { a ->
                ResultItem(a.question.text, a.correct, a.given, answersToShow(a.question).joinToString(" • "), a.question.explanation)
            },
            retry = Screen.Interview,
        )
        interview = null
        replaceTop(Screen.Results)
    }

    // ---------- Australian practice test ----------
    fun startAuTest() {
        val bank = auBank ?: return
        auTest = AuTest.start(bank, rnd)
        if (current == Screen.Results) replaceTop(Screen.AuPractice) else navigate(Screen.AuPractice)
    }

    fun chooseAu(option: Int) {
        val t = auTest ?: return
        val q = t.current ?: return
        if (t.currentAnswered) return
        auTest = t.choose(option)
        graph.progress.record(q.id, option == q.answerIndex)
    }

    fun nextAu() {
        stopSpeaking()
        val t = auTest?.next() ?: return
        auTest = t
        if (t.finished) finishAuTest(t)
    }

    private fun finishAuTest(t: AuTest) {
        graph.progress.addTest(TestRecord(System.currentTimeMillis(), "au", t.score, t.questions.size, t.passed))
        val valuesOk = t.valuesRight == t.valuesTotal
        result = ResultSummary(
            title = if (t.passed) "You passed!" else "Not this time",
            passed = t.passed,
            right = t.score,
            total = t.questions.size,
            detail = buildString {
                append("You needed ${t.passMark} of ${t.questions.size}. ")
                append("Values questions: ${t.valuesRight} of ${t.valuesTotal} right")
                append(if (valuesOk) "." else " (all of them must be right to pass).")
            },
            items = t.questions.mapIndexed { i, q ->
                val chosen = t.chosen[i]
                ResultItem(q.text, chosen == q.answerIndex, chosen?.let { q.options[it] }, q.options[q.answerIndex], q.explanation)
            },
            retry = Screen.AuPractice,
        )
        auTest = null
        replaceTop(Screen.Results)
    }

    fun retryFromResults() {
        when (result?.retry) {
            Screen.Interview -> startInterview()
            Screen.AuPractice -> startAuTest()
            else -> Unit
        }
    }

    fun leaveResults(activity: Activity) {
        graph.ads.afterTest(activity) { resetTo(Screen.Home) }
    }

    // ---------- store ----------
    fun buyPro(activity: Activity): Boolean = graph.billing.buy(activity)
    fun restorePurchases() = graph.billing.restore()
    fun privacyOptionsRequired(): Boolean = graph.ads.privacyOptionsRequired
    fun showPrivacyOptions(activity: Activity) = graph.ads.showPrivacyOptions(activity)
}
