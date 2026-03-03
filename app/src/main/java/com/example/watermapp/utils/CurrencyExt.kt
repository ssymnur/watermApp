package com.example.watermapp.utils

import java.util.Locale

fun Double.toTL(): String =
    "%,.2f ₺".format(Locale.getDefault(), this)

fun Double.toCurrency(symbol: String = "₺"): String {
    return String.format(Locale.getDefault(), "%,.2f $symbol", this)
}