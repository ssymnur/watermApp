package com.example.watermapp

// ---------------- ROOT ----------------

data class Root(
    val kategoriler: List<Kategori>
)

data class Kategori(
    val kategoriAdi: String,
    val urunler: List<BaseUrun>
)

// ---------------- BASE MODEL ----------------

sealed class BaseUrun {

    abstract val id: String
    abstract val anaKategori: String
    abstract val altKategori: String

    abstract val urun_adi: String
    abstract val marka: String?
    abstract val urun_kodu: String?
    abstract val fiyat: Double?
    abstract val paraBirimi: String
    abstract val ozellikler: String?
}

// ---------------- EVSEL ----------------

data class EvselUrun(
    override val id: String = "",
    override val anaKategori: String = "Evsel",
    override val altKategori: String = "",

    override val urun_adi: String = "",
    override val marka: String? = null,
    override val fiyat: Double? = null,
    override val paraBirimi: String = "USD",
    override val ozellikler: String? = null,
    override val urun_kodu: String? = null,

    val pompa: String? = null,
    val asama: String? = null,
    val membran_gpd: Double? = null,
    val tank_galon: Double? = null,

) : BaseUrun()

// ---------------- ENDÜSTRİYEL ----------------

data class EndustriyelUrun(
    override val id: String = "",
    override val anaKategori: String = "Endüstriyel",
    override val altKategori: String = "",

    override val urun_adi: String = "",
    override val marka: String? = null,
    override val fiyat: Double? = null,
    override val paraBirimi: String = "EUR",
    override val ozellikler: String? = null,
    override val urun_kodu: String? = null,

    val model: String? = null,
    val tank_baglanti: String? = null,
    val giris_cikis_baglanti: String? = null,
    val debi_m3_saat: Double? = null
) : BaseUrun()

// ---------------- PEYZAJ ----------------

data class PeyzajUrun(
    override val id: String = "",
    override val anaKategori: String = "Peyzaj",
    override val altKategori: String = "",

    override val urun_adi: String = "",
    override val marka: String? = null,
    override val fiyat: Double? = null,
    override val paraBirimi: String = "USD",
    override val ozellikler: String? = null,
    override val urun_kodu: String? = null,

    val ebat: String? = null,
    val istasyon_sayisi: Int? = null,
    val ambalaj_adedi: Int? = null,
    val tip: String? = null,
    val koli_adedi: Int? = null,
    val kutu_adedi: Int? = null,
    val nozul_rengi: String? = null,
    val atis_mesafesi: String? = null,
    val basinc_degeri: String? = null,
    val tanim: String? = null,


) : BaseUrun()