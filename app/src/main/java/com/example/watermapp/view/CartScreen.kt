package com.example.watermapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.data.CartManager
import com.example.watermapp.printView
import kotlin.collections.component1
import kotlin.collections.component2
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.LaunchedEffect


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavHostController) {
    var showSummary by remember { mutableStateOf(false) }

    val expandedProjects = remember { mutableStateMapOf<String, Boolean>() }
    var projectToDelete by remember { mutableStateOf<String?>(null) }

    val cart = CartManager.cart

    val projectItems = cart.filter {
        !it.projectName.isNullOrBlank() &&
                !it.urun?.urun_adi.isNullOrBlank()
    }
    val otherItems = cart.filter { it.projectName == null }
    val groupedProjects = projectItems.groupBy { it.projectInstanceId }

    val totals by remember(
        CartManager.cart,
        CartManager.appliedDiscountPercent,
        CartManager.kdvEnabled,
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        derivedStateOf { CartManager.totalsWithDiscount() }
    }

    val baseTotals by remember(
        CartManager.cart,
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        derivedStateOf { CartManager.totalsByCurrency() }
    }

    val context = LocalContext.current
    val view = LocalView.current

    var usdText by remember { mutableStateOf(CartManager.usdRate.toString()) }
    var eurText by remember { mutableStateOf(CartManager.eurRate.toString()) }

    //swipe
    var showDeleteDialog by remember { mutableStateOf(false) }

    var discountText by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<CartManager.CartItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sepet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {

                    IconButton(onClick = {
                        printView(context, view)
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Yazdır")
                    }

                    if (CartManager.SessionManager.isStaff) {
                        IconButton(onClick = {
                            navController.navigate("customerForm") {
                                launchSingleTop = true
                            }
                        }) {
                            Icon(Icons.Default.Person, contentDescription = "Müşteri Formu")
                        }
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
            Text("Güncel Kur Gir", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = usdText,
                onValueChange = {
                    usdText = it
                    CartManager.usdRate = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("USD → TL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = eurText,
                onValueChange = {
                    eurText = it
                    CartManager.eurRate = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("EUR → TL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // ✅ SEPET LİSTESİ
            LazyColumn(modifier = Modifier.weight(1f)) {

                // 📦 PROJELER
                items(groupedProjects.toList()) { (instanceId, items) ->

                    val projectName = items.first().projectName ?: "Proje"

                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {

                            // 🔥 SADECE STATE SET ET
                            projectToDelete = instanceId
                            showDeleteDialog = true
                            itemToDelete = null

                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(24.dp))
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                            }
                        }
                    ) {

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = "📦 $projectName",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            expandedProjects[instanceId ?: ""] =
                                                !(expandedProjects[instanceId ?: ""] ?: false)
                                        }
                                    ) {
                                        Icon(
                                            imageVector =
                                                if (expandedProjects[instanceId ?: ""] == true)
                                                    Icons.Default.KeyboardArrowUp
                                                else
                                                    Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand"
                                        )
                                    }
                                }

                                if (expandedProjects[instanceId ?: ""] == true) {
                                    items.forEach { item ->

                                        if (item.urun?.urun_adi.isNullOrBlank()) return@forEach
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            // ✅ ÜRÜN ADI EKLE
                                            Text(
                                                text = item.urun?.urun_adi ?: "",
                                                modifier = Modifier.weight(1f)
                                            )

                                            // ✅ ADET KONTROL
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                IconButton(
                                                    onClick = {
                                                        if (item.quantity.value > 1) {
                                                            item.quantity.value--
                                                        } else {
                                                            CartManager.cart.remove(item)
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Azalt")
                                                }

                                                Text(
                                                    text = item.quantity.value.toString(),
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )

                                                IconButton(
                                                    onClick = { item.quantity.value++ }
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Arttır")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 🛒 DİĞER ÜRÜNLER (TEK TEK)
                items(otherItems) { item ->

                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                            itemToDelete = item
                            showDeleteDialog = true
                            projectToDelete = null
                            dismissState.reset() // 🔥 EN KRİTİK SATIR
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(24.dp))
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // ✅ ÜRÜN ADI GERİ GELDİ
                                Text(
                                    text = item.urun?.urun_adi ?: "",
                                    modifier = Modifier.weight(1f)
                                )

                                // ✅ ADET KONTROL
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    IconButton(
                                        onClick = {
                                            if (item.quantity.value > 1) {
                                                item.quantity.value--
                                            } else {
                                                CartManager.cart.remove(item)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Azalt")
                                    }

                                    Text(
                                        text = item.quantity.value.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = { item.quantity.value++ }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Arttır")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        itemToDelete = null
                        projectToDelete = null
                    },
                    title = {
                        Text(
                            if (itemToDelete != null) "Ürünü Sil"
                            else "Projeyi Sil"
                        )
                    },
                    text = {
                        Text(
                            if (itemToDelete != null)
                                "Bu ürünü sepetten silmek istiyor musunuz?"
                            else
                                "Bu projeyi ve içindeki tüm ürünleri silmek istiyor musunuz?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {

                            // 🔥 ÜRÜN SİL
                            if (itemToDelete != null) {
                                CartManager.cart.remove(itemToDelete!!)
                            }

                            // 🔥 PROJE SİL (EN ÖNEMLİ KISIM)
                            if (projectToDelete != null) {
                                CartManager.cart.removeAll {
                                    it.projectInstanceId == projectToDelete
                                }
                            }

                            itemToDelete = null
                            projectToDelete = null
                            showDeleteDialog = false

                        }) {
                            Text("Evet")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            itemToDelete = null
                            projectToDelete = null
                            showDeleteDialog = false
                        }) {
                            Text("Vazgeç")
                        }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showSummary = !showSummary },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showSummary) "Detayları Gizle ▲" else "Detayları Göster ▼")
            }

            if (showSummary) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { discountText = it },
                                label = { Text("% İskonto") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = CartManager.kdvEnabled,
                                    onCheckedChange = { CartManager.kdvEnabled = it }
                                )
                                Text("KDV")
                            }

                            Button(
                                onClick = {
                                    val percent = discountText.toDoubleOrNull() ?: 0.0
                                    CartManager.applyDiscount(percent)
                                }
                            ) {
                                Text("Uygula")
                            }
                        }

                        // 🔥 TOPLAMLAR BURADA
                        val totals = CartManager.totalsByCurrency()

                        totals.forEach { (currency, baseTotal) ->

                            val tlValue = when (currency) {
                                "USD" -> baseTotal * CartManager.usdRate
                                "EUR" -> baseTotal * CartManager.eurRate
                                "TL" -> baseTotal
                                else -> baseTotal
                            }

                            val afterDiscount =
                                tlValue * (1 - CartManager.appliedDiscountPercent / 100)

                            val finalTL =
                                if (CartManager.kdvEnabled)
                                    afterDiscount * (1 + CartManager.kdvRate)
                                else
                                    afterDiscount

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {

                                // 🔹 ÜSTTE GRİ TL (iskontolu + KDV'li)
                                Text(
                                    text = "≈ %.2f ₺".format(finalTL),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )

                                // 🔹 ALTTA DÖVİZ (ASLINDAKİ FİYAT)
                                Text(
                                    text = when (currency) {
                                        "USD" -> "%.2f $".format(baseTotal)
                                        "EUR" -> "%.2f €".format(baseTotal)
                                        "TL" -> "%.2f ₺".format(baseTotal)
                                        else -> "$baseTotal"
                                    },
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val totalTL = CartManager.totalInTL()

                        Text(
                            text = "Genel Toplam: %.2f ₺".format(totalTL),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            /*
            Spacer(Modifier.height(12.dp))

            // ✅ TOPLAMLAR
            val totals = CartManager.totalsByCurrency()

            totals.forEach { (currency, baseTotal) ->

                // TL karşılığı
                val tlValue = when (currency) {
                    "USD" -> baseTotal * CartManager.usdRate
                    "EUR" -> baseTotal * CartManager.eurRate
                    "TL" -> baseTotal
                    else -> baseTotal
                }

                // TL'ye iskonto uygula
                val afterDiscount =
                    tlValue * (1 - CartManager.appliedDiscountPercent / 100)

                // TL'ye KDV uygula
                val finalTL =
                    if (CartManager.kdvEnabled)
                        afterDiscount * (1 + CartManager.kdvRate)
                    else
                        afterDiscount

                Column {

                    // 🔹 ÜSTTE GRİ TL (iskontolu + KDV'li)
                    Text(
                        text = "≈ %.2f ₺".format(finalTL),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // 🔹 ALTTA DÖVİZ (DEĞİŞMEZ)
                    Text(
                        text = when (currency) {
                            "USD" -> "%.2f $".format(baseTotal)
                            "EUR" -> "%.2f €".format(baseTotal)
                            "TL" -> "%.2f ₺".format(baseTotal)
                            else -> "$baseTotal"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

// ✅ GENEL TOPLAM TL
            val totalTL = CartManager.totalInTL()

            Text(
                text = "Genel Toplam (TL): %.2f ₺".format(totalTL),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2E7D32)
            )*/


        }
    }
}