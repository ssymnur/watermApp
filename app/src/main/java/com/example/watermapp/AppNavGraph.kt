package com.example.watermapp

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.watermapp.view.CartScreen
import com.example.watermapp.view.CategoryScreen
import com.example.watermapp.view.DetailScreenCategory
import com.example.watermapp.view.DetailScreenProducts
import com.example.watermapp.view.projects.AddProjectProductFormScreen
import com.example.watermapp.view.projects.EditProjectProductScreen
import com.example.watermapp.view.projects.ProjeDetayScreen
import com.example.watermapp.view.projects.ProjelerScreen
import com.example.watermapp.view.screens.AddProductFormScreen
import com.example.watermapp.view.screens.CustomerFormScreen
import com.example.watermapp.view.screens.EditOrderScreen
import com.example.watermapp.view.screens.EditProductScreen
import com.example.watermapp.view.screens.OrdersScreen
import com.google.firebase.firestore.FirebaseFirestore


    @Composable
    fun AppNavigator(startDestination: String) {

        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            // ÜRÜN DÜZENLEME
            composable(
                route = "edit/{productId}",
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val productId =
                    backStackEntry.arguments?.getString("productId") ?: ""

                var urun by remember { mutableStateOf<BaseUrun?>(null) }

                LaunchedEffect(productId) {

                    FirebaseFirestore.getInstance()
                        .collection("products")
                        .document(productId)
                        .get()
                        .addOnSuccessListener { doc ->

                            if (!doc.exists()) return@addOnSuccessListener

                            val anaKategori = doc.getString("anaKategori")

                            urun = when (anaKategori) {

                                "Evsel" -> {
                                    EvselUrun(
                                        id = doc.id,
                                        anaKategori = anaKategori ?: "",
                                        altKategori = doc.getString("altKategori") ?: "",
                                        urun_adi = doc.getString("urun_adi") ?: "",
                                        marka = doc.getString("marka"),

                                        urun_kodu = doc.get("urun_kodu")?.toString(),

                                        fiyat = when (val f = doc.get("fiyat")) {
                                            is Number -> f.toDouble()
                                            is String -> f.toDoubleOrNull()
                                            else -> null
                                        },

                                        paraBirimi = doc.getString("paraBirimi") ?: "USD",
                                        ozellikler = doc.getString("ozellikler"),

                                        pompa = doc.getString("pompa"),
                                        asama = doc.getString("asama"),
                                        membran_gpd = (doc.get("membran_gpd") as? Number)?.toDouble(),
                                        tank_galon = (doc.get("tank_galon") as? Number)?.toDouble()
                                    )
                                }

                                "Endüstriyel" -> {
                                    EndustriyelUrun(
                                        id = doc.id,
                                        anaKategori = anaKategori ?: "",
                                        altKategori = doc.getString("altKategori") ?: "",
                                        urun_adi = doc.getString("urun_adi") ?: "",
                                        marka = doc.getString("marka"),

                                        urun_kodu = doc.get("urun_kodu")?.toString(),

                                        fiyat = when (val f = doc.get("fiyat")) {
                                            is Number -> f.toDouble()
                                            is String -> f.toDoubleOrNull()
                                            else -> null
                                        },

                                        paraBirimi = doc.getString("paraBirimi") ?: "USD",
                                        ozellikler = doc.getString("ozellikler"),

                                        model = doc.getString("model"),
                                        tank_baglanti = doc.getString("tank_baglanti"),
                                        giris_cikis_baglanti = doc.getString("giris_cikis_baglanti"),
                                        debi_m3_saat = (doc.get("debi_m3_saat") as? Number)?.toDouble()
                                    )
                                }

                                "Peyzaj" -> {
                                    PeyzajUrun(
                                        id = doc.id,
                                        anaKategori = anaKategori ?: "",
                                        altKategori = doc.getString("altKategori") ?: "",
                                        urun_adi = doc.getString("urun_adi") ?: "",
                                        marka = doc.getString("marka"),

                                        urun_kodu = doc.get("urun_kodu")?.toString(),

                                        fiyat = when (val f = doc.get("fiyat")) {
                                            is Number -> f.toDouble()
                                            is String -> f.toDoubleOrNull()
                                            else -> null
                                        },

                                        paraBirimi = doc.getString("paraBirimi") ?: "USD",
                                        ozellikler = doc.getString("ozellikler"),

                                        ebat = doc.getString("ebat"),
                                        istasyon_sayisi = (doc.get("istasyon_sayisi") as? Number)?.toInt(),
                                        ambalaj_adedi = (doc.get("ambalaj_adedi") as? Number)?.toInt(),
                                        tip = doc.getString("tip"),
                                        koli_adedi = (doc.get("koli_adedi") as? Number)?.toInt(),
                                        kutu_adedi = (doc.get("kutu_adedi") as? Number)?.toInt(),
                                        nozul_rengi = doc.getString("nozul_rengi"),
                                        atis_mesafesi = doc.getString("atis_mesafesi"),
                                        basinc_degeri = doc.getString("basinc_degeri"),
                                        tanim = doc.getString("tanim")
                                    )
                                }

                                else -> null
                            }
                        }
                }

                if (urun == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    EditProductScreen(
                        urun = urun!!,
                        productId = productId,
                        navController = navController
                    )
                }
            }
            composable(
                route = "edit_project_product/{urunId}/{projeAdi}",
                arguments = listOf(
                    navArgument("urunId") { type = NavType.StringType },
                    navArgument("projeAdi") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val urunId = backStackEntry.arguments?.getString("urunId") ?: ""

                val projeAdi = Uri.decode(
                    backStackEntry.arguments?.getString("projeAdi") ?: ""
                )

                EditProjectProductScreen(
                    urunId = urunId,
                    projeAdi = projeAdi,
                    navController = navController
                )
            }
            composable(
                route = "detail/{ana}/{ust}",
                arguments = listOf(
                    navArgument("ana") { type = NavType.StringType },
                    navArgument("ust") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val ana = backStackEntry.arguments?.getString("ana") ?: ""
                val ust = backStackEntry.arguments?.getString("ust")?.fromNavArg()

                DetailScreenCategory(
                    anaKategori = ana,
                    ustKategori = ust,
                    navController = navController
                )
            }

            // PROJELER LİSTESİ
            composable(
                route = "proje/{projeAdi}",
                arguments = listOf(
                    navArgument("projeAdi") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val projeAdi = Uri.decode(
                    backStackEntry.arguments?.getString("projeAdi") ?: ""
                )

                ProjeDetayScreen(
                    projeAdi = projeAdi,
                    navController = navController
                )
            }

            // PROJEYE ÜRÜN EKLE
            composable(
                route = "add_project_product_form/{projeAdi}",
                arguments = listOf(
                    navArgument("projeAdi") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val projeAdi = Uri.decode(
                    backStackEntry.arguments?.getString("projeAdi") ?: ""
                )

                AddProjectProductFormScreen(
                    navController = navController,
                    projeAdi = projeAdi
                )
            }
            // YENİ ÜRÜN FORMU
            composable(
                route = "add_product_form/{ana}/{alt}",
                arguments = listOf(
                    navArgument("ana") { type = NavType.StringType },
                    navArgument("alt") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val ana =
                    backStackEntry.arguments?.getString("ana")?.fromNavArg() ?: ""

                val alt =
                    backStackEntry.arguments?.getString("alt")?.fromNavArg() ?: ""

                AddProductFormScreen(navController, ana, alt)
            }

            // KATEGORİLER
            composable("categories") {
                MainLayout(navController) {
                    CategoryScreen(navController)
                }
            }

            composable(
                route = "kategori/{ana}",
                arguments = listOf(
                    navArgument("ana") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val ana = backStackEntry.arguments?.getString("ana") ?: ""

                DetailScreenCategory(
                    anaKategori = ana,
                    ustKategori = null,
                    navController = navController
                )
            }

            composable(
                route = "urunler/{ana}/{alt}",
                arguments = listOf(
                    navArgument("ana") { type = NavType.StringType },
                    navArgument("alt") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val ana =
                    backStackEntry.arguments?.getString("ana") ?: ""

                val alt =
                    backStackEntry.arguments?.getString("alt") ?: ""

                DetailScreenProducts(ana, alt, navController)
            }
            composable(
                route = "projeler/{kategori}",
                arguments = listOf(
                    navArgument("kategori") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val kategori =
                    backStackEntry.arguments?.getString("kategori") ?: ""

                ProjelerScreen(
                    navController = navController,
                    kategori = kategori
                )
            }

            // SİPARİŞLER
            composable("orders") {
                OrdersScreen(navController)
            }

            composable(
                route = "editOrder/{orderId}",
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val id =
                    backStackEntry.arguments?.getString("orderId") ?: ""

                EditOrderScreen(id, navController)
            }

            // SEPET
            composable("cart") {
                CartScreen(navController)
            }

            // MÜŞTERİ FORMU
            composable("customerForm") {
                CustomerFormScreen(navController)
            }

            // LOGIN
            composable(
                route = "projeler/{kategori}",
                arguments = listOf(navArgument("kategori") { type = NavType.StringType })
            ) { backStackEntry ->
                val kategori = backStackEntry.arguments?.getString("kategori") ?: ""
                ProjelerScreen(navController, kategori)
            }

            composable(
                route = "proje/{projeAdi}",
                arguments = listOf(navArgument("projeAdi") { type = NavType.StringType })
            ) { backStackEntry ->
                val projeAdi = Uri.decode(backStackEntry.arguments?.getString("projeAdi") ?: "")
                ProjeDetayScreen(projeAdi, navController)
            }

            composable(
                route = "edit_project_product/{urunId}/{projeAdi}",
                arguments = listOf(
                    navArgument("urunId") { type = NavType.StringType },
                    navArgument("projeAdi") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val urunId = backStackEntry.arguments?.getString("urunId") ?: ""
                val projeAdi = Uri.decode(backStackEntry.arguments?.getString("projeAdi") ?: "")
                EditProjectProductScreen(urunId, projeAdi, navController)
            }


        }
    }
