package fr.shikkanime

import java.text.Normalizer
import java.text.Normalizer.Form
import java.util.regex.Pattern

private val NONLATIN: Pattern = Pattern.compile("[^\\w-]")
private val WHITESPACE: Pattern = Pattern.compile("\\s")

fun toSlug(input: String): String {
    val nowhitespace: String = WHITESPACE.matcher(input).replaceAll("-")
    val normalized: String = Normalizer.normalize(nowhitespace, Form.NFD)
    val slug: String = NONLATIN.matcher(normalized).replaceAll("")
    return slug.lowercase()
}

fun main() {
    println(toSlug("Metallic Rouge"))
    println(toSlug("BANISHED FROM THE HERO'S PARTY"))
    println(toSlug("'TIS TIME FOR \"TORTURE,\" PRINCESS"))
    println(toSlug("HOKKAIDO GALS ARE SUPER ADORABLE!"))
}