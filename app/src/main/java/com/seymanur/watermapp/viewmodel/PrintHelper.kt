package com.seymanur.watermapp

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import com.seymanur.watermapp.model.Order

fun printOrder(
    context: Context,
    order: Order
) {

    val printManager =
        context.getSystemService(
            Context.PRINT_SERVICE
        ) as PrintManager

    val printAdapter =
        OrderPrintAdapter(
            context = context,
            order = order
        )

    val attributes =
        PrintAttributes.Builder()
            .setMediaSize(
                PrintAttributes.MediaSize.ISO_A4
            )
            .setColorMode(
                PrintAttributes.COLOR_MODE_COLOR
            )
            .setMinMargins(
                PrintAttributes.Margins.NO_MARGINS
            )
            .build()

    printManager.print(
        "Müşteri Kaydı - ${
            order.customer.customerName.ifBlank {
                "Müşteri"
            }
        }",
        printAdapter,
        attributes
    )
}