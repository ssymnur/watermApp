package com.example.watermapp

import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

object ProductRepository {

    private val db = Firebase.firestore


    // ✅ Ürün Güncelle
    fun updateProduct(
        productId: String,
        urun: BaseUrun,
        projeAdi: String? = null
    ) {

        val collectionRef = if (projeAdi == null) {
            db.collection("products")
        } else {
            db.collection("projects")
                .document(projeAdi)
                .collection("urunler")
        }

        collectionRef.document(productId).set(urun)
    }


    // ⭐ PRODUCTS (LIVE SNAPSHOT LISTENER)
    fun getProducts(
        anaKategori: String,
        altKategori: String,
        onResult: (List<BaseUrun>) -> Unit
    ): ListenerRegistration {

        return db.collection("products")
            .whereEqualTo("anaKategori", anaKategori)
            .whereEqualTo("altKategori", altKategori)
            .addSnapshotListener { snapshot, _ ->

                val list = snapshot?.documents?.mapNotNull { doc ->

                    when (doc.getString("anaKategori")) {

                        "Evsel" ->
                            doc.toObject(EvselUrun::class.java)
                                ?.copy(id = doc.id)

                        "Endüstriyel" ->
                            doc.toObject(EndustriyelUrun::class.java)
                                ?.copy(id = doc.id)

                        "Peyzaj" ->
                            doc.toObject(PeyzajUrun::class.java)
                                ?.copy(id = doc.id)

                        else -> null
                    }

                } ?: emptyList()

                onResult(list)
            }
    }


    // ⭐ PROJE ÜRÜNLERİ (LIVE SNAPSHOT)
    fun getProjectProducts(
        projeAdi: String,
        onResult: (List<BaseUrun>) -> Unit
    ): ListenerRegistration {

        return db.collection("projects")
            .document(projeAdi)
            .collection("urunler")
            .addSnapshotListener { snapshot, _ ->

                val list = snapshot?.documents?.mapNotNull { doc ->

                    when (doc.getString("anaKategori")) {

                        "Evsel" ->
                            doc.toObject(EvselUrun::class.java)
                                ?.copy(id = doc.id)

                        "Endüstriyel" ->
                            doc.toObject(EndustriyelUrun::class.java)
                                ?.copy(id = doc.id)

                        "Peyzaj" ->
                            doc.toObject(PeyzajUrun::class.java)
                                ?.copy(id = doc.id)

                        else -> null
                    }

                } ?: emptyList()

                onResult(list)
            }
    }
}