package com.example.workmanagerdemo.presenter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.workmanagerdemo.model.File
import com.example.workmanagerdemo.utill.FileParams
import com.example.workmanagerdemo.utill.shortToast
import com.example.workmanagerdemo.utill.startDownloadingFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch


private suspend fun getFileLink(): File? {
    return HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                isLenient = false
                ignoreUnknownKeys = true
            })
        }
    }.get {
        url(
            scheme = "http",
            host = "rest-testing.epizy.com/index.php",
            path = "welcome/download"
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DownloadFileScreen() {

    val lifeCycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    )

    DisposableEffect(key1 = lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (Lifecycle.Event.ON_START == event) {
                permissionState.launchMultiplePermissionRequest()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DownloadScreen(permissionState)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DownloadScreen(permissionState: MultiplePermissionsState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val loading = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(.9f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 1.dp, color = Color.Green),
        elevation = 7.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            TextButton(onClick = {
                if (permissionState.allPermissionsGranted) {
                    scope.launch {
                        loading.value = false
                        val fileDownloadWorker = getFileLink()?.startDownloadingFile()

                        if(fileDownloadWorker != null) {
                            loading.value = true
                            val workManager = WorkManager.getInstance(context.applicationContext)
                            workManager.getWorkInfosForUniqueWorkLiveData("FILE_DOWNLOAD_WORKER")
                                .observe(context as MainActivity) {
                                    if (it.isNotEmpty()) {
                                        val fileDownload = it.find { it.id == fileDownloadWorker.id }
                                        fileDownload?.let {
                                            updateUI(fileDownload, context)
                                        }
                                    }
                                }

                            workManager.beginUniqueWork(
                                "FILE_DOWNLOAD_WORKER",
                                ExistingWorkPolicy.KEEP,
                                fileDownloadWorker
                            ).enqueue()
                        }
                    }
                }
            }) {
                Text(text = "Download", fontSize = 20.sp, color = Color.Blue)
                Spacer(modifier = Modifier.width(10.dp))
                if(loading.value) {
                    CircularProgressIndicator(color = Color.Red)
                }
            }

        }
    }
}


private fun updateUI(workInfo: WorkInfo, context: Context) {
    when (workInfo.state) {
        WorkInfo.State.RUNNING -> {
            "File Downloading...".shortToast(context)
        }
        WorkInfo.State.FAILED -> {
            "Download Failed".shortToast(context)
        }
        WorkInfo.State.SUCCEEDED -> {
            val data = workInfo.outputData.getString(FileParams.KEY_FILE_URI)
            if (data != null && data.isNotEmpty()) {
                try {

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(data.toUri(), "application/pdf")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    (context as MainActivity).startActivity(intent)

                } catch (ex: Exception) {
                    Log.d("OtherException", "${ex.message}")
                }
            }
        }
    }
}