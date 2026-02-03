package com.example.watermapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Product(
    var name: String,
    var code: String,
    var model: String,
    price: Double
) {
    var price by mutableStateOf(price)
    var quantity = mutableStateOf(0)
}

