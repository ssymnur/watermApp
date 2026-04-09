package com.example.watermapp.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.watermapp.BaseUrun

    data class OrderItemDetail(
        val model: String,
        val currency: String,
        val originalPrice: Double,
        val priceTL: Double,
        val quantity: Int,
        val totalTL: Double
    )
    data class Customer(
        val customerName: String = "",
        val companyName: String = "",
        val phone: String = "",
        val deviceName: String = "",
        val email: String = "",
        val staffName: String = "",
        val note: String = "",
        val address: String = ""
    )

    data class OrderItem(
        val model: String = "",
        val quantity: Int = 0,
        val price: Double = 0.0,
        val currency: String = "USD"
    )

    data class Order(

        val customer: Customer = Customer(),
        val items: List<OrderItem> = emptyList(),

        val discountPercent: Double = 0.0,
        val kdvPercent: Double = 20.0,

        val baseTotalTL: Double = 0.0,
        val discountedTotalTL: Double = 0.0,
        val kdvAmountTL: Double = 0.0,
        val grandTotalTL: Double = 0.0,

        val baseTotals: Map<String, Double> = emptyMap(),
        val discountedTotals: Map<String, Double> = emptyMap(),

        val totalTL: Double = 0.0,

        val usdRate: Double = 0.0,
        val eurRate: Double = 0.0,

        val timestamp: Long = 0,
        val createdAt: Long = 0

    )

    data class CartItem(
        val urun: BaseUrun,
        val projeAdi: String? = null,
        var quantity: MutableState<Int> = mutableStateOf(1)
    )

data class OrderDoc(
    val id: String,
    val order: Order
)
