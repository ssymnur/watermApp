package com.seymanur.watermapp.utils

import com.google.firebase.firestore.DocumentSnapshot

fun getFirestoreValueAsString(
    doc: DocumentSnapshot,
    field: String
): String {

    return when (val value = doc.get(field)) {

        is Double -> value.toString()
        is Long -> value.toString()
        is String -> value
        else -> ""
    }
}