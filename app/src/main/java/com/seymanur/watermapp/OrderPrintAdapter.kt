package com.seymanur.watermapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.seymanur.watermapp.model.Order
import java.io.FileOutputStream
import java.util.Locale


class OrderPrintAdapter(
    private val context: Context,
    private val order: Order
) : PrintDocumentAdapter() {

    private var pageCount = 1

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {

        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(
            "musteri_kaydi.pdf"
        )
            .setContentType(
                PrintDocumentInfo.CONTENT_TYPE_DOCUMENT
            )
            .setPageCount(
                PrintDocumentInfo.PAGE_COUNT_UNKNOWN
            )
            .build()

        callback?.onLayoutFinished(
            info,
            true
        )
    }


    override fun onWrite(
        pages: Array<PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {

        val document = PdfDocument()

        try {

            // =====================================================
            // A4 SAYFA BOYUTU
            // =====================================================

            val pageWidth = 595
            val pageHeight = 842

            val leftMargin = 40f
            val rightMargin = 40f
            val topMargin = 40f
            val bottomMargin = 40f

            val contentWidth =
                pageWidth - leftMargin - rightMargin

            // =====================================================
            // PAINT
            // =====================================================

            val normalPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.BLACK
                textSize = 11f
                typeface =
                    Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL
                    )
            }

            val boldPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.BLACK
                textSize = 11f
                typeface =
                    Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                    )
            }

            val titlePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.BLACK
                textSize = 20f
                typeface =
                    Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                    )
            }

            val sectionPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.BLACK
                textSize = 13f
                typeface =
                    Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                    )
            }

            val smallPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.DKGRAY
                textSize = 9f
                typeface =
                    Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL
                    )
            }

            val greenPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.rgb(
                    46,
                    125,
                    50
                )

                textSize = 14f

                typeface =
                    Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                    )
            }

            // =====================================================
            // DEĞİŞKENLER
            // =====================================================

            var pageNumber = 1

            var pageInfo =
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()

            var page =
                document.startPage(pageInfo)

            var canvas: Canvas = page.canvas

            var y = topMargin


            // =====================================================
            // YENİ SAYFA FONKSİYONU
            // =====================================================

            fun startNewPage() {

                document.finishPage(page)

                pageNumber++

                pageInfo =
                    PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        pageNumber
                    ).create()

                page =
                    document.startPage(pageInfo)

                canvas =
                    page.canvas

                y = topMargin
            }


            // =====================================================
            // SAYFA KONTROLÜ
            // =====================================================

            fun checkSpace(
                neededHeight: Float
            ) {

                if (
                    y + neededHeight >
                    pageHeight - bottomMargin
                ) {

                    startNewPage()
                }
            }


            // =====================================================
            // BAŞLIK
            // =====================================================

            canvas.drawColor(Color.WHITE)

            canvas.drawText(
                "WATERMAPP",
                leftMargin,
                y,
                titlePaint
            )

            y += 28f

            canvas.drawText(
                "MÜŞTERİ KAYDI",
                leftMargin,
                y,
                sectionPaint
            )

            y += 22f

            canvas.drawLine(
                leftMargin,
                y,
                pageWidth - rightMargin,
                y,
                normalPaint
            )

            y += 25f


            // =====================================================
            // MÜŞTERİ BİLGİLERİ
            // =====================================================

            canvas.drawText(
                "MÜŞTERİ BİLGİLERİ",
                leftMargin,
                y,
                sectionPaint
            )

            y += 20f


            fun drawCustomerRow(
                label: String,
                value: String
            ) {

                checkSpace(18f)

                canvas.drawText(
                    "$label:",
                    leftMargin,
                    y,
                    boldPaint
                )

                canvas.drawText(
                    value.ifBlank { "-" },
                    leftMargin + 100f,
                    y,
                    normalPaint
                )

                y += 17f
            }


            drawCustomerRow(
                "Müşteri",
                order.customer.customerName
            )

            drawCustomerRow(
                "Şirket",
                order.customer.companyName
            )

            drawCustomerRow(
                "Telefon",
                order.customer.phone
            )

            drawCustomerRow(
                "Cihaz",
                order.customer.deviceName
            )

            drawCustomerRow(
                "E-posta",
                order.customer.email
            )

            drawCustomerRow(
                "Personel",
                order.customer.staffName
            )

            drawCustomerRow(
                "Adres",
                order.customer.address
            )

            drawCustomerRow(
                "Not",
                order.customer.note
            )

            drawCustomerRow(
                "Tarih",
                formatDate(order.timestamp)
            )


            y += 15f


            // =====================================================
            // ÜRÜNLER
            // =====================================================

            checkSpace(50f)

            canvas.drawText(
                "ÜRÜNLER",
                leftMargin,
                y,
                sectionPaint
            )

            y += 20f


            // TABLO BAŞLIĞI

            canvas.drawText(
                "Ürün",
                leftMargin,
                y,
                boldPaint
            )

            canvas.drawText(
                "Miktar",
                leftMargin + 300f,
                y,
                boldPaint
            )

            canvas.drawText(
                "Birim Fiyat",
                leftMargin + 360f,
                y,
                boldPaint
            )

            y += 8f

            canvas.drawLine(
                leftMargin,
                y,
                pageWidth - rightMargin,
                y,
                normalPaint
            )

            y += 18f


            // =====================================================
            // ÜRÜNLERİ YAZ
            // =====================================================

            order.items.forEach { item ->

                checkSpace(35f)

                // Ürün adı
                canvas.drawText(
                    item.model.take(42),
                    leftMargin,
                    y,
                    normalPaint
                )

                // Miktar
                canvas.drawText(
                    "${item.quantity}",
                    leftMargin + 300f,
                    y,
                    normalPaint
                )

                // Fiyat
                val priceText =
                    String.format(
                        Locale.getDefault(),
                        "%.2f %s",
                        item.price,
                        item.currency
                    )

                canvas.drawText(
                    priceText,
                    leftMargin + 360f,
                    y,
                    normalPaint
                )

                y += 17f
            }


            // =====================================================
            // FİYAT BİLGİLERİ
            // =====================================================

            y += 15f

            checkSpace(120f)

            canvas.drawLine(
                leftMargin,
                y,
                pageWidth - rightMargin,
                y,
                normalPaint
            )

            y += 25f

            canvas.drawText(
                "FİYAT BİLGİLERİ",
                leftMargin,
                y,
                sectionPaint
            )

            y += 22f


            // Ara toplam

            val baseTotalText =
                String.format(
                    Locale.getDefault(),
                    "%.2f ₺",
                    order.baseTotalTL
                )

            canvas.drawText(
                "Ara Toplam:",
                leftMargin,
                y,
                boldPaint
            )

            canvas.drawText(
                baseTotalText,
                leftMargin + 390f,
                y,
                normalPaint
            )

            y += 20f


            // İskonto

            val discountAmount =
                order.baseTotalTL -
                        order.discountedTotalTL

            val discountText =
                String.format(
                    Locale.getDefault(),
                    "-%.2f ₺",
                    discountAmount
                )

            canvas.drawText(
                "İskonto (%${order.discountPercent}):",
                leftMargin,
                y,
                boldPaint
            )

            canvas.drawText(
                discountText,
                leftMargin + 390f,
                y,
                normalPaint
            )

            y += 20f


            // KDV

            val kdvText =
                String.format(
                    Locale.getDefault(),
                    "%.2f ₺",
                    order.kdvAmountTL
                )

            canvas.drawText(
                "KDV (%${order.kdvPercent}):",
                leftMargin,
                y,
                boldPaint
            )

            canvas.drawText(
                kdvText,
                leftMargin + 390f,
                y,
                normalPaint
            )

            y += 25f


            // =====================================================
            // GENEL TOPLAM
            // =====================================================

            canvas.drawLine(
                leftMargin,
                y,
                pageWidth - rightMargin,
                y,
                normalPaint
            )

            y += 25f


            val totalText =
                String.format(
                    Locale.getDefault(),
                    "%.2f ₺",
                    order.totalTL
                )

            canvas.drawText(
                "GENEL TOPLAM:",
                leftMargin,
                y,
                greenPaint
            )

            canvas.drawText(
                totalText,
                leftMargin + 390f,
                y,
                greenPaint
            )

            y += 35f


            // =====================================================
            // ALT BİLGİ
            // =====================================================

            checkSpace(30f)

            canvas.drawText(
                "WatermApp - Müşteri Kayıt Sistemi",
                leftMargin,
                pageHeight - 25f,
                smallPaint
            )

            canvas.drawText(
                "Sayfa $pageNumber",
                pageWidth - 100f,
                pageHeight - 25f,
                smallPaint
            )


            // =====================================================
            // SON SAYFAYI TAMAMLA
            // =====================================================

            document.finishPage(page)

            pageCount = pageNumber


            // =====================================================
            // İPTAL KONTROLÜ
            // =====================================================

            if (
                cancellationSignal?.isCanceled == true
            ) {

                document.close()

                callback?.onWriteCancelled()

                return
            }


            // =====================================================
            // PDF'İ YAZ
            // =====================================================

            FileOutputStream(
                destination.fileDescriptor
            ).use { outputStream ->

                document.writeTo(
                    outputStream
                )
            }

            document.close()

            callback?.onWriteFinished(
                arrayOf(
                    PageRange.ALL_PAGES
                )
            )

        } catch (e: Exception) {

            try {
                document.close()
            } catch (_: Exception) {
            }

            callback?.onWriteFailed(
                e.message
                    ?: "PDF oluşturulurken hata oluştu."
            )
        }
    }
}