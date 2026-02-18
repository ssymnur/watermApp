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
    val anaKategori: String = "",
    val altKategori: String = "",

    val urun_adi: String = "",
    val model: String? = null,
    val marka: String? = null,

    val pompa: String? = null,
    val asama: String? = null,

    val membran_gpd: Double? = null,
    val tank_galon: Double? = null,

    val tank_baglanti: String? = null,
    val giris_cikis_baglanti: String? = null,
    val debi_m3_saat: Double? = null,

    val urun_kodu: Int? = null,
    val fiyat: Double? = null,
    val paraBirimi: String = "USD",

    val ozellikler: String? = null
)


