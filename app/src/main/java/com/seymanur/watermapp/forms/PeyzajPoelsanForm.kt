package com.seymanur.watermapp.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PeyzajPoelsanForm(

    urunAdi: String,
    onUrunAdiChange: (String) -> Unit,

    marka: String,
    onMarkaChange: (String) -> Unit,

    kod: String,
    onKodChange: (String) -> Unit,

    fiyat: String,
    onFiyatChange: (String) -> Unit,

    ozellikler: String,
    onOzelliklerChange: (String) -> Unit,

    nozulRengi: String,
    onNozulRengiChange: (String) -> Unit,

    atisMesafesi: String,
    onAtisMesafesiChange: (String) -> Unit,

    ebat: String,
    onEbatChange: (String) -> Unit,

    istasyonSayisi: String,
    onIstasyonSayisiChange: (String) -> Unit,

    ambalajAdedi: String,
    onAmbalajAdediChange: (String) -> Unit,

    tip: String,
    onTipChange: (String) -> Unit,

    koliAdedi: String,
    onKoliAdediChange: (String) -> Unit,

    kutuAdedi: String,
    onKutuAdediChange: (String) -> Unit,

    basincDegeri: String,
    onBasincDegeriChange: (String) -> Unit,

    tanim: String,
    onTanimChange: (String) -> Unit

) {

    OutlinedTextField(
        value = urunAdi,
        onValueChange = onUrunAdiChange,
        label = { Text("Ürün Adı") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = marka,
        onValueChange = onMarkaChange,
        label = { Text("Marka") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = kod,
        onValueChange = onKodChange,
        label = { Text("Ürün Kodu") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = fiyat,
        onValueChange = onFiyatChange,
        label = { Text("Fiyat") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ozellikler,
        onValueChange = onOzelliklerChange,
        label = { Text("Özellikler") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = nozulRengi,
        onValueChange = onNozulRengiChange,
        label = { Text("Nozul Rengi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = atisMesafesi,
        onValueChange = onAtisMesafesiChange,
        label = { Text("Atış Mesafesi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ebat,
        onValueChange = onEbatChange,
        label = { Text("Ebat") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = istasyonSayisi,
        onValueChange = onIstasyonSayisiChange,
        label = { Text("İstasyon Sayısı") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ambalajAdedi,
        onValueChange = onAmbalajAdediChange,
        label = { Text("Ambalaj Adedi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = tip,
        onValueChange = onTipChange,
        label = { Text("Tip") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = koliAdedi,
        onValueChange = onKoliAdediChange,
        label = { Text("Koli Adedi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = kutuAdedi,
        onValueChange = onKutuAdediChange,
        label = { Text("Kutu Adedi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = basincDegeri,
        onValueChange = onBasincDegeriChange,
        label = { Text("Basınç Değeri") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = tanim,
        onValueChange = onTanimChange,
        label = { Text("Tanım") },
        modifier = Modifier.fillMaxWidth()
    )

}