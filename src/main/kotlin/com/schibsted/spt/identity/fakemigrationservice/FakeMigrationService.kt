package com.schibsted.spt.identity.fakemigrationservice

import com.google.gson.GsonBuilder
import io.codearte.jfairy.Fairy
import io.codearte.jfairy.producer.person.Person
import org.joda.time.format.DateTimeFormat
import spark.Spark.exception
import spark.Spark.get
import spark.SparkBase.port
import java.text.DateFormat
import java.time.Instant
import java.util.*
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
 * - `+notfound`: Return a 404 Not Found response (will ignore options to return invalid data)
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
        val auth = req.headers("X-Auth")
        if (!authHeader.equals(auth)) {
            throw UnauthorizedException()
        }
        val email = req.queryParams("email")!!
        val tags = tags(email)
        maybeAddDelay(tags.singleOrNull { s -> s.matches(Regex("delay\\d+")) })
        if ("notfound" in tags) {
            throw NotFoundException();
        }
        newUser(email)
    }, { model -> gson.toJson(model) })

    get("/healthcheck", {req, res ->
        res.type("application/json")
        """{"status":"ok"}"""})

    exception(javaClass<UnauthorizedException>(), { e, req, res ->
        res.status(403)
        res.body("Missing/invalid auth header")
    })

    exception(javaClass<NotFoundException>(), { e, req, res ->
        res.status(404)
        res.body("User not found")
    })

}

class UnauthorizedException : RuntimeException() { }
class NotFoundException : RuntimeException() { }

val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create()
val fairy = Fairy.create()
val timeZones = TimeZone.getAvailableIDs()
val locales = Locale.getAvailableLocales()

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
        val timeZone: String?,
        val addresses: List<Address>?)

data class Address(
        val streetAddress: String?,
        val postalCode: String?,
        val country: String?,
        val locality: String?,
        val region: String?,
        val type: String?)
//        val latitude: Double?,
//        val longitude: Double?,
//        val altitude: Double?)

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

fun addressTypes(): List<String> {
    val types = arrayListOf("HOME", "DELIVERY", "WORK")
    return types.subList(0, fairy.baseProducer().randomInt(2))
}

fun createAddresses(types: List<String>): List<Address>? {
    val addresses = ArrayList<Address>()
    for (type in addressTypes()) {
        addresses.add(createAddress(type))
    }
    if (addresses.isEmpty()) {
        return null
    }
    return addresses
}

fun createAddress(type: String): Address {
    val address = fairy.person().getAddress()
    return Address(
            address.street() + " " + address.streetNumber(),
            address.getPostalCode(),
            "USA",
            address.getCity(),
            address.getCity(),
            type)
//            fairy.baseProducer().randomBetween(-90.0, 90.0),
//            fairy.baseProducer().randomBetween(-180.0, 180.0),
//            fairy.baseProducer().randomBetween(-394.0, 8848.0))
}

fun newUser(email: String): User {
    val person = fairy.person()
    val tags = tags(email)
    val phone = phone(person.telephoneNumber(), "invalidphone" in tags)

    return User(
            email(email, "modifyemail" in tags),
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
            timeZone("invalidtimezone" in tags),
            createAddresses(addressTypes()))
}
