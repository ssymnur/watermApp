package com.example.watermapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.watermapp.model.Order
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.Locale


@Composable
fun EditOrderScreen(orderId: String, navController: NavHostController) {

    var order by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(orderId) {
        Firebase.firestore.collection("orders")
            .document(orderId)
            .get()
            .addOnSuccessListener {
                order = it.toObject(Order::class.java)
            }
    }

    val currentOrder = order ?: return Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    var name by remember { mutableStateOf(currentOrder.customer.customerName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {

            Text("İskonto: %${currentOrder.discountPercent}")

            Spacer(Modifier.height(8.dp))

            Text(
                "Ara Toplam: %.2f ₺".format(
                    Locale.getDefault(),
                    currentOrder.baseTotalTL
                )
            )

            val discountAmount =
                currentOrder.baseTotalTL - currentOrder.discountedTotalTL

            Text(
                "İskonto (%${currentOrder.discountPercent}) : -%.2f ₺"
                    .format(Locale.getDefault(), discountAmount),
                color = Color(0xFF2E7D32)
            )

            Text(
                "KDV (%${currentOrder.kdvPercent}) : %.2f ₺"
                    .format(Locale.getDefault(), currentOrder.kdvAmountTL),
                color = Color.Blue
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Genel Toplam: %.2f ₺"
                    .format(Locale.getDefault(), currentOrder.totalTL),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2E7D32)
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Müşteri Adı") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    val updated = currentOrder.copy(
                        customer = currentOrder.customer.copy(
                            customerName = name
                        )
                    )

                    Firebase.firestore
                        .collection("orders")
                        .document(orderId)
                        .set(updated)

                    navController.popBackStack()
                }
            ) {
                Text("Kaydet")
            }
        }

    }
}