package com.example.watermapp

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Category(
    val name: String,
    val products: SnapshotStateList<Product> = mutableStateListOf()
)




