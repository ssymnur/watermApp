package com.seymanur.watermapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    val api: CurrencyApi by lazy {

        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)

    }

}