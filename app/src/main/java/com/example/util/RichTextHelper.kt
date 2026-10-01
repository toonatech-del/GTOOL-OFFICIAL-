package com.example.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.WarmGold

/**
 * Visual Transformation that dynamically renders Markdown tokens in real-time
 * without causing cursor offset drift or typing jitter.
 */
class MarkdownVisualTransformation(
    private val isDarkMode: Boolean = true
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder()
        
        // Regex patterns for bold, italic, and headings
        val boldRegex = Regex("""\*\*(.*?)\*\*""")
        val italicRegex = Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)""")
        val h1Regex = Regex("""^#\s+(.*)$""", RegexOption.MULTILINE)
        val h2Regex = Regex("""^##\s+(.*)$""", RegexOption.MULTILINE)

        builder.append(rawText)

        val delimiterColor = if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f)

        // Apply Bold styling to content inside **...** and subtly tint delimiters
        boldRegex.findAll(rawText).forEach { match ->
            builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            builder.addStyle(SpanStyle(color = delimiterColor), match.range.first, match.range.first + 2)
            builder.addStyle(SpanStyle(color = delimiterColor), match.range.last - 1, match.range.last + 1)
        }

        // Apply Italic styling to content inside *...*
        italicRegex.findAll(rawText).forEach { match ->
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            builder.addStyle(SpanStyle(color = delimiterColor), match.range.first, match.range.first + 1)
            builder.addStyle(SpanStyle(color = delimiterColor), match.range.last, match.range.last + 1)
        }

        // Apply Heading 1 styling
        h1Regex.findAll(rawText).forEach { match ->
            builder.addStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else Color.Black), match.range.first, match.range.last + 1)
            builder.addStyle(SpanStyle(color = delimiterColor), match.range.first, match.range.first + 2)
        }

        // Apply Heading 2 styling
        h2Regex.findAll(rawText).forEach { match ->
            builder.addStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black), match.range.first, match.range.last + 1)
            builder.addStyle(SpanStyle(color = delimiterColor), match.range.first, match.range.first + 3)
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/**
 * Utility functions for rich text editing and PDF canvas generation.
 */
object RichTextHelper {

    /**
     * Checks if current selection or cursor is inside bold formatting.
     */
    fun isBoldActive(current: TextFieldValue): Boolean {
        val text = current.text
        val cursor = current.selection.start
        if (text.isEmpty()) return false
        val before = text.substring(0, cursor)
        val after = text.substring(cursor)
        val starsBefore = before.count { it == '*' }
        val starsAfter = after.count { it == '*' }
        return starsBefore % 4 >= 2 && starsAfter % 4 >= 2
    }

    /**
     * Checks if current selection or cursor is inside italic formatting.
     */
    fun isItalicActive(current: TextFieldValue): Boolean {
        val text = current.text
        val cursor = current.selection.start
        if (text.isEmpty()) return false
        val before = text.substring(0, cursor)
        val after = text.substring(cursor)
        val starsBefore = before.count { it == '*' }
        val starsAfter = after.count { it == '*' }
        return (starsBefore % 2 == 1) && (starsAfter % 2 == 1)
    }

    /**
     * Toggles Bold (**):
     * - If text is selected: bolds or unbolds selection.
     * - If no text selected: toggles bold typing mode by inserting/removing ****.
     */
    fun toggleBold(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection

        if (selection.start != selection.end) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selectedText = text.substring(start, end)

            // Check if already selected with **
            if (selectedText.startsWith("**") && selectedText.endsWith("**") && selectedText.length >= 4) {
                val unwrapped = selectedText.substring(2, selectedText.length - 2)
                val newText = text.replaceRange(start, end, unwrapped)
                return TextFieldValue(newText, TextRange(start, start + unwrapped.length))
            }

            // Check if boundary has ** right outside
            if (start >= 2 && end + 2 <= text.length && text.substring(start - 2, start) == "**" && text.substring(end, end + 2) == "**") {
                val newText = text.substring(0, start - 2) + selectedText + text.substring(end + 2)
                return TextFieldValue(newText, TextRange(start - 2, (start - 2) + selectedText.length))
            }

            // Otherwise, wrap in **
            val newText = text.replaceRange(start, end, "**$selectedText**")
            val newStart = start + 2
            val newEnd = newStart + selectedText.length
            return TextFieldValue(newText, TextRange(newStart, newEnd))
        } else {
            val cursor = selection.start.coerceIn(0, text.length)
            // If cursor is right between ****, remove them
            if (cursor >= 2 && cursor + 2 <= text.length && text.substring(cursor - 2, cursor + 2) == "****") {
                val newText = text.removeRange(cursor - 2, cursor + 2)
                return TextFieldValue(newText, TextRange(cursor - 2, cursor - 2))
            }
            // Insert **** and place cursor in the middle
            val newText = StringBuilder(text).insert(cursor, "****").toString()
            val newCursorPos = cursor + 2
            return TextFieldValue(newText, TextRange(newCursorPos, newCursorPos))
        }
    }

    /**
     * Toggles Italic (*):
     * - If text is selected: italicizes or un-italicizes selection.
     * - If no text selected: toggles italic typing mode by inserting/removing **.
     */
    fun toggleItalic(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection

        if (selection.start != selection.end) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selectedText = text.substring(start, end)

            // Check if selected with *
            if (selectedText.startsWith("*") && selectedText.endsWith("*") && selectedText.length >= 2 && !selectedText.startsWith("**")) {
                val unwrapped = selectedText.substring(1, selectedText.length - 1)
                val newText = text.replaceRange(start, end, unwrapped)
                return TextFieldValue(newText, TextRange(start, start + unwrapped.length))
            }

            // Check if boundary has * outside
            if (start >= 1 && end + 1 <= text.length && text[start - 1] == '*' && text[end] == '*' && (start < 2 || text[start - 2] != '*')) {
                val newText = text.substring(0, start - 1) + selectedText + text.substring(end + 1)
                return TextFieldValue(newText, TextRange(start - 1, (start - 1) + selectedText.length))
            }

            val newText = text.replaceRange(start, end, "*$selectedText*")
            val newStart = start + 1
            val newEnd = newStart + selectedText.length
            return TextFieldValue(newText, TextRange(newStart, newEnd))
        } else {
            val cursor = selection.start.coerceIn(0, text.length)
            // If cursor is right between **, remove them
            if (cursor >= 1 && cursor + 1 <= text.length && text.substring(cursor - 1, cursor + 1) == "**" && (cursor < 2 || text[cursor - 2] != '*')) {
                val newText = text.removeRange(cursor - 1, cursor + 1)
                return TextFieldValue(newText, TextRange(cursor - 1, cursor - 1))
            }
            val newText = StringBuilder(text).insert(cursor, "**").toString()
            val newCursorPos = cursor + 1
            return TextFieldValue(newText, TextRange(newCursorPos, newCursorPos))
        }
    }

    /**
     * Clears rich-text formatting (T button).
     */
    fun clearFormatting(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection

        if (selection.start != selection.end) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selectedText = text.substring(start, end)
            val cleaned = selectedText
                .replace("**", "")
                .replace("*", "")
                .replace("## ", "")
                .replace("# ", "")
                .replace("• ", "")
                .replace(Regex("""^\d+\.\s+"""), "")
                .replace("[ ] ", "")
                .replace("> ", "")
            val newText = text.replaceRange(start, end, cleaned)
            return TextFieldValue(newText, TextRange(start, start + cleaned.length))
        } else {
            val cleaned = text
                .replace("**", "")
                .replace("*", "")
                .replace("## ", "")
                .replace("# ", "")
                .replace("• ", "")
                .replace(Regex("""(?m)^\d+\.\s+"""), "")
                .replace("[ ] ", "")
                .replace("> ", "")
            val newCursor = current.selection.start.coerceIn(0, cleaned.length)
            return TextFieldValue(cleaned, TextRange(newCursor, newCursor))
        }
    }

    /**
     * Applies markdown formatting around selected text or inserts token at cursor position.
     */
    fun applyFormatting(
        current: TextFieldValue,
        prefix: String,
        suffix: String = ""
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection

        return if (selection.start != selection.end) {
            // Text is actively highlighted by user: wrap only the selected portion
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selectedText = text.substring(start, end)
            val newText = text.replaceRange(start, end, "$prefix$selectedText$suffix")
            val newCursorPos = start + prefix.length + selectedText.length + suffix.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos, newCursorPos)
            )
        } else {
            // No selection: Insert formatting wrapper and place cursor inside between prefix & suffix
            val cursor = selection.start.coerceIn(0, text.length)
            val newText = StringBuilder(text).insert(cursor, "$prefix$suffix").toString()
            val newCursorPos = cursor + prefix.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos, newCursorPos)
            )
        }
    }

    /**
     * Converts inline markdown **bold** tokens into a real Android Spannable
     * for crisp, high-fidelity PDF canvas text drawing.
     */
    fun createSpannedText(
        rawLine: String,
        primaryColorInt: Int,
        bodyColorInt: Int
    ): CharSequence {
        val ssb = SpannableStringBuilder()
        
        // Handle H1 Heading (# )
        if (rawLine.trimStart().startsWith("# ")) {
            val content = rawLine.trimStart().removePrefix("# ").trim()
            val start = ssb.length
            ssb.append(content)
            val end = ssb.length
            ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(ForegroundColorSpan(primaryColorInt), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(AbsoluteSizeSpan(22, true), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            return ssb
        }

        // Handle H2 Heading (## )
        if (rawLine.trimStart().startsWith("## ")) {
            val content = rawLine.trimStart().removePrefix("## ").trim()
            val start = ssb.length
            ssb.append(content)
            val end = ssb.length
            ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(ForegroundColorSpan(primaryColorInt), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(AbsoluteSizeSpan(18, true), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            return ssb
        }

        // Robust manual parsing for Bold (**...**) and Italic (*...*) to ensure no raw symbols are drawn
        var i = 0
        while (i < rawLine.length) {
            if (rawLine.startsWith("**", i)) {
                val end = rawLine.indexOf("**", i + 2)
                if (end != -1) {
                    val content = rawLine.substring(i + 2, end)
                    val startSpan = ssb.length
                    ssb.append(content)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), startSpan, ssb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(primaryColorInt), startSpan, ssb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 2
                    continue
                }
            }
            if (rawLine.startsWith("*", i)) {
                val end = rawLine.indexOf("*", i + 1)
                if (end != -1) {
                    val content = rawLine.substring(i + 1, end)
                    val startSpan = ssb.length
                    ssb.append(content)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), startSpan, ssb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(bodyColorInt), startSpan, ssb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 1
                    continue
                }
            }
            ssb.append(rawLine[i])
            i++
        }

        return if (ssb.isEmpty()) rawLine else ssb
    }
}

fun String.cleanAsterisks(): String = this.replace("*", "").replace("#", "").trim()
