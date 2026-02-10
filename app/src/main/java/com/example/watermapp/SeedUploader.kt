package com.example.watermapp

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object SeedUploader {

    private val db = Firebase.firestore

    fun uploadFromJson(context: Context) {

        val data = loadEvselData(context) // senin mevcut JSON loader

        data.kategoriler.forEach { kategori ->

            kategori.urunler.forEach { urun ->

                val map = hashMapOf(
                    "kategori" to kategori.kategoriAdi.normalizeCategory(),
                    "urun_model" to urun.urun_model,
                    "marka" to urun.marka,
                    "pompa" to urun.pompa,
                    "asama" to urun.asama,
                    "membran_gpd" to urun.membran_gpd,
                    "tank_galon" to urun.tank_galon,
                    "urun_kodu" to urun.urun_kodu,
                    "fiyat_usd" to urun.fiyat_usd,
                    "ozellikler" to urun.ozellikler
                )


                db.collection("products")
                    .document(urun.urun_kodu.toString())
                    .set(map)

            }
        }
    }
}
