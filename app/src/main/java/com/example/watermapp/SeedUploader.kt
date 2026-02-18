package com.example.watermapp

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
object SeedUploader {

    private val db = Firebase.firestore

    fun uploadFromJson(
        context: Context,
        anaKategori: String,
        loader: (Context) -> Root
    ) {

        val data = loader(context)

        data.kategoriler.forEach { kategori ->

            kategori.urunler.forEach { urun ->

                val map = hashMapOf(
                    "anaKategori" to anaKategori,
                    "altKategori" to kategori.kategoriAdi,

                    "urun_adi" to urun.urun_adi,
                    "model" to urun.model,
                    "marka" to urun.marka,

                    "pompa" to urun.pompa,
                    "asama" to urun.asama,

                    "membran_gpd" to urun.membran_gpd,
                    "tank_galon" to urun.tank_galon,

                    "tank_baglanti" to urun.tank_baglanti,
                    "giris_cikis_baglanti" to urun.giris_cikis_baglanti,
                    "debi_m3_saat" to urun.debi_m3_saat,

                    "urun_kodu" to urun.urun_kodu,
                    "fiyat" to urun.fiyat,
                    "paraBirimi" to urun.paraBirimi,

                    "ozellikler" to urun.ozellikler
                )


                db.collection("products")
                    .document("${anaKategori}_${urun.urun_kodu}") // çakışmayı önler
                    .set(map)
            }
        }
    }
}
