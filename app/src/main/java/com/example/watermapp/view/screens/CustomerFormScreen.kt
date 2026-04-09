package com.example.watermapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.OrderCalculator
import com.example.watermapp.data.CartManager
import com.example.watermapp.model.Customer
import com.example.watermapp.model.OrderItem
import com.example.watermapp.saveOrderToFirebase
import com.example.watermapp.viewmodel.OrderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    navController: NavHostController,
    viewModel: OrderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var customerName by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var deviceName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var staffName by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }



    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val totalTL = CartManager.totalInTL()

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Müşteri Kaydı") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {

                        val customer = Customer(
                            customerName = customerName,
                            companyName = companyName,
                            phone = phone,
                            deviceName = deviceName,
                            email = email,
                            staffName = staffName,
                            note = note,
                            address = address
                        )

                        val items = CartManager.cart.map {
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
                            discountPercent = CartManager.appliedDiscountPercent,
                            kdvPercent = 20.0,
                            usdRate = CartManager.usdRate,
                            eurRate = CartManager.eurRate
                        )

                        saveOrderToFirebase(order)

                        scope.launch {
                            snackbarHostState.showSnackbar("Sipariş kaydedildi ✅")
                            delay(1200)

                            CartManager.cart.clear()
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            }
        },

        snackbarHost = { SnackbarHost(snackbarHostState) }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Müşteri Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Toplam Tutar: %.2f ₺".format(totalTL),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Şirket Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Cihaz Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Mail Adresi") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = { staffName = it },
                    label = { Text("Personel Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adres") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

        }
    }
}