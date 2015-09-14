package com.schibsted.spt.identity.fakemigrationservice

import com.google.gson.GsonBuilder
import io.codearte.jfairy.Fairy
import org.joda.time.format.DateTimeFormat
import spark.Spark.get
import spark.SparkBase.port
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.text.Regex

/**
 * Implementation of a user data migration service returning fake data.
 *
 * The behaviour of the fake migration service can be controlled using
 * sub-addressing. By appending `+tag` to the user part of the address,
 * the behaviour can be controlled in the following way:
 *
 * - `+delayN`: Wait N milliseconds before responding, e.g. `+delay2500` to wait 2.5 s.
 * - `+invalidlocale`: Return an invalid locale in the user data.
 * - `+invalidphone`: Return an invalid phone number format in the user data.
 * - `+invalidsex`: Return invalid sex data in the user data.
 * - `+invalidtimezone`: Return an invalid timezone in the user data.
 * - `+modifyemail`: Modify the email address in the user data.
 *
 * These can also be combined by separating multiple tags with dashes.
 * To delay the response and return an invalid timezone, the requester
 * can send a request for `jane.doe+delay2500-invalidtimezone@example.com`.
 */
fun main(args: Array<String>) {

    port(getPort())

    val authHeader = getAuth()

    get("/", { req, res ->
        res.type("application/json")
        val email = req.queryParams("email")!!
        maybeAddDelay(tags(email).singleOrNull { s -> s.matches(Regex("delay\\d+")) })
        val auth = req.headers("X-Auth")!!
        if (auth.equals(authHeader)) {
            newUser(email)
        } else {
            Error("Bad auth header")
        }

    }, { model -> gson.toJson(model) })

    get("/healthcheck", {req, res ->
        res.type("application/json")
        """{"status":"ok"}"""})

}

val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create()
val fairy = Fairy.create()
val timeZones = TimeZone.getAvailableIDs()
val locales = Locale.getAvailableLocales()

data class Error(
        val message: String)

data class User(
        val email: String?,
        val birthday: String?,
        val userId: String?,
        val displayName: String?,
        val sex: String?,
        val locale: String?,
        val fullName: String?,
        val mobilePhone: String?,
        val homePhone: String?,
        val photo: String?,
        val createdTime: Date?,
        val timeZone: String?)

fun maybeAddDelay(delayTag: String?) {
    delayTag?.let { Thread.sleep(delayTag!!.substringAfter("delay").toLong()) }
}

fun getPort(): Int {
    val port = System.getenv("PORT")
    return Integer.parseInt(port ?: "9091")
}

fun getAuth(): String {
    val auth = System.getenv("AUTH")
    return auth ?: "BEEFC4FFEE"
}

fun <T> random(items: Array<T>): T {
    val i = fairy.baseProducer().randomInt(items.size() - 1)
    return items.get(i)
}

fun sex(invalid: Boolean): String? {
    return if (invalid) """¯\_(ツ)_/¯""" else when (fairy.baseProducer().randomInt(100)) {
        in 0..24 -> "FEMALE"
        in 25..49 -> "MALE"
        in 50..74 -> "UNDISCLOSED"
        else -> null
    }
}

fun timeZone(invalid: Boolean): String? {
    return if (invalid) "Not a timezone, obviously" else when (fairy.baseProducer().randomInt(100)) {
        in 0..49 -> random(timeZones).toString()
        else -> null
    }
}

fun locale(invalid: Boolean): String? {
    return if (invalid) "Not a locale, obviously" else when (fairy.baseProducer().randomInt(100)) {
        in 0..20 -> random(locales).toString()
        else -> null
    }
}

fun email(email: String, modify: Boolean): String {
    return if (modify) "xxx" + email else email
}

fun phone(phone: String, invalid: Boolean): String {
    val validPhone = "+" + phone.replace("""-""", "")
    return if (invalid) phone else validPhone
}

fun tags(email: String): List<String> {
    val local = email.splitBy("@")[0].splitBy("+", limit = 2)
    if (local.size() < 2) return emptyList()
    return local[1].splitBy("-")
}

fun newUser(email: String): User {
    val person = fairy.person()
    val tags = tags(email)
    val phone = phone(person.telephoneNumber(), "invalidphone" in tags)
    return User(
            email(email, tags.contains("modifyemail")),
            DateTimeFormat.forPattern("yyyy-MM-dd").print(person.dateOfBirth()),
            person.nationalIdentificationNumber(),
            person.username(),
            sex("invalidsex" in tags),
            locale("invalidlocale" in tags),
            person.fullName(),
            phone,
            phone,
            fairy.company().url(),
            Date.from(Instant.now()),
            timeZone("invalidtimezone" in tags))
}
