package com.example.watermapp.view

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

    DisposableEffect(anaKategori, ustKategori) {

        // 🔹 1️⃣ PEYZAJ ANA KATEGORİ
        if (anaKategori == "Peyzaj" && ustKategori == null) {

            kategoriler = listOf("POELSAN", "RAINBIRD")

            onDispose { }

        }

        // 🔹 2️⃣ PEYZAJ → POELSAN ALT KATEGORİLERİ
        else if (anaKategori == "Peyzaj" && ustKategori == "POELSAN") {

            kategoriler = listOf(
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

            onDispose { }

        }
        // 🔹 KİMYASAL ANA KATEGORİ
        else if (anaKategori == "Kimyasal" && ustKategori == null) {

            kategoriler = listOf("Havuz")

            onDispose { }
        }

        // 🔹 3️⃣ DİĞER KATEGORİLER (Firebase’den ÇEK)
        else {

            val listener = FirebaseFirestore.getInstance()
                .collection("products")
                .addSnapshotListener { snapshot, error ->

                    Log.d("FIREBASE", "snapshot size = ${snapshot?.size()}")
                    Log.d("FIREBASE", "error = $error")

                    snapshot?.documents?.forEach {
                        Log.d("FIREBASE", it.data.toString())
                    }

                    val list = snapshot?.documents
                        ?.mapNotNull { doc ->

                            val ana = doc.getString("anaKategori")

                            if (ana == anaKategori)
                                doc.getString("altKategori")
                            else null
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
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.navigate("cart") }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Sepet"
                        )
                    }
                }
            }
        },

        floatingActionButton = {
            if (anaKategori == "Endüstriyel") {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("projeler/Endüstriyel")
                    }
                ) {
                    Icon(
                        Icons.Default.Folder,
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

                            // 🔥 Eğer Peyzaj ana ekrandaysak → üst kategoriye git
                            if (ustKategori == null && anaKategori == "Peyzaj") {

                                navController.navigate(
                                    "detail/$anaKategori/${kategori.toNavArg()}"
                                )

                            } else {

                                navController.navigate(
                                    "urunler/$anaKategori/${kategori.toNavArg()}"
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