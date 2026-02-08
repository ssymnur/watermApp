package com.example.watermapp

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object ProductRepository {

    private val db = Firebase.firestore

    fun updateProduct(id: String, urun: Urun) {
        db.collection("products")
            .document(id)
            .set(urun)
    }

    fun getProducts(onResult: (List<Urun>) -> Unit) {
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->

                val list = result.map { doc ->
                    val urun = doc.toObject(Urun::class.java)
                    urun.copy(id = doc.id)
                }

                onResult(list)
            }
    }
}
