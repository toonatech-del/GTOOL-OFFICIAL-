package com.example.util

import com.example.model.ChatMessage
import com.example.model.MessageSender

object AssistantEngine {

    /**
     * Layer 1: Natural Chat & Greetings
     * Returns a conversational response if the query matches common greetings or identity questions.
     */
    fun getGreetingResponse(query: String): String? {
        val q = query.lowercase().trim()
        return when {
            q == "hi" || q == "hello" || q == "hey" || q == "greeting" || q == "greetings" -> 
                "Hello! I'm your GTOOL X offline assistant. How can I help you manage your notes and documents today?"
            q.contains("how are you") || q.contains("how's it going") -> 
                "I'm functioning perfectly and ready to help you search your encrypted vault. What are we looking for?"
            q == "help" || q.contains("what can you do") || q == "commands" || q == "info" -> 
                "I can search through your notes, PDFs, and scanned images entirely offline. Try searching for keywords like 'plan expiry', 'bill', or specific dates like '30 sep'."
            q.contains("who are you") || q.contains("your name") || q.contains("what is this") -> 
                "I am the GTOOL X Assistant, a strictly offline AI engine designed to keep your data private while providing smart search and problem-solving capabilities."
            else -> null
        }
    }

    /**
     * Cleans text by removing markdown-style formatting symbols like asterisks for a cleaner UI.
     */
    fun formatCleanText(text: String): String {
        return text.replace("**", "")
            .replace("*", "")
            .replace("###", "")
            .replace("##", "")
            .replace("#", "")
            .replace("`", "")
            .trim()
    }

    /**
     * Layer 2: Problem Solving & Multi-Token Logic
     * Splits query into tokens to run more flexible matching.
     */
    fun getSearchTokens(query: String): List<String> {
        return query.lowercase()
            .replace(Regex("[^a-z0-9\\s/]"), "") // Keep slashes for dates
            .split("\\s+".toRegex())
            .filter { it.length >= 2 }
    }
}
