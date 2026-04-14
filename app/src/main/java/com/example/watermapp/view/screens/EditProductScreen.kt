package com.example.watermapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.BaseUrun
import com.example.watermapp.EndustriyelUrun
import com.example.watermapp.EvselUrun
import com.example.watermapp.KimyasalUrun
import com.example.watermapp.PeyzajUrun
import com.example.watermapp.ProductRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    urun: BaseUrun,
    productId: String,
    navController: NavHostController
) {

    // ---------------- ORTAK STATE ----------------

    var urunAdi by rememberSaveable { mutableStateOf(urun.urun_adi) }
    var marka by rememberSaveable { mutableStateOf(urun.marka ?: "") }
    var kod by rememberSaveable {
        mutableStateOf(
            when (urun) {
                is EvselUrun -> urun.urun_kodu?.toString() ?: ""
                is EndustriyelUrun -> urun.urun_kodu?.toString() ?: ""
                is PeyzajUrun -> urun.urun_kodu?.toString() ?: ""
                else -> ""
            }
        )
    }
    var fiyat by rememberSaveable { mutableStateOf(urun.fiyat?.toString() ?: "") }
    var paraBirimi by rememberSaveable { mutableStateOf(urun.paraBirimi) }

    val currencyOptions = listOf("USD", "TL", "EUR")
    var expandedCurrency by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ---------------- KATEGORİYE ÖZEL STATE ----------------

    var model by rememberSaveable { mutableStateOf((urun as? EndustriyelUrun)?.model ?: "") }
    var debi by rememberSaveable { mutableStateOf((urun as? EndustriyelUrun)?.debi_m3_saat?.toString() ?: "") }

    var pompa by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.pompa ?: "") }
    var asama by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.asama ?: "") }
    var membran by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.membran_gpd?.toString() ?: "") }
    var tank by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.tank_galon?.toString() ?: "") }

    var nozul by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.nozul_rengi ?: "") }
    var atis by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.atis_mesafesi ?: "") }
    var basinc by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.basinc_degeri ?: "") }
    var ebat by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.ebat ?: "") }
    var istasyon by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.istasyon_sayisi?.toString() ?: "") }
    var ambalaj by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.ambalaj_adedi?.toString() ?: "") }
    var tip by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.tip ?: "") }
    var koli by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.koli_adedi?.toString() ?: "") }
    var kutu by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.kutu_adedi?.toString() ?: "") }
    var tanim by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.tanim ?: "") }

    var ambalajKg by rememberSaveable {
        mutableStateOf((urun as? KimyasalUrun)?.ambalaj_kg ?: "")
    }
    var kullanimAmaci by rememberSaveable {
        mutableStateOf((urun as? KimyasalUrun)?.kullanim_amaci ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürünü Düzenle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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

            // -------- ORTAK ALANLAR --------

            item { OutlinedTextField(urunAdi, { urunAdi = it }, label = { Text("Ürün Adı") }) }
            item { OutlinedTextField(marka, { marka = it }, label = { Text("Marka") }) }
            item { OutlinedTextField(kod, { kod = it }, label = { Text("Kod") }) }

            // -------- KATEGORİYE GÖRE --------

            when (urun) {

                is EvselUrun -> {

                    item { OutlinedTextField(pompa, { pompa = it }, label = { Text("Pompa") }) }
                    item { OutlinedTextField(asama, { asama = it }, label = { Text("Aşama") }) }
                    item { OutlinedTextField(membran, { membran = it }, label = { Text("Membran (GPD)") }) }
                    item { OutlinedTextField(tank, { tank = it }, label = { Text("Tank (Galon)") }) }
                }

                is EndustriyelUrun -> {

                    item { OutlinedTextField(model, { model = it }, label = { Text("Model") }) }
                    item { OutlinedTextField(debi, { debi = it }, label = { Text("Debi (m³/saat)") }) }
                }

                is PeyzajUrun -> {

                    item { OutlinedTextField(nozul, { nozul = it }, label = { Text("Nozul Rengi") }) }
                    item { OutlinedTextField(atis, { atis = it }, label = { Text("Atış Mesafesi") }) }
                    item { OutlinedTextField(basinc, { basinc = it }, label = { Text("Basınç") }) }
                    item { OutlinedTextField(ebat, { ebat = it }, label = { Text("Ebat") }) }
                    item { OutlinedTextField(istasyon, { istasyon = it }, label = { Text("İstasyon Sayısı") }) }
                    item { OutlinedTextField(ambalaj, { ambalaj = it }, label = { Text("Ambalaj Adedi") }) }
                    item { OutlinedTextField(tip, { tip = it }, label = { Text("Tip") }) }
                    item { OutlinedTextField(koli, { koli = it }, label = { Text("Koli Adedi") }) }
                    item { OutlinedTextField(kutu, { kutu = it }, label = { Text("Kutu Adedi") }) }
                    item { OutlinedTextField(nozul, { nozul = it }, label = { Text("Nozul Rengi") }) }
                    item { OutlinedTextField(basinc, { basinc = it }, label = { Text("Basınç") }) }
                    item { OutlinedTextField(tanim, { tanim = it }, label = { Text("Tanım") }) }
                }
                is KimyasalUrun -> {

                    item {
                        OutlinedTextField(
                            ambalajKg,
                            { ambalajKg = it },
                            label = { Text("Ambalaj / KG") }
                        )
                    }

                    item {
                        OutlinedTextField(
                            kullanimAmaci,
                            { kullanimAmaci = it },
                            label = { Text("Kullanım Amacı") }
                        )
                    }
                }
            }

            // -------- FİYAT --------

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    OutlinedTextField(
                        value = fiyat,
                        onValueChange = { fiyat = it },
                        label = { Text("Fiyat") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Box {
                        OutlinedButton(onClick = { expandedCurrency = true }) {
                            Text(paraBirimi)
                        }

                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencyOptions.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        paraBirimi = it
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -------- KAYDET --------

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {

                        val updated: BaseUrun = when (urun) {

                            is EvselUrun -> urun.copy(
                                urun_adi = urunAdi,
                                marka = marka,
                                pompa = pompa,
                                asama = asama,
                                membran_gpd = membran.toDoubleOrNull(),
                                tank_galon = tank.toDoubleOrNull(),
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )

                            is EndustriyelUrun -> urun.copy(
                                urun_adi = urunAdi,
                                marka = marka,
                                model = model,
                                debi_m3_saat = debi.toDoubleOrNull(),
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )

                            is PeyzajUrun -> urun.copy(
                                urun_adi = urunAdi,
                                marka = marka,
                                ebat = ebat,
                                istasyon_sayisi = istasyon.toIntOrNull(),
                                ambalaj_adedi = ambalaj.toIntOrNull(),
                                tip = tip,
                                koli_adedi = koli.toIntOrNull(),
                                kutu_adedi = kutu.toIntOrNull(),
                                nozul_rengi = nozul,
                                atis_mesafesi = atis,
                                basinc_degeri = basinc,
                                tanim = tanim,
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )
                            is KimyasalUrun -> urun.copy(
                                urun_adi = urunAdi,
                                ambalaj_kg = ambalajKg,
                                kullanim_amaci = kullanimAmaci,
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )
                        }

                        ProductRepository.updateProduct(productId, updated)

                        scope.launch {
                            snackbarHostState.showSnackbar("Kaydedildi")
                            delay(1000)
                            navController.popBackStack()
                        }
                    }
                ) { Text("Kaydet") }
            }

            // -------- SİL --------

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = { showDeleteDialog = true }
                ) { Text("Sil") }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Ürünü sil") },
                text = { Text("Silmek istediğinize emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            Firebase.firestore
                                .collection("products")
                                .document(productId)
                                .delete()

                            navController.popBackStack()
                        }
                    ) { Text("Evet") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Hayır")
                    }
                }
            )
        }
    }
}