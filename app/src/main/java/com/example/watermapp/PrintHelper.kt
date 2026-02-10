package com.example.watermapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.View

fun printView(context: Context, view: View) {

    val bitmap = Bitmap.createBitmap(
        view.width,
        view.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    view.draw(canvas)

    val printManager =
        context.getSystemService(Context.PRINT_SERVICE) as PrintManager

    val adapter = BitmapPrintAdapter(context, bitmap)

    printManager.print(
        "Sepet Yazdır",
        adapter,
        PrintAttributes.Builder().build()
    )
}
