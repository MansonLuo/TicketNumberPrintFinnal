package com.example.ticketnumberprintfinnal.api

import com.example.ticketnumberprintfinnal.api.models.Status
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MBrushService {
    @GET("cgi-bin/cmd?cmd=rm_upload")
    suspend fun removeUpload(): Status

    @Multipart
    @POST("cgi-bin/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part
    ): Status
}