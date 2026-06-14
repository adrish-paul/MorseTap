package com.example.morsetap

object MorseTranslator {
    private val charToMorse = mapOf(
        'A' to ".-",   'B' to "-...", 'C' to "-.-.", 'D' to "-..",  'E' to ".",    'F' to "..-.",
        'G' to "--.",  'H' to "....", 'I' to "..",   'J' to ".---", 'K' to "-.-",  'L' to ".-..",
        'M' to "--",   'N' to "-.",   'O' to "---",  'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...",  'T' to "-",    'U' to "..-",  'V' to "...-", 'W' to ".--",  'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
        '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-.", '-' to "-....-",
        '(' to "-.--.",  ')' to "-.--.-", '@' to ".--.-.", '=' to "-...-"
    )

    private val morseToChar = charToMorse.entries.associate { it.value to it.key }

    /** Decode a single Morse sequence (e.g. ".-") → character string. Returns "?" if unknown. */
    fun decodeLetter(morseLetter: String): String {
        if (morseLetter.isEmpty()) return ""
        return (morseToChar[morseLetter] ?: '?').toString()
    }

    /** Full alphabet list for the dictionary sheet. */
    fun getAlphabet(): List<Pair<String, String>> = charToMorse.map { it.key.toString() to it.value }
}
