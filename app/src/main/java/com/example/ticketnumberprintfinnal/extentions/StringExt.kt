package com.example.ticketnumberprintfinnal.extentions

import androidx.core.text.isDigitsOnly

fun String.selectNumberText(): String {
    return this.split(" ").map {
        it.trim()
    }.filter {
        it.isDigitsOnly()
    }.first()
}