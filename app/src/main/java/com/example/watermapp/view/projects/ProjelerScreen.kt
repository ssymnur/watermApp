package com.example.watermapp.view.projects

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.BaseUrun
import com.example.watermapp.EndustriyelUrun
import com.example.watermapp.EvselUrun
import com.example.watermapp.PeyzajUrun
import com.example.watermapp.data.CartManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjelerScreen(
    navController: NavHostController,
    kategori: String
) {

    val db = FirebaseFirestore.getInstance()
    var projeler by remember { mutableStateOf<List<String>>(emptyList()) }
    var yeniProjeAdi by remember { mutableStateOf("") }
    var silinecekProje by remember { mutableStateOf<String?>(null) }

    // Firestore listener
    DisposableEffect(Unit) {
        val listener = db.collection("projects")
            .addSnapshotListener { snapshot, _ ->
                projeler = snapshot?.documents?.mapNotNull { it.id } ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projeler") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(projeler) { projeAdi ->

                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd &&
                            CartManager.SessionManager.isStaff
                        ) {
                            silinecekProje = projeAdi
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = CartManager.SessionManager.isStaff,
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = projeAdi,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate("proje/${Uri.encode(projeAdi)}")
                                        }
                                )

                                // Sepete ekleme butonu
                                // Sepete ekleme kısmı düzeltilmiş
                                IconButton(onClick = {
                                    // Projeyi sepete ekle (header)
                                    CartManager.cart.add(
                                        CartManager.CartItem(
                                            urun = null,
                                            quantity = mutableStateOf(1),
                                            isProjectHeader = true,
                                            projectName = projeAdi
                                        )
                                    )

                                    // Firestore'dan ürünleri çek
                                    db.collection("projects")
                                        .document(projeAdi)
                                        .collection("urunler")
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            snapshot.documents.forEach { doc ->
                                                val urun = when(doc.getString("anaKategori")) {
                                                    "Evsel" -> doc.toObject(EvselUrun::class.java)?.copy(id = doc.id)
                                                    "Endüstriyel" -> doc.toObject(EndustriyelUrun::class.java)?.copy(id = doc.id)
                                                    "Peyzaj" -> doc.toObject(PeyzajUrun::class.java)?.copy(id = doc.id)
                                                    else -> null
                                                }

                                                urun?.let {
                                                    CartManager.cart.add(
                                                        CartManager.CartItem(
                                                            urun = it,
                                                            quantity = mutableStateOf(1),
                                                            isProjectHeader = false,
                                                            projectName = projeAdi
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                }) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Sepete Ekle")
                                }
                            }
                        }
                    }
                }
            }

            // Yeni proje oluşturma bölümü (sadece admin)
            if (CartManager.SessionManager.isStaff) {
                Divider()
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = yeniProjeAdi,
                        onValueChange = { yeniProjeAdi = it },
                        label = { Text("Yeni Proje Adı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (yeniProjeAdi.isNotBlank()) {
                                db.collection("projects")
                                    .document(yeniProjeAdi.trim())
                                    .set(mapOf("createdAt" to System.currentTimeMillis()))
                                yeniProjeAdi = ""
                            }
                        }
                    ) {
                        Text("Proje Oluştur")
                    }
                }
            }
        }

        // Silme onayı
        silinecekProje?.let { projeAdi ->
            AlertDialog(
                onDismissRequest = { silinecekProje = null },
                title = { Text("Projeyi Sil") },
                text = { Text("Bu proje ve içindeki ürünler silinecek.") },
                confirmButton = {
                    TextButton(onClick = {
                        val projeRef = db.collection("projects").document(projeAdi)
                        projeRef.collection("urunler").get().addOnSuccessListener { snapshot ->
                            val batch = db.batch()
                            snapshot.documents.forEach { batch.delete(it.reference) }
                            batch.delete(projeRef)
                            batch.commit().addOnSuccessListener { silinecekProje = null }
                        }
                    }) { Text("Evet", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { silinecekProje = null }) { Text("Vazgeç") }
                }
            )
        }
    }
}