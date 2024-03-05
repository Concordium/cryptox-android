package com.concordium.wallet.ui.more.export

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

interface FileDownloadApi {
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String?): Response<ResponseBody>
}

sealed class FileDownloadScreenState {
    object Idle : FileDownloadScreenState()
    data class Downloading(val progress: Int) : FileDownloadScreenState()
    object Failed : FileDownloadScreenState()
    object Downloaded : FileDownloadScreenState()
    object NoContent : FileDownloadScreenState()
}

class ExportTransactionLogViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    private lateinit var api: FileDownloadApi

    val HTTP_OK = 200
    val HTTP_NO_CONTENT = 204

    val textResourceInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    private sealed class DownloadState {
        data class Downloading(val progress: Int, val bytesProgress: Long, val bytesTotal: Long) :
            DownloadState()

        object Finished : DownloadState()
        data class Failed(val error: Throwable? = null) : DownloadState()
    }

    val downloadState: MutableLiveData<FileDownloadScreenState> by lazy { MutableLiveData<FileDownloadScreenState>() }
    private var downloadJob: Job? = null

    init {
        createRetrofitApi()
    }

    fun onIdleRequested() {
        downloadJob?.cancel()
        downloadState.value = FileDownloadScreenState.Idle
    }

    fun downloadFile(destinationFolder: Uri) {
        val downloadFile = "statement?accountAddress=${account.address}"
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Emit 0 progress at the start, before making a connection.
                downloadState.postValue(
                    FileDownloadScreenState.Downloading(
                        progress = 0
                    )
                )

                val response = api.downloadFile(downloadFile)
                val statusCode = response.code()
                if (statusCode == HTTP_NO_CONTENT) {
                    this@ExportTransactionLogViewModel.downloadState.postValue(FileDownloadScreenState.NoContent)
                    return@launch
                } else if (statusCode != HTTP_OK || response.body() == null) {
                    this@ExportTransactionLogViewModel.downloadState.postValue(FileDownloadScreenState.Failed)
                    return@launch
                }

                response.body()!!.saveFile(destinationFolder)
                    .collect { downloadState ->
                        // Add visual delay and ensure the coroutine is active.
                        delay(300)

                        this@ExportTransactionLogViewModel.downloadState.postValue(
                            when (downloadState) {
                                is DownloadState.Downloading -> {
                                    FileDownloadScreenState.Downloading(
                                        progress = downloadState.progress,
                                    )
                                }

                                is DownloadState.Failed -> {
                                    FileDownloadScreenState.Failed
                                }

                                DownloadState.Finished -> {
                                    FileDownloadScreenState.Downloaded
                                }
                            }
                        )
                    }
            } catch (ex: Exception) {
                FileDownloadScreenState.Failed
            }
        }
    }

    private fun ResponseBody.saveFile(destinationFolder: Uri): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0, 0, 0))
        val destinationFileName = "${account.address}.csv"
        val doc = DocumentFile.fromTreeUri(App.appContext, destinationFolder)
        val file = doc?.createFile("csv", destinationFileName)
        try {
            byteStream().use { inputStream ->
                App.appContext.contentResolver.openOutputStream(file!!.uri)
                    .use { outputStream ->
                        val totalBytes = contentLength()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream?.write(buffer, 0, bytes)
                            progressBytes += bytes
                            bytes = inputStream.read(buffer)
                            emit(
                                DownloadState.Downloading(
                                    ((progressBytes * 100) / totalBytes).toInt(),
                                    progressBytes,
                                    totalBytes
                                )
                            )
                        }
                    }
            }
            emit(DownloadState.Finished)
        } catch (e: CancellationException) {
            try {
                file?.delete()
            } catch (e: Exception) {
                Log.e("failed_deleting_file_on_cancellation", e)
            }
        } catch (e: Exception) {
            try {
                file?.delete()
            } catch (e: Exception) {
                Log.e("failed_deleting_file_on_failure", e)
            }
            emit(DownloadState.Failed(e))
        }
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()

    private fun createRetrofitApi() {
        @Suppress("KotlinConstantConditions")
        val baseUrl =
            if (BuildConfig.ENV_NAME == "production")
                "https://api-ccdscan.mainnet.concordium.software/rest/export/"
            else
                "https://api-ccdscan.testnet.concordium.com/rest/export/"

        val loggingInterceptor =
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
        api = Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .readTimeout(120L, TimeUnit.SECONDS)
                    .writeTimeout(120L, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .build()
            )
            .baseUrl(baseUrl)
            .build()
            .create(FileDownloadApi::class.java)
    }

    fun getExplorerUrl(): String =
        BuildConfig.URL_EXPLORER_BASE
}
