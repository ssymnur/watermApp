package com.example.watermapp

data class Root(
    val kategoriler: List<Kategori>
)

data class Kategori(
    val kategoriAdi: String,
    val urunler: List<Urun>
)

data class Urun(
    val id: String = "",
    val kategori: String = "",
    val urun_model: String = "",
    val marka: String? = "",
    val pompa: String? = "",
    val asama: String? = "",
    val membran_gpd: Double? = 0.0,
    val tank_galon: Double? = 0.0,
    val urun_kodu: Int? = 0,
    val fiyat_usd: Double? = 0.0,
    val ozellikler: String? = ""
)


