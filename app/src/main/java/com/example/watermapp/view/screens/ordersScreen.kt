package com.example.watermapp.view.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.data.CartManager
import com.example.watermapp.formatDate
import com.example.watermapp.model.Order
import com.example.watermapp.model.OrderDoc
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavHostController) {

    val isStaff = CartManager.SessionManager.isStaff

    if (!isStaff) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    var search by remember { mutableStateOf("") }
    var orders by remember { mutableStateOf<List<OrderDoc>>(emptyList()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var orderToDelete by remember { mutableStateOf<OrderDoc?>(null) }

    DisposableEffect(Unit) {

        val listener = Firebase.firestore
            .collection("orders")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e("FIREBASE_ERROR", error.message ?: "")
                    return@addSnapshotListener
                }

                orders = snapshot?.documents?.mapNotNull { doc ->

                    val order = doc.toObject(Order::class.java)

                    order?.let {
                        OrderDoc(
                            id = doc.id,
                            order = it
                        )
                    }

                } ?: emptyList()
            }

        onDispose { listener.remove() }
    }

    val filtered = orders.filter {
        it.order.customer.customerName.contains(search, true) ||
                it.order.customer.companyName.contains(search, true)
    }

    // DELETE DIALOG
    if (showDeleteDialog && orderToDelete != null) {

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Silme Onayı") },
            text = { Text("Siparişi silmek istiyor musun?") },
            confirmButton = {

                TextButton(
                    onClick = {

                        Firebase.firestore
                            .collection("orders")
                            .document(orderToDelete!!.id)
                            .delete()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siparişler") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Müşteri / Şirket Ara") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(filtered, key = { it.id }) { orderDoc ->

                    val order = orderDoc.order

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text("Müşteri: ${order.customer.customerName}")
                            Text("Şirket: ${order.customer.companyName}")
                            Text("Telefon: ${order.customer.phone}")
                            Text("Personel: ${order.customer.staffName}")
                            Text("Tarih: ${formatDate(order.timestamp)}")

                            Spacer(Modifier.height(8.dp))

                            // ÜRÜN LİSTESİ
                            order.items.forEach { item ->

                                /*val tlPrice = when (item.currency) {
                                    "USD" -> item.price * order.usdRate
                                    "EUR" -> item.price * order.eurRate
                                    else -> item.price
                                }*/
                                val tlPrice = item.price

                                val totalItemTL = tlPrice * item.quantity

                                Text("${item.model}  •  ${item.quantity} adet")

                                Spacer(Modifier.height(6.dp))
                            }

                            Spacer(Modifier.height(12.dp))

                            // FİYAT ÖZETİ
                            Text(
                                "Ara Toplam: %.2f ₺".format(order.baseTotalTL),
                                color = Color.Gray
                            )

                            val discountAmount =
                                order.baseTotalTL - order.discountedTotalTL

                            Text(
                                text = "İskonto (%${order.discountPercent}) : -${"%.2f".format(discountAmount)} ₺",
                                color = Color.Gray
                            )

                            Text(
                                text = "KDV (%${order.kdvPercent}) : ${"%.2f".format(order.kdvAmountTL)} ₺",
                                color = Color.Blue
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Genel Toplam : %.2f ₺"
                                    .format(order.totalTL),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2E7D32)
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    orderToDelete = orderDoc
                                    showDeleteDialog = true
                                }
                            ) {
                                Text("Sil")
                            }
                        }
                    }
                }
            }
        }
    }
}