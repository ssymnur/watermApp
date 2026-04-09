package com.example.watermapp.view.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.watermapp.BaseUrun
import com.example.watermapp.EndustriyelUrun
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectProductFormScreen(
    navController: NavHostController,
    projeAdi: String
) {

    var urunAdi by rememberSaveable { mutableStateOf("") }
    var marka by rememberSaveable { mutableStateOf("") }
    var kod by rememberSaveable { mutableStateOf("") }
    var fiyat by rememberSaveable { mutableStateOf("") }
    var ozellikler by rememberSaveable { mutableStateOf("") }

    var tankBaglanti by rememberSaveable { mutableStateOf("") }
    var girisCikis by rememberSaveable { mutableStateOf("") }
    var debi by rememberSaveable { mutableStateOf("") }

    var paraBirimi by rememberSaveable { mutableStateOf("USD") }
    var expanded by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projeye Ürün Ekle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Basit field fonksiyonu
            fun field(value: String, onChange: (String) -> Unit, label: String) {
                item {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            field(urunAdi, { urunAdi = it }, "Ürün Adı")
            field(marka, { marka = it }, "Marka")
            field(kod, { kod = it }, "Ürün Kodu")
            field(tankBaglanti, { tankBaglanti = it }, "Tank Bağlantı")
            field(girisCikis, { girisCikis = it }, "Giriş / Çıkış")
            field(debi, { debi = it }, "Debi (m³/saat)")

            item {
                OutlinedTextField(
                    value = fiyat,
                    onValueChange = { fiyat = it },
                    label = { Text("Fiyat") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Para birimi dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = paraBirimi,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Para Birimi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("USD", "EUR", "TL").forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    paraBirimi = currency
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            field(ozellikler, { ozellikler = it }, "Özellikler")

            // Kaydet butonu
            item {

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    onClick = {

                        if (urunAdi.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Ürün adı boş olamaz")
                            }
                            return@Button
                        }

                        loading = true

                        // 🔹 BaseUrun yerine concrete class
                        val product: BaseUrun = EndustriyelUrun(
                            anaKategori = "Proje",
                            altKategori = projeAdi,
                            urun_adi = urunAdi.trim(),
                            marka = marka.trim(),
                            model = "",
                            tank_baglanti = tankBaglanti.trim(),
                            giris_cikis_baglanti = girisCikis.trim(),
                            debi_m3_saat = debi.toDoubleOrNull(),
                            urun_kodu = kod.toIntOrNull(),
                            fiyat = fiyat.toDoubleOrNull(),
                            paraBirimi = paraBirimi,
                            ozellikler = ozellikler.trim()
                        )

                        Firebase.firestore
                            .collection("projects")
                            .document(projeAdi)
                            .collection("urunler")
                            .add(product)
                            .addOnSuccessListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Ürün projeye eklendi ✅")
                                    delay(800)
                                    navController.popBackStack()
                                }
                            }
                            .addOnFailureListener { e ->
                                loading = false
                                e.printStackTrace()

                                scope.launch {
                                    snackbarHostState.showSnackbar(e.message ?: "Bilinmeyen hata")
                                }
                            }
                    }
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}