package com.example.watermapp.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {

    private val symbols = DecimalFormatSymbols(Locale("tr", "TR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,##0.00", symbols)

    fun tl(value: Double): String =
        formatter.format(value) + " ₺"

    fun usd(value: Double): String =
        formatter.format(value) + " $"

    fun eur(value: Double): String =
        formatter.format(value) + " €"
}