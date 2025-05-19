package com.example.wordboost.data.util

import android.util.Log
import androidx.compose.ui.text.TextRange
import java.text.BreakIterator
import java.util.Locale

fun findSentenceBoundaries(text: String, characterOffset: Int, locale: Locale): TextRange? {
    if (text.isEmpty()) return null
    val safeOffset = characterOffset.coerceIn(0, text.length)

    val iterator = BreakIterator.getSentenceInstance(locale)
    iterator.setText(text)

    var start = 0
    var end = 0

    if (safeOffset == text.length && text.isNotEmpty()) {
        end = iterator.last()
        start = iterator.previous()
        if (start == BreakIterator.DONE) start = 0
    } else {
        end = iterator.following(safeOffset)
        if (end == BreakIterator.DONE) {
            end = text.length
        }
        start = iterator.previous()
        if (start == BreakIterator.DONE) {
            start = 0
        }
    }
    if (start <= safeOffset && safeOffset <= end && start < end) {
        return TextRange(start, end)
    } else if (start == 0 && end == text.length && text.isNotEmpty()){
        return TextRange(start, end)
    }

    if (iterator.isBoundary(safeOffset) && safeOffset != 0) {
        val boundaryEnd = safeOffset
        val boundaryStart = iterator.preceding(safeOffset)
        if (boundaryStart != BreakIterator.DONE && boundaryStart < boundaryEnd) {
            return TextRange(boundaryStart, boundaryEnd)
        }
    }
    Log.w("findSentenceBoundaries", "Could not accurately determine sentence for offset: $safeOffset")
    return null
}