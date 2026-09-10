package com.seymanur.watermapp.view.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.seymanur.watermapp.data.CartManager
import com.seymanur.watermapp.formatDate
import com.seymanur.watermapp.model.Order
import com.seymanur.watermapp.model.OrderDoc
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import com.seymanur.watermapp.printOrder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavHostController) {

    // =========================================================
    // PERSONEL KONTROLÜ
    // =========================================================
    val context = LocalContext.current
    val isStaff = CartManager.SessionManager.isStaff

    if (!isStaff) {

        LaunchedEffect(Unit) {
            navController.popBackStack()
        }

        return
    }

    // =========================================================
    // STATE'LER
    // =========================================================

    var search by remember {
        mutableStateOf("")
    }

    var orders by remember {
        mutableStateOf<List<OrderDoc>>(emptyList())
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var orderToDelete by remember {
        mutableStateOf<OrderDoc?>(null)
    }

    // Aynı anda sadece bir müşteri açık olacak
    var expandedOrderId by remember {
        mutableStateOf<String?>(null)
    }

    // =========================================================
    // FIREBASE SİPARİŞLERİ DİNLE
    // =========================================================

    DisposableEffect(Unit) {

        val listener = Firebase.firestore
            .collection("orders")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Log.e(
                        "FIREBASE_ERROR",
                        error.message ?: ""
                    )

                    return@addSnapshotListener
                }

                orders = snapshot?.documents
                    ?.mapNotNull { doc ->

                        val order =
                            doc.toObject(Order::class.java)

                        order?.let {

                            OrderDoc(
                                id = doc.id,
                                order = it
                            )
                        }

                    } ?: emptyList()
            }

        onDispose {
            listener.remove()
        }
    }

    // =========================================================
    // ARAMA
    // =========================================================

    val filtered = orders.filter {

        it.order.customer.customerName
            .contains(
                search,
                ignoreCase = true
            ) ||

                it.order.customer.companyName
                    .contains(
                        search,
                        ignoreCase = true
                    )
    }

    // =========================================================
    // SİLME DİYALOĞU
    // =========================================================

    if (
        showDeleteDialog &&
        orderToDelete != null
    ) {

        AlertDialog(

            onDismissRequest = {

                showDeleteDialog = false
                orderToDelete = null
            },

            title = {
                Text("Silme Onayı")
            },

            text = {
                Text(
                    "Bu müşteri kaydını silmek istiyor musun?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        Firebase.firestore
                            .collection("orders")
                            .document(
                                orderToDelete!!.id
                            )
                            .delete()

                        // Silinen müşteri açıksa kapat
                        if (
                            expandedOrderId ==
                            orderToDelete!!.id
                        ) {

                            expandedOrderId = null
                        }

                        showDeleteDialog = false
                        orderToDelete = null
                    }
                ) {

                    Text("Evet")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showDeleteDialog = false
                        orderToDelete = null
                    }
                ) {

                    Text("İptal")
                }
            }
        )
    }

    // =========================================================
    // ANA EKRAN
    // =========================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Müşteri Kayıtları")
                },

                navigationIcon = {

                    IconButton(

                        onClick = {
                            navController.popBackStack()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription =
                                "Geri"
                        )
                    }
                }
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // =================================================
            // ARAMA ALANI
            // =================================================

            OutlinedTextField(

                value = search,

                onValueChange = {
                    search = it
                },

                label = {
                    Text("Müşteri / Şirket Ara")
                },

                modifier = Modifier
                    .fillMaxWidth(),

                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =================================================
            // MÜŞTERİ LİSTESİ
            // =================================================

            LazyColumn(

                modifier = Modifier.fillMaxSize(),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(

                    items = filtered,

                    key = {
                        it.id
                    }

                ) { orderDoc ->

                    val order =
                        orderDoc.order

                    // Bu müşteri açık mı?
                    val isExpanded =
                        expandedOrderId ==
                                orderDoc.id

                    // =================================================
                    // MÜŞTERİ CARD
                    // =================================================

                    Card(

                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                expandedOrderId =
                                    if (isExpanded) {

                                        null

                                    } else {

                                        orderDoc.id
                                    }
                            }
                    ) {

                        Column(

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            // =================================================
                            // ÜST KISIM
                            // =================================================

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Column(

                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    // MÜŞTERİ ADI
                                    Text(

                                        text =
                                            order.customer.customerName
                                                .ifBlank {
                                                    "İsimsiz Müşteri"
                                                },

                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium
                                    )

                                    // ŞİRKET ADI
                                    if (
                                        order.customer.companyName
                                            .isNotBlank()
                                    ) {

                                        Text(

                                            text =
                                                order.customer.companyName,

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall,

                                            color =
                                                Color.Gray
                                        )
                                    }
                                }

                                // AÇ / KAPAT OKU
                                Icon(

                                    imageVector =
                                        if (isExpanded)

                                            Icons.Default
                                                .KeyboardArrowUp

                                        else

                                            Icons.Default
                                                .KeyboardArrowDown,

                                    contentDescription =
                                        if (isExpanded)

                                            "Kapat"

                                        else

                                            "Detayları göster"
                                )
                            }

                            // =================================================
                            // DETAYLAR
                            // =================================================

                            if (isExpanded) {

                                Spacer(
                                    modifier =
                                        Modifier.height(16.dp)
                                )

                                // =================================================
                                // MÜŞTERİ BİLGİLERİ
                                // =================================================

                                Text(

                                    text =
                                        "MÜŞTERİ BİLGİLERİ",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleSmall
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Text(
                                    "Müşteri: ${order.customer.customerName}"
                                )

                                Text(
                                    "Şirket: ${order.customer.companyName}"
                                )

                                Text(
                                    "Telefon: ${order.customer.phone}"
                                )

                                Text(
                                    "Cihaz: ${order.customer.deviceName}"
                                )

                                Text(
                                    "Mail: ${order.customer.email}"
                                )

                                Text(
                                    "Personel: ${order.customer.staffName}"
                                )

                                Text(
                                    "Adres: ${order.customer.address}"
                                )

                                Text(
                                    "Not: ${order.customer.note}"
                                )

                                Text(
                                    "Tarih: ${formatDate(order.timestamp)}"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(16.dp)
                                )

                                // =================================================
                                // ÜRÜNLER
                                // =================================================

                                Text(

                                    text =
                                        "ÜRÜNLER",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleSmall
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                if (order.items.isEmpty()) {

                                    Text(
                                        "Bu siparişte ürün bulunmuyor.",
                                        color = Color.Gray
                                    )

                                } else {

                                    order.items.forEach { item ->

                                        Text(
                                            "• ${item.model}  •  ${item.quantity} adet"
                                        )

                                        Text(
                                            "  Birim fiyat: ${item.price} ${item.currency}",
                                            color = Color.Gray
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(4.dp)
                                        )
                                    }
                                }

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                // =================================================
                                // FİYAT BİLGİLERİ
                                // =================================================

                                Text(

                                    text =
                                        "FİYAT BİLGİLERİ",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleSmall
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                // ARA TOPLAM
                                Text(
                                    text =
                                        "Ara Toplam: ${
                                            String.format(
                                                Locale.getDefault(),
                                                "%.2f",
                                                order.baseTotalTL
                                            )
                                        } ₺",

                                    color =
                                        Color.Gray
                                )

                                // İSKONTO HESABI
                                val discountAmount =
                                    order.baseTotalTL -
                                            order.discountedTotalTL

                                // İSKONTO
                                Text(
                                    text =
                                        "İskonto (%${order.discountPercent}): -${
                                            String.format(
                                                Locale.getDefault(),
                                                "%.2f",
                                                discountAmount
                                            )
                                        } ₺",

                                    color =
                                        Color.Gray
                                )

                                // KDV
                                Text(
                                    text =
                                        "KDV (%${order.kdvPercent}): ${
                                            String.format(
                                                Locale.getDefault(),
                                                "%.2f",
                                                order.kdvAmountTL
                                            )
                                        } ₺",

                                    color =
                                        Color.Blue
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                // GENEL TOPLAM
                                Text(
                                    text =
                                        "Genel Toplam: ${
                                            String.format(
                                                Locale.getDefault(),
                                                "%.2f",
                                                order.totalTL
                                            )
                                        } ₺",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,

                                    color =
                                        Color(0xFF2E7D32)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(16.dp)
                                )

                                // =================================================
                                // BUTONLAR
                                // =================================================

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    // =================================================
                                    // YAZDIR
                                    // =================================================

                                    Button(

                                        modifier =
                                            Modifier.weight(1f),

                                        onClick = {

                                            printOrder(
                                                context = context,
                                                order = order
                                            )

                                        }
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Print,

                                            contentDescription =
                                                "Yazdır"
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.padding(
                                                    horizontal = 3.dp
                                                )
                                        )

                                        Text("Yazdır")
                                    }

                                    // =================================================
                                    // SİL
                                    // =================================================

                                    Button(

                                        modifier =
                                            Modifier.weight(1f),

                                        onClick = {

                                            orderToDelete =
                                                orderDoc

                                            showDeleteDialog =
                                                true
                                        }
                                    ) {

                                        Icon(

                                            imageVector =
                                                Icons.Default.Delete,

                                            contentDescription =
                                                "Sil"
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.padding(
                                                    horizontal = 3.dp
                                                )
                                        )

                                        Text("Sil")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}