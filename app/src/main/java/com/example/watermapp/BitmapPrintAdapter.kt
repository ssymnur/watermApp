package com.example.watermapp

import android.content.Context
import android.graphics.Bitmap
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileOutputStream

class BitmapPrintAdapter(
    private val context: Context,
    private val bitmap: Bitmap
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: android.print.PrintAttributes?,
        newAttributes: android.print.PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        callback?.onLayoutFinished(
            PrintDocumentInfo.Builder("sepet.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<android.print.PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        FileOutputStream(destination.fileDescriptor).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
    }
}
