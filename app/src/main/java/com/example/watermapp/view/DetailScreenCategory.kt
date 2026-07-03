package com.example.watermapp.view

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.toNavArg
import com.google.firebase.firestore.FirebaseFirestore
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenCategory(
    anaKategori: String,
    ustKategori: String? = null,
    navController: NavHostController
) {

    var kategoriler by remember { mutableStateOf<List<String>>(emptyList()) }

    fun getStaticCategories(): List<String>? {

        return when {

            // 🔹 PEYZAJ ANA KATEGORİ
            anaKategori == "Peyzaj" && ustKategori == null -> {
                listOf("POELSAN", "RAINBIRD")
            }

            // 🔹 POELSAN
            anaKategori == "Peyzaj" && ustKategori == "POELSAN" -> {
                listOf(
                    "CUP/BT PİLLİ SULAMA KONTROL ÜNİTESİ",
                    "SPREYLER VE NOZULLAR",
                    "ELEKTROFÜZYON VE SPİGOT EK PARÇALARI",
                    "MAVİ SERİ",
                    "YEŞİL SERİ",
                    "PRİZ KOLYE",
                    "PRİZ KOLYE GEÇMELİ",
                    "LAYFLAT EK PARÇALARI",
                    "DİŞLİ BAĞLANTI ELEMANLARI",
                    "KÜRESEL VANALAR",
                    "CAN SUYU VANA",
                    "VANA KUTULARI",
                    "PLASTİK FİLTRE",
                    "MİNİ VANA (DN 8)",
                    "MİNİ VANA (DN 15)",
                    "HAT SONU TAHLİYE VANASI",
                    "KİLİTLİ BAĞLANTI ELEMANLARI",
                    "DAMLA SULAMA KURTAĞZI",
                    "DAMLA SULAMA SOMUNLU",
                    "YASSI DAMLA SULAMA SOMUNLU",
                    "YASSI DAMLA SULAMA YÜZÜKLÜ",
                    "MİNİ SPRİNKLER VE EK PARÇALARI",
                    "HAT ÜSTÜ DAMLATICI"
                )
            }

            // 🔹 RAINBIRD
            anaKategori == "Peyzaj" && ustKategori == "RAINBIRD" -> {
                listOf(
                    "MIKRO / DAMLA SULAMA",
                    "SPREY BASLIKLAR",
                    "SPREY BASLIK NOZULLARI",
                    "SPREY BASLIK AKSESUARLARI",
                    "DÖNER (R-VAN) NOZULLAR",
                    "1/2 POP-UP ROTOR SPRINKLER",
                    "3/4 POP-UP ROTOR SPRINKLER",
                    "SPREY BASLIKLAR VE ORTA MESAFE ROTORLAR İÇİN AKSESUARLAR",
                    "1 POP-UP ROTOR",
                    "1.5 POP-UP ROTOR",
                    "VANALAR VE AKSESUARLARI",
                    "ELEKTRIK KABLOLARI VE AKSESUARLARI",
                    "POLYPRO & RAIN BIRD VANA KUTUSU SERISI",
                    "KONTROL CIHAZLARI (ELEKTRIKLI)",
                    "KONTROL CIHAZLARI (PILLI)",
                    "MERKEZI SULAMA KONTROL SISTEMI"
                )
            }

            // 🔹 KİMYASAL
            anaKategori == "Kimyasal" && ustKategori == null -> {
                listOf("Havuz")
            }



            else -> null
        }
    }

    DisposableEffect(anaKategori, ustKategori) {

        val static = getStaticCategories()

        if (static != null) {
            kategoriler = static
            onDispose { }
        } else {

            val listener = FirebaseFirestore.getInstance()
                .collection("products")
                .addSnapshotListener { snapshot, _ ->

                    val list = snapshot?.documents
                        ?.mapNotNull { doc ->
                            val ana = doc.getString("anaKategori")
                            if (ana == anaKategori) doc.getString("altKategori") else null
                        }
                        ?.distinct()
                        ?.sorted()

                    if (!list.isNullOrEmpty()) {
                        kategoriler = list
                    }
                }

            onDispose { listener.remove() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (ustKategori == null)
                            "$anaKategori Kategorileri"
                        else
                            ustKategori
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },

        floatingActionButton = {

            // SADECE ANA KATEGORİLERDE GÖZÜKSÜN
            if (anaKategori == "Endüstriyel" && ustKategori == null) {

                FloatingActionButton(
                    onClick = {
                        navController.navigate(
                            "projeler/${Uri.encode(anaKategori)}"
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Projeler"
                    )
                }
            }
        },

        floatingActionButtonPosition = FabPosition.End
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            items(kategoriler) { kategori ->

                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {

                            if (ustKategori == null) {

                                when (anaKategori) {

                                    "Peyzaj" -> {
                                        // 🔥 2 aşamalı devam et
                                        navController.navigate(
                                            "detail/${anaKategori.toNavArg()}/${kategori.toNavArg()}"
                                        )
                                    }

                                    "Evsel", "Endüstriyel", "Kimyasal" -> {
                                        // 🔥 DİREKT ÜRÜNLERE GİT
                                        navController.navigate(
                                            "urunler/${anaKategori.toNavArg()}/GENEL/${kategori.toNavArg()}"
                                        )
                                    }
                                }

                            } else {
                                // 🔥 Peyzaj altından gelenler
                                navController.navigate(
                                    "urunler/${anaKategori.toNavArg()}/${ustKategori.toNavArg()}/${kategori.toNavArg()}"
                                )
                            }
                        }
                ) {
                    Text(
                        text = kategori,
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}