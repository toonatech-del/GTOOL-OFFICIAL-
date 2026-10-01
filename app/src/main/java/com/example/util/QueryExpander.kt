package com.example.util

/**
 * Intelligent Synonym Expansion & Semantic Intent Resolver.
 * Maps Hinglish, colloquial terms, and Hindi queries to domain-specific keywords for
 * telecom, expiry dates, bills & payments, IDs, etc.
 */
object QueryExpander {
    private val synonymGroups = listOf(
        // Telecom / Recharge domain
        setOf("recharge", "plan", "pack", "validity", "valid", "data", "jio", "airtel", "vi", "bsnl", "sim", "telecom"),
        
        // Expiry / Termination domain
        setOf("khtm", "khatam", "expire", "expiry", "expires", "end", "ending", "valid till", "due", "last date", "samapt", "over"),
        
        // Bills & Payments domain
        setOf("bill", "bijli", "electricity", "water", "broadband", "wifi", "emi", "installment", "rent", "credit card"),

        // Identity domain
        setOf("pan", "aadhaar", "uidai", "passport", "license", "licence", "voter")
    )

    fun expandQuery(spokenQuery: String): List<String> {
        val words = spokenQuery.lowercase().split("\\s+".toRegex())
        val expandedKeywords = mutableSetOf<String>()

        for (word in words) {
            var matched = false
            for (group in synonymGroups) {
                if (group.contains(word)) {
                    expandedKeywords.addAll(group)
                    matched = true
                }
            }
            if (!matched && word.length > 2 && !isStopWord(word)) {
                expandedKeywords.add(word)
            }
        }
        return expandedKeywords.toList()
    }

    fun isStopWord(word: String): Boolean {
        val stopWords = setOf("mera", "meri", "mere", "kab", "hoga", "hai", "ka", "ki", "ke", "ko", "set", "reminder", "batao", "dhundo", "mujhe", "please", "plz", "bata")
        return stopWords.contains(word.lowercase())
    }

    /**
     * Checks if the expanded query indicates a recharge / plan query
     */
    fun isRechargeQuery(query: String): Boolean {
        val expanded = expandQuery(query)
        val rechargeSynonyms = setOf("recharge", "plan", "pack", "validity", "valid", "jio", "airtel", "vi", "bsnl", "sim", "telecom")
        return expanded.any { rechargeSynonyms.contains(it) }
    }

    /**
     * Checks if the expanded query indicates an expiry / termination query
     */
    fun isExpiryQuery(query: String): Boolean {
        val expanded = expandQuery(query)
        val expirySynonyms = setOf("khtm", "khatam", "expire", "expiry", "expires", "end", "ending", "valid till", "due", "last date", "samapt", "over")
        return expanded.any { expirySynonyms.contains(it) }
    }
}
