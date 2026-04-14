package com.example.watermapp.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.BaseUrun
import com.example.watermapp.EndustriyelUrun
import com.example.watermapp.EvselUrun
import com.example.watermapp.KimyasalUrun
import com.example.watermapp.PeyzajUrun
import com.example.watermapp.data.CartManager
import com.example.watermapp.toNavArg
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenProducts(
    anaKategori: String,
    kategoriAdi: String,
    navController: NavHostController
) {

    var urunler by remember {
        mutableStateOf<List<BaseUrun>>(emptyList())
    }

    // 🔥 FIREBASE CANLI LİSTE (SEALED CLASS UYUMLU)
    DisposableEffect(anaKategori, kategoriAdi) {

        val listener = FirebaseFirestore.getInstance()
            .collection("products")
            .whereEqualTo("anaKategori", anaKategori)
            .whereEqualTo("altKategori", kategoriAdi)
            .addSnapshotListener { snapshot, _ ->

                urunler = snapshot?.documents?.mapNotNull { doc ->

                    when (doc.getString("anaKategori")) {

                        "Evsel" ->
                            doc.toObject(EvselUrun::class.java)?.copy(id = doc.id)

                        "Endüstriyel" ->
                            doc.toObject(EndustriyelUrun::class.java)?.copy(id = doc.id)

                        "Peyzaj" ->
                            doc.toObject(PeyzajUrun::class.java)?.copy(id = doc.id)

                        "Kimyasal" ->
                            doc.toObject(KimyasalUrun::class.java)?.copy(id = doc.id)

                        else -> null
                    }

                } ?: emptyList()
            }

        onDispose { listener.remove() }
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text(kategoriAdi) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    if (CartManager.SessionManager.isStaff) {
                        IconButton(onClick = {
                            navController.navigate(
                                "add_product_form/${anaKategori.toNavArg()}/${kategoriAdi.toNavArg()}"
                            )
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "")
                        }
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = { navController.navigate("cart") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ShoppingCart, "Sepet")
                }
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            items(urunler) { urun ->

                val qty = CartManager.getQuantity(urun)
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(modifier = Modifier.weight(1f)) {

                            Text(
                                urun.urun_adi,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text("Fiyat: ${urun.fiyat ?: "-"} ${urun.paraBirimi ?: ""}")

                            urun.marka
                                ?.takeIf { it.isNotBlank() }
                                ?.let { Text("Marka: $it") }
                            urun.urun_kodu?.let { Text("Kod: $it") }

                            // 🔥 KATEGORİYE GÖRE TÜM ÖZELLİKLER

                            when (urun) {

                                is EvselUrun -> {
                                    urun.pompa?.let { Text("Pompa: $it") }
                                    urun.asama?.let { Text("Aşama: $it") }
                                    urun.membran_gpd?.let { Text("Membran: $it GPD") }
                                    urun.tank_galon?.let { Text("Tank: $it Galon") }
                                }

                                is EndustriyelUrun -> {
                                    urun.model?.let { Text("Model: $it") }
                                    urun.tank_baglanti?.let { Text("Tank Bağlantı: $it") }
                                    urun.giris_cikis_baglanti?.let { Text("Giriş/Çıkış: $it") }
                                    urun.debi_m3_saat?.let { Text("Debi: $it m3/saat") }
                                }

                                is PeyzajUrun -> {

                                    urun.ebat?.takeIf { it.isNotBlank() }?.let {
                                        Text("Ebat: $it")
                                    }

                                    urun.istasyon_sayisi?.let {
                                        Text("İstasyon Sayısı: $it")
                                    }

                                    urun.ambalaj_adedi?.let {
                                        Text("Ambalaj Adedi: $it")
                                    }

                                    urun.tip?.takeIf { it.isNotBlank() }?.let {
                                        Text("Tip: $it")
                                    }

                                    urun.koli_adedi?.let {
                                        Text("Koli Adedi: $it")
                                    }

                                    urun.kutu_adedi?.let {
                                        Text("Kutu Adedi: $it")
                                    }

                                    urun.nozul_rengi?.takeIf { it.isNotBlank() }?.let {
                                        Text("Nozul: $it")
                                    }

                                    urun.atis_mesafesi?.takeIf { it.isNotBlank() }?.let {
                                        Text("Atış: $it")
                                    }

                                    urun.basinc_degeri?.takeIf { it.isNotBlank() }?.let {
                                        Text("Basınç: $it")
                                    }

                                    urun.tanim?.takeIf { it.isNotBlank() }?.let {
                                        Text("Tanım: $it")
                                    }
                                }
                                is KimyasalUrun -> {
                                    urun.ambalaj_kg?.let { Text("Ambalaj/KG: $it") }
                                    urun.kullanim_amaci?.let { Text("Kullanım Amacı: $it") }
                                }
                            }

                            TextButton(onClick = { expanded = !expanded }) {
                                Text(if (expanded) "Gizle" else "Açıklama")
                            }

                            if (expanded) {
                                Text(
                                    text = urun.ozellikler ?: "Bilgi yok",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (CartManager.SessionManager.isStaff) {
                                TextButton(onClick = {
                                    navController.navigate("edit/${urun.id}")
                                }) {
                                    Text("Düzenle")
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            IconButton(onClick = { CartManager.remove(urun) }) {
                                Text("-", style = MaterialTheme.typography.headlineMedium)
                            }

                            Text(
                                text = qty.toString(),
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(onClick = { CartManager.add(urun) }) {
                                Text("+", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}