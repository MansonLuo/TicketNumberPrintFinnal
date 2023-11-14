package com.example.ticketnumberprintfinnal.api

import com.example.ticketnumberprintfinnal.api.models.Status
import kotlinx.coroutines.delay
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

    suspend fun simulateShortPress(): Status {
        return apiService.simulateShortPress()
    }

    suspend fun getPrinterStatu(): WorkingStatu {
        val stAllInfo = apiService.getPrinterStatu()

        val stId = stAllInfo.info.split(",").get(1).split(":").get(1).trim()
        val dataIndex = stAllInfo.info.split(",").get(2).split(":").get(1).trim().toInt()

        return when(stId) {
            "0" -> WorkingStatu.Idle(dataIndex = dataIndex)
            "1" -> WorkingStatu.Printing(dataIndex = dataIndex)
            "2" -> WorkingStatu.Cleaning
            else -> WorkingStatu.WaitTrigger
        }
    }
}


//st: 0: idle, 1: printing, 2: cleaning, 3: wait trigger (constant speed mode only)
sealed class WorkingStatu() {
    data class Idle(val dataIndex: Int, val description: String = "idle") : WorkingStatu()
    data class Printing(val dataIndex: Int, val description: String = "printing"): WorkingStatu()
    object Cleaning: WorkingStatu()
    object WaitTrigger: WorkingStatu()
}