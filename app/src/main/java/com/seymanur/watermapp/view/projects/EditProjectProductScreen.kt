package com.seymanur.watermapp.view.projects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.seymanur.watermapp.FormField
import com.seymanur.watermapp.utils.getFirestoreValueAsString
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectProductScreen(
    urunId: String,
    projeAdi: String,
    navController: NavHostController
) {

    val db = FirebaseFirestore.getInstance()

    var urunAdi by remember { mutableStateOf("") }
    var urunKodu by remember { mutableStateOf("") }
    var fiyat by remember { mutableStateOf("") }
    var marka by remember { mutableStateOf("") }
    var paraBirimi by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var tankBaglanti by remember { mutableStateOf("") }
    var girisCikis by remember { mutableStateOf("") }
    var debi by remember { mutableStateOf("") }
    var ozellikler by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }

    // 🔥 Firestore verisini çek
    LaunchedEffect(urunId) {

        db.collection("projects")
            .document(projeAdi)
            .collection("urunler")
            .document(urunId)
            .get()
            .addOnSuccessListener { doc ->

                urunAdi = doc.getString("urun_adi") ?: ""
                urunKodu = getFirestoreValueAsString(doc, "urun_kodu")

                fiyat = getFirestoreValueAsString(doc, "fiyat")

                marka = doc.getString("marka") ?: ""
                paraBirimi = doc.getString("paraBirimi") ?: ""
                debi =
                    getFirestoreValueAsString(doc, "debi_m3_saat")

                model = doc.getString("model") ?: ""
                tankBaglanti = doc.getString("tank_baglanti") ?: ""
                girisCikis = doc.getString("giris_cikis_baglanti") ?: ""
                ozellikler = doc.getString("ozellikler") ?: ""

                isLoading = false
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürünü Düzenle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
            ) {

                FormField("Ürün Adı", urunAdi) { urunAdi = it }
                FormField("Ürün Kodu", urunKodu) { urunKodu = it }
                FormField("Fiyat", fiyat) { fiyat = it }
                FormField("Marka", marka) { marka = it }
                FormField("Para Birimi", paraBirimi) { paraBirimi = it }
                FormField("Model", model) { model = it }
                FormField("Tank Bağlantı", tankBaglanti) { tankBaglanti = it }
                FormField("Giriş/Çıkış", girisCikis) { girisCikis = it }
                FormField("Debi (m3/saat)", debi) { debi = it }
                FormField("Özellikler", ozellikler) { ozellikler = it }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {

                        val updatedData = mapOf(
                            "urun_adi" to urunAdi,
                            "urun_kodu" to urunKodu,
                            "fiyat" to fiyat.toDoubleOrNull(),
                            "marka" to marka,
                            "paraBirimi" to paraBirimi,
                            "model" to model,
                            "tank_baglanti" to tankBaglanti,
                            "giris_cikis_baglanti" to girisCikis,
                            "debi_m3_saat" to debi.toDoubleOrNull(),
                            "ozellikler" to ozellikler
                        )

                        db.collection("projects")
                            .document(projeAdi)
                            .collection("urunler")
                            .document(urunId)
                            .update(updatedData)
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kaydet")
                }
            }
        }
    }
}