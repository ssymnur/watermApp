package com.example.watermapp.view.projects

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.BaseUrun
import com.example.watermapp.EndustriyelUrun
import com.example.watermapp.EvselUrun
import com.example.watermapp.KimyasalUrun
import com.example.watermapp.PeyzajUrun
import com.example.watermapp.data.CartManager
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjeDetayScreen(
    projeAdi: String,
    navController: NavHostController
) {

    val db = FirebaseFirestore.getInstance()
    var urunler by remember { mutableStateOf<List<BaseUrun>>(emptyList()) }
    var silinecekUrun by remember { mutableStateOf<BaseUrun?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val isStaff = CartManager.SessionManager.isStaff

    val toplamTL = urunler.filter { it.paraBirimi == "TL" }.sumOf { it.fiyat ?: 0.0 }
    val toplamUSD = urunler.filter { it.paraBirimi == "USD" }.sumOf { it.fiyat ?: 0.0 }
    val toplamEUR = urunler.filter { it.paraBirimi == "EUR" }.sumOf { it.fiyat ?: 0.0 }

    // 🔹 Firestore Listener
    DisposableEffect(projeAdi) {
        val listener = db
            .collection("projects")
            .document(projeAdi)
            .collection("urunler")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                urunler = snapshot?.documents?.mapNotNull { doc ->

                    val item = doc.toObject(EndustriyelUrun::class.java)

                    if (item?.urun_adi.isNullOrBlank()) {
                        null // 🔥 boş ürünleri tamamen yok say
                    } else {
                        item.copy(id = doc.id)
                    }
                } ?: emptyList()

                isLoading = false
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(projeAdi, maxLines = 1) // proje adı tek satırda
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TL: $toplamTL TL", style = MaterialTheme.typography.bodySmall)
                            Text("USD: $toplamUSD USD", style = MaterialTheme.typography.bodySmall)
                            Text("EUR: $toplamEUR EUR", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (isStaff) {
                        IconButton(
                            onClick = {
                                navController.navigate(
                                    "add_project_product_form/${Uri.encode(projeAdi)}"
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, "Ürün Ekle")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            urunler.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bu projede henüz ürün yok")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(items = urunler, key = { it.id }) { urun ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd && isStaff) {
                                silinecekUrun = urun
                                dismissState.reset()
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = isStaff,
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
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(urun.urun_adi ?: "-", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    when (urun) {
                                        is EvselUrun -> {
                                            Text("Pompa: ${urun.pompa ?: "-"}")
                                            Text("Aşama: ${urun.asama ?: "-"}")
                                            Text("Membran: ${urun.membran_gpd ?: "-"} GPD")
                                            Text("Tank: ${urun.tank_galon ?: "-"} Galon")
                                        }
                                        is EndustriyelUrun -> {
                                            Text("Model: ${urun.model ?: "-"}")
                                            Text("Debi: ${urun.debi_m3_saat ?: "-"} m³/saat")
                                            Text("Tank Bağlantı: ${urun.tank_baglanti ?: "-"}")
                                            Text("Giriş/Çıkış: ${urun.giris_cikis_baglanti ?: "-"}")
                                        }
                                        is PeyzajUrun -> {
                                            Text("Nozul Rengi: ${urun.nozul_rengi ?: "-"}")
                                            Text("Atış Mesafesi: ${urun.atis_mesafesi ?: "-"}")
                                        }
                                        is KimyasalUrun -> {
                                            Text("Ambalaj/KG: ${urun.ambalaj_kg ?: "-"}")
                                            Text("Kullanım Amacı: ${urun.kullanim_amaci ?: "-"}")
                                        }
                                    }
                                    Text("Marka: ${urun.marka ?: "-"}")
                                    Text("Kod: ${urun.urun_kodu ?: "-"}")
                                    Text("Fiyat: ${urun.fiyat ?: "-"} ${urun.paraBirimi}")
                                    Text("Özellikler: ${urun.ozellikler ?: "-"}")

                                    if (isStaff) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = {
                                                navController.navigate(
                                                    "edit_project_product/${urun.id}/${Uri.encode(projeAdi)}"
                                                )
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) { Text("Düzenle") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 Silme Dialogu
        silinecekUrun?.let { urun ->
            AlertDialog(
                onDismissRequest = { silinecekUrun = null },
                title = { Text("Ürünü Sil") },
                text = { Text("Bu ürün silinecek. Emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            db.collection("projects")
                                .document(projeAdi)
                                .collection("urunler")
                                .document(urun.id)
                                .delete()
                            silinecekUrun = null
                        }
                    ) { Text("Evet", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { silinecekUrun = null }) { Text("Vazgeç") }
                }
            )
        }
    }
}