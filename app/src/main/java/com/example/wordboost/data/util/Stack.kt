package com.example.wordboost.data.util


class Stack<T> {
    private val elements: MutableList<T> = mutableListOf()
    fun push(item: T) = elements.add(item)
    fun pop(): T? = if (elements.isNotEmpty()) elements.removeAt(elements.size - 1) else null
    fun peek(): T? = elements.lastOrNull()
    fun isEmpty(): Boolean = elements.isEmpty()
    fun isNotEmpty(): Boolean = elements.isNotEmpty()
    fun size(): Int = elements.size
    fun clear() = elements.clear()
}