package com.seymanur.watermapp.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.seymanur.watermapp.OrderCalculator
import com.seymanur.watermapp.data.CartManager
import com.seymanur.watermapp.model.*

class OrderViewModel : ViewModel() {

    var cart = mutableStateListOf<CartItem>()
        private set

    var totalTL by mutableStateOf(0.0)
        private set

    var appliedDiscountPercent by mutableStateOf(0.0)
    var usdRate by mutableStateOf(1.0)
    var eurRate by mutableStateOf(1.0)

    fun calculateTotal(customer: Customer = Customer()) {
        val items = cart.map {
            OrderItem(
                model = it.urun?.urun_adi ?: "Bilinmeyen Ürün",
                quantity = it.quantity.value,
                price = it.urun?.fiyat ?: 0.0,
                currency = it.urun?.paraBirimi ?: "USD"
            )
        }

        val order = OrderCalculator.calculateOrder(
            customer = customer,
            items = items,
            discountPercent = appliedDiscountPercent,
            kdvPercent = 20.0,
            usdRate = usdRate,
            eurRate = eurRate
        )

        totalTL = order.totalTL
    }

    fun createOrder(customer: Customer): Order {
        return OrderCalculator.calculateOrder(
            customer = customer,
            items = CartManager.cart.map {
                OrderItem(
                    model = it.urun?.urun_adi ?: "Bilinmeyen Ürün",
                    quantity = it.quantity.value,
                    price = it.urun?.fiyat ?: 0.0,
                    currency = it.urun?.paraBirimi ?: "USD"
                )
            },
            discountPercent = CartManager.appliedDiscountPercent,
            kdvPercent = 20.0,
            usdRate = CartManager.usdRate,
            eurRate = CartManager.eurRate
        )
    }
}