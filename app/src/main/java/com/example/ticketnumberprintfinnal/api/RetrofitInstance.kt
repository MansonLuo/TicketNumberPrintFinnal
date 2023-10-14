package com.example.ticketnumberprintfinnal.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitInstance {
    private var url = "http://192.168.44.1/"

    private var retrofit =
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val mBrushService: MBrushService by lazy {
        retrofit.create(MBrushService::class.java)
    }
}