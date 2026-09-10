package com.seymanur.watermapp.network

import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {

    @GET("latest")
    suspend fun getRates(
        @Query("from") from: String = "USD",
        @Query("to") to: String = "TRY,EUR"
    ): ExchangeResponse
}