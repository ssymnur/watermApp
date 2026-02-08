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
    val marka: String? = null,
    val pompa: String? = null,
    val asama: String? = null,
    val membran_gpd: Double? = null,
    val tank_galon: Double? = null,
    val urun_kodu: Int? = null,
    val fiyat_usd: Double? = null,
    val ozellikler: String? = null
)

