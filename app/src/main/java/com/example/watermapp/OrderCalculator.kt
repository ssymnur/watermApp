package com.example.watermapp

object OrderCalculator {

    fun calculateOrder(
        customer: Customer,
        items: List<OrderItem>,
        discountPercent: Double,
        kdvPercent: Double,
        usdRate: Double,
        eurRate: Double
    ): Order {

        val itemDetails = items.map { item ->

            val priceTL = when (item.currency) {
                "USD" -> item.price * usdRate
                "EUR" -> item.price * eurRate
                else -> item.price
            }

            OrderItemDetail(
                model = item.model,
                currency = item.currency,
                originalPrice = item.price,
                priceTL = priceTL,
                quantity = item.quantity,
                totalTL = priceTL * item.quantity
            )
        }

        val baseTotalTL = itemDetails.sumOf { it.totalTL }

        val discountedTotalTL =
            baseTotalTL * (1 - discountPercent / 100)

        val kdvAmount =
            discountedTotalTL * (kdvPercent / 100)

        val grandTotal =
            discountedTotalTL + kdvAmount

        return Order(
            customer = customer,
            items = items,
            discountPercent = discountPercent,
            kdvPercent = kdvPercent,
            baseTotalTL = baseTotalTL,
            discountedTotalTL = discountedTotalTL,
            kdvAmountTL = kdvAmount,
            totalTL = grandTotal,
            usdRate = usdRate,
            eurRate = eurRate
        )
    }
}