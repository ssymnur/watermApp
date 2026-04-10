package com.example.watermapp.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.watermapp.BaseUrun
import java.util.UUID

object CartManager {

    var usdRate by mutableStateOf(32.0)
    var eurRate by mutableStateOf(35.0)

    var cart = mutableStateListOf<CartItem>()

    var appliedDiscountPercent by mutableStateOf(0.0)

    // KDV
    var kdvEnabled by mutableStateOf(false)
    var kdvRate by mutableStateOf(0.20) // %20

    // Proje ekleme

    fun addProject(projeAdi: String, urunler: List<BaseUrun>) {

        val newInstanceId = UUID.randomUUID().toString() // 🔥 HER EKLEMEDE YENİ ID

        urunler.forEach { urun ->

            cart.add(
                CartItem(
                    urun = urun,
                    quantity = mutableStateOf(1),
                    projectName = projeAdi,
                    projectInstanceId = newInstanceId // 💥 EN KRİTİK SATIR
                )
            )
        }
    }

    // İskonto uygulama
    fun applyDiscount(percent: Double) {
        appliedDiscountPercent = percent
    }

    fun rawTotalTL(): Double {
        return cart.sumOf { item ->
            val price = item.urun?.fiyat ?: 0.0
            val qty = item.quantity.value
            val tlPrice = when (item.urun?.paraBirimi) {
                "USD" -> price * usdRate
                "EUR" -> price * eurRate
                "TL" -> price
                else -> price
            }
            tlPrice * qty
        }
    }

    fun totalInTL(): Double {
        val afterDiscount = rawTotalTL() * (1 - appliedDiscountPercent / 100)
        return if (kdvEnabled) afterDiscount * (1 + kdvRate) else afterDiscount
    }

    fun totalsByCurrency(): Map<String, Double> {
        return cart
            .filter { it.urun != null }
            .groupBy { it.urun?.paraBirimi ?: "TL" }
            .mapValues { (_, items) ->
                items.sumOf { (it.urun?.fiyat ?: 0.0) * it.quantity.value }
            }
    }

    fun totalsWithDiscount(): Map<String, Double> {
        return totalsByCurrency().mapValues { (_, total) ->
            var discounted = total * (1 - appliedDiscountPercent / 100)
            if (kdvEnabled) discounted *= (1 + kdvRate)
            discounted
        }
    }

    fun add(urun: BaseUrun, projectName: String? = null) {
        val existing = cart.find { it.urun?.id == urun.id }
        if (existing != null) {
            existing.quantity.value++
        } else {
            cart.add(CartItem(urun = urun, quantity = mutableStateOf(1), projectName = projectName))
        }
    }

    fun remove(urun: BaseUrun) {
        val existing = cart.find { it.urun?.id == urun.id }
        existing?.let {
            if (it.quantity.value > 1) it.quantity.value-- else cart.remove(it)
        }
    }

    fun getQuantity(urun: BaseUrun): Int {
        return cart.find { it.urun?.id == urun.id }?.quantity?.value ?: 0
    }

    fun totalPrice(): Double {
        return cart.sumOf { (it.urun?.fiyat ?: 0.0) * it.quantity.value }
    }

    data class CartItem(
        val projectInstanceId: String? = null,
        val urun: BaseUrun?,
        val quantity: MutableState<Int> = mutableStateOf(1),
        val projectName: String? = null,
        val isProjectHeader: Boolean = false
    )

    object SessionManager {
        var isStaff by mutableStateOf(false)
    }

}