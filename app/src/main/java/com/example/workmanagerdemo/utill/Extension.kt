package com.example.workmanagerdemo.utill

import android.content.Context
import android.widget.Toast
import androidx.work.*
import com.example.workmanagerdemo.model.File
import com.example.workmanagerdemo.workmanager.FileDownloadWorker


fun String.shortToast(context: Context) {
    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
}

fun File.startDownloadingFile(): OneTimeWorkRequest {

    val data = Data.Builder()

    data.apply {
        putString(FileParams.KEY_FILE_NAME, name)
        putString(FileParams.KEY_FILE_URL, downloadLink)
        putString(FileParams.KEY_FILE_TYPE, fileType)
    }

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val fileDownloadWorker = OneTimeWorkRequestBuilder<FileDownloadWorker>()
        .setConstraints(constraints)
        .setInputData(data.build())
        .build()


    return fileDownloadWorker
}