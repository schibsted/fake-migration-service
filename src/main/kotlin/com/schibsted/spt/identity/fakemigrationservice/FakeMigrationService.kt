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

fun main(args: Array<String>) {

    port(getPort())

    val authHeader = getAuth()

    get("/", { req, res ->
        maybeSleep()
        res.type("application/json")
        val email = req.queryParams("email")!!
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

fun maybeSleep() {
    when (fairy.baseProducer().randomInt(100)) {
        in 0..6 -> Thread.sleep(1000)
        in 7..9 -> Thread.sleep(2500)
        else -> {}
    }
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

fun sex(): String? {
    return when (fairy.baseProducer().randomInt(100)) {
        in 0..14 -> "FEMALE"
        in 15..29 -> "MALE"
        in 30..44 -> "UNDISCLOSED"
        in 45..50 -> """¯\_(ツ)_/¯"""
        else -> null
    }
}

fun status(): String? {
    return when (fairy.baseProducer().randomInt(100)) {
        in 0..14 -> "VERIFIED"
        in 15..29 -> "BLOCKED"
        in 30..44 -> "DISABLED"
        in 45..49 -> "DELETED"
        in 50..54 -> "UNVERIFIED"
        in 55..60 -> "ᕕ( ᐛ )ᕗ"
        else -> null
    }
}

fun timeZone(): String? {
    return when (fairy.baseProducer().randomInt(100)) {
        in 0..14 -> random(timeZones).toString()
        in 15..20 -> "Not a timezone, obviously"
        else -> null
    }
}

fun locale(): String? {
    return when (fairy.baseProducer().randomInt(100)) {
        in 0..14 -> random(locales).toString()
        in 15..20 -> "Not a locale, obviously"
        else -> null
    }
}

fun email(email: String): String {
    return when (fairy.baseProducer().randomInt(100)) {
        in 0..90 -> email
        else -> "xxx" + email
    }
}

fun newUser(email: String): User {
    val person = fairy.person()
    val invalidPhone = person.telephoneNumber()
    val validPhone = "+" + invalidPhone.replace("""-""", "")
    val phone = fairy.baseProducer().randomElement(invalidPhone, validPhone)
    return User(
            email(email),
            DateTimeFormat.forPattern("yyyy-MM-dd").print(person.dateOfBirth()),
            person.nationalIdentificationNumber(),
            person.username(),
            sex(),
            locale(),
            person.fullName(),
            phone,
            phone,
            fairy.company().url(),
            Date.from(Instant.now()),
            timeZone())
}
