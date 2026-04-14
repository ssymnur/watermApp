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

                val map = hashMapOf<String, Any?>(
                    "anaKategori" to anaKategori,
                    "altKategori" to kategori.kategoriAdi,

                    "urun_adi" to urun.urun_adi,
                    "marka" to (urun.marka ?: ""),
                    "urun_kodu" to (urun.urun_kodu ?: ""),
                    "fiyat" to (urun.fiyat ?: 0.0),
                    "paraBirimi" to (urun.paraBirimi ?: "USD"),
                    "ozellikler" to (urun.ozellikler ?: "")
                )

                when (urun) {

                    is EvselUrun -> {
                        map["pompa"] = urun.pompa ?: ""
                        map["asama"] = urun.asama ?: ""
                        map["membran_gpd"] = urun.membran_gpd ?: 0.0
                        map["tank_galon"] = urun.tank_galon ?: 0.0
                    }

                    is EndustriyelUrun -> {
                        map["model"] = urun.model ?: ""
                        map["tank_baglanti"] = urun.tank_baglanti ?: ""
                        map["giris_cikis_baglanti"] = urun.giris_cikis_baglanti ?: ""
                        map["debi_m3_saat"] = urun.debi_m3_saat ?: 0.0
                    }

                    is PeyzajUrun -> {
                        map["ebat"] = urun.ebat ?: ""
                        map["istasyon_sayisi"] = urun.istasyon_sayisi ?: 0
                        map["ambalaj_adedi"] = urun.ambalaj_adedi ?: 0
                        map["tip"] = urun.tip ?: ""
                        map["koli_adedi"] = urun.koli_adedi ?: 0
                        map["kutu_adedi"] = urun.kutu_adedi ?: 0
                        map["nozul_rengi"] = urun.nozul_rengi ?: ""
                        map["atis_mesafesi"] = urun.atis_mesafesi ?: ""
                        map["basinc_degeri"] = urun.basinc_degeri ?: ""
                        map["tanim"] = urun.tanim ?: ""
                    }
                    is KimyasalUrun -> {
                        map["ambalaj_kg"] = urun.ambalaj_kg ?: ""
                        map["kullanim_amaci"] = urun.kullanim_amaci ?: ""
                    }
                }

                db.collection("products")
                    .add(map)
            }
        }
    }
}