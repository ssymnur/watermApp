package com.example.watermapp

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object ProductRepository {

    private val db = Firebase.firestore

    // 🔹 Ürün Güncelle (Hem kategori hem proje için)
    fun updateProduct(
        productId: String,
        urun: Urun,
        projeAdi: String? = null
    ) {

        if (projeAdi == null) {
            // Normal kategori ürünü
            db.collection("products")
                .document(productId)
                .set(urun)
        } else {
            // Proje içindeki ürün
            db.collection("projects")
                .document(projeAdi)
                .collection("urunler")
                .document(productId)
                .set(urun)
        }
    }

    // 🔹 Kategori ürünlerini getir
    fun getProducts(onResult: (List<Urun>) -> Unit) {

        db.collection("products")
            .get()
            .addOnSuccessListener { result ->

                val list = result.documents.mapNotNull { doc ->

                    val urun = doc.toObject(Urun::class.java)

                    urun?.copy(id = doc.id)

                }

                onResult(list)
            }
    }

    // 🔹 Proje ürünlerini getir
    fun getProjectProducts(
        projeAdi: String,
        onResult: (List<Urun>) -> Unit
    ) {

        db.collection("projects")
            .document(projeAdi)
            .collection("urunler")
            .get()
            .addOnSuccessListener { result ->

                val list = result.documents.mapNotNull { doc ->

                    val urun = doc.toObject(Urun::class.java)

                    urun?.copy(id = doc.id)

                }

                onResult(list)
            }
    }

}
