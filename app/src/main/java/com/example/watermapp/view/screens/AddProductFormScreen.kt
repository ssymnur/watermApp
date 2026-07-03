package com.example.watermapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import com.example.watermapp.EndustriyelUrun
import com.example.watermapp.EvselUrun
import com.example.watermapp.KimyasalUrun
import com.example.watermapp.PeyzajUrun
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.String


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductFormScreen(
    navController: NavHostController,
    anaKategori: String,
    ustKategori: String,
    altKategori: String
) {
    //FİYAT

    var urunAdi by rememberSaveable { mutableStateOf("") }
    var marka by rememberSaveable { mutableStateOf("") }
    var pompa by rememberSaveable { mutableStateOf("") }
    var asama by rememberSaveable { mutableStateOf("") }
    var membran by rememberSaveable { mutableStateOf("") }
    var tank by rememberSaveable { mutableStateOf("") }
    var kod by rememberSaveable { mutableStateOf("") }
    var fiyat by rememberSaveable { mutableStateOf("") }
    var ozellikler by rememberSaveable { mutableStateOf("") }

    // Endüstriyel alanlar
    var tankBaglanti by rememberSaveable { mutableStateOf("") }
    var girisCikis by rememberSaveable { mutableStateOf("") }
    var debi by rememberSaveable { mutableStateOf("") }
    var sayfa by rememberSaveable { mutableStateOf("") }
    var sayfaPdf by rememberSaveable { mutableStateOf("") }

    var paraBirimi by rememberSaveable { mutableStateOf("USD") }
    var expanded by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    //PEYZAJ ALANLAR
    var ebat by rememberSaveable { mutableStateOf("") }
    var istasyonSayisi by rememberSaveable { mutableStateOf("") }
    var ambalajAdedi by rememberSaveable { mutableStateOf("") }
    var tip by rememberSaveable { mutableStateOf("") }
    var koliAdedi by rememberSaveable { mutableStateOf("") }
    var tanim by rememberSaveable { mutableStateOf("") }
    var kutuAdedi by rememberSaveable { mutableStateOf("") }
    var basincDegeri by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var kodRain by rememberSaveable { mutableStateOf("") }
    var aciklama by rememberSaveable { mutableStateOf("") }
    var agirlikKg by rememberSaveable { mutableStateOf("") }
    var bayiiAdedi by rememberSaveable { mutableStateOf("") }
    var koliAdediRain by rememberSaveable { mutableStateOf("") }
    var paletAdedi by rememberSaveable { mutableStateOf("") }

    //KİMYASAL
    var ambalajKg by rememberSaveable { mutableStateOf("") }
    var kullanimAmaci by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürün Ekle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Text("Kategori: $anaKategori / $altKategori")
            }

            fun field(value: String, onChange: (String) -> Unit, label: String) {
                item {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ✅ Ortak alanlar: ürün adı, marka, ürün kodu, fiyat, özellikler
            field(urunAdi, { urunAdi = it }, "Ürün Adı")
            field(kod, { kod = it }, "Ürün Kodu")
            field(fiyat, { fiyat = it }, "Fiyat")
            field(ozellikler, { ozellikler = it }, "Özellikler")

            // ✅ Kategoriye özel alanlar
            when (anaKategori) {

                "Evsel" -> {
                    field(pompa, { pompa = it }, "Pompa")
                    field(asama, { asama = it }, "Aşama")
                    field(membran, { membran = it }, "Membran GPD")
                    field(tank, { tank = it }, "Tank Galon")
                    field(marka, { marka = it }, "Marka")
                }

                "Endüstriyel" -> {
                    field(sayfa, { sayfa = it }, "Model")
                    field(debi, { debi = it }, "Debi m³/saat")
                    field(tankBaglanti, { tankBaglanti = it }, "Tank Bağlantı")
                    field(girisCikis, { girisCikis = it }, "Giriş / Çıkış")
                    field(marka, { marka = it }, "Marka")
                }

                "Peyzaj" -> {

                    if (ustKategori.trim().equals("RAINBIRD", ignoreCase = true)) {

                        field(model, { model = it }, "Model")
                        field(kodRain, { kodRain = it }, "Kod")
                        field(aciklama, { aciklama = it }, "Açıklama")

                        field(agirlikKg, { agirlikKg = it }, "Ağırlık / KG")
                        field(bayiiAdedi, { bayiiAdedi = it }, "Bayii Adedi")
                        field(koliAdediRain, { koliAdediRain = it }, "Koli Adedi")
                        field(paletAdedi, { paletAdedi = it }, "Palet Adedi")

                    } else {
                        // POELSAN eski alanlar
                        field(sayfa, { sayfa = it }, "Nozul Rengi")
                        field(sayfaPdf, { sayfaPdf = it }, "Atış Mesafesi")
                        field(ebat, { ebat = it }, "Ebat")
                        field(istasyonSayisi, { istasyonSayisi = it }, "İstasyon Sayısı")
                        field(ambalajAdedi, { ambalajAdedi = it }, "Ambalaj Adedi")
                        field(tip, { tip = it }, "Tip")
                        field(koliAdedi, { koliAdedi = it }, "Koli Adedi")
                        field(kutuAdedi, { kutuAdedi = it }, "Kutu Adedi")
                        field(basincDegeri, { basincDegeri = it }, "Basınç Değeri")
                        field(tanim, { tanim = it }, "Tanım")
                    }
                }
                "Kimyasal" -> {
                    field(ambalajKg, { ambalajKg = it }, "Ambalaj / KG")
                    field(kullanimAmaci, { kullanimAmaci = it }, "Kullanım Amacı")
                }
            }

            // ✅ Para birimi dropdown
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
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

            // ✅ Kaydet butonu
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val product = when (anaKategori) {

                            "Evsel" -> EvselUrun(
                                anaKategori = anaKategori,
                                altKategori = altKategori,
                                urun_adi = urunAdi,
                                marka = marka,
                                pompa = pompa,
                                asama = asama,
                                membran_gpd = membran.toDoubleOrNull(),
                                tank_galon = tank.toDoubleOrNull(),
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi,
                                ozellikler = ozellikler
                            )

                            "Endüstriyel" -> EndustriyelUrun(
                                anaKategori = anaKategori,
                                altKategori = altKategori,
                                urun_adi = urunAdi,
                                marka = marka,
                                model = sayfa,
                                debi_m3_saat = debi.toDoubleOrNull(),
                                tank_baglanti = tankBaglanti,
                                giris_cikis_baglanti = girisCikis,
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi,
                                ozellikler = ozellikler
                            )

                            "Peyzaj" -> {

                                if (altKategori.trim().equals("RAINBIRD", ignoreCase = true)) {

                                    PeyzajUrun(
                                        anaKategori = anaKategori,
                                        altKategori = altKategori,
                                        urun_adi = urunAdi,
                                        urun_kodu = kodRain.ifBlank { null },
                                        fiyat = fiyat.toDoubleOrNull(),
                                        paraBirimi = paraBirimi,

                                        model = model,
                                        aciklama = aciklama,
                                        agirlik_kg = agirlikKg,
                                        bayii_adedi = bayiiAdedi.toIntOrNull(),
                                        rainbird_koli_adedi = koliAdediRain.toIntOrNull(),
                                        palet_adedi = paletAdedi.toIntOrNull()
                                    )

                                } else {

                                    PeyzajUrun(
                                        anaKategori = anaKategori,
                                        altKategori = altKategori,
                                        urun_adi = urunAdi,
                                        marka = marka,
                                        urun_kodu = kod.ifBlank { null },
                                        fiyat = fiyat.toDoubleOrNull(),
                                        paraBirimi = paraBirimi,
                                        ozellikler = ozellikler,

                                        ebat = ebat,
                                        istasyon_sayisi = istasyonSayisi.toIntOrNull(),
                                        ambalaj_adedi = ambalajAdedi.toIntOrNull(),
                                        tip = tip,
                                        koli_adedi = koliAdedi.toIntOrNull(),
                                        kutu_adedi = kutuAdedi.toIntOrNull(),
                                        nozul_rengi = sayfa,
                                        atis_mesafesi = sayfaPdf,
                                        basinc_degeri = basincDegeri,
                                        tanim = tanim
                                    )
                                }
                            }
                            "Kimyasal" -> KimyasalUrun(
                                anaKategori = anaKategori,
                                altKategori = altKategori,
                                urun_adi = urunAdi,
                                marka = null, // boş bırakmak daha doğru
                                urun_kodu = null,
                                ambalaj_kg = ambalajKg,
                                kullanim_amaci = kullanimAmaci,
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi,
                                ozellikler = ozellikler
                            )

                            else -> return@Button
                        }

                        Firebase.firestore
                            .collection("products")
                            .add(product)

                        scope.launch {
                            snackbar.showSnackbar("Ürün eklendi ✅")
                            delay(1000)
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            }

        }
    }
}