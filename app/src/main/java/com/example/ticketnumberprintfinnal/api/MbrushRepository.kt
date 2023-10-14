package com.example.ticketnumberprintfinnal.api

import com.example.ticketnumberprintfinnal.api.models.Status
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class MbrushRepository(private val apiService: MBrushService) {
    suspend fun removeUpload(): Status {
        return apiService.removeUpload()
    }

    suspend fun upload(
        mbdFilePath: String,
        pos: Int = 0
    ): Status {
        val file = File(mbdFilePath)

        val fileReq = RequestBody.create(MediaType.parse("*"), file)
        val filePart = MultipartBody.Part.createFormData("file", "$pos.mbd", fileReq)

        return apiService.upload(filePart)
    }
}