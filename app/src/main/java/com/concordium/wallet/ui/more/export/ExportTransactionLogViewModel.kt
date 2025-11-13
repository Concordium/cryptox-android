package com.concordium.wallet.ui.more.export

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.net.HttpURLConnection
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

private interface CcdScanRestApi {
    @Streaming
    @GET("export/statement")
    suspend fun getStatement(
        @Query("accountAddress")
        accountAddress: String,
    ): Response<ResponseBody>
}

sealed class FileDownloadScreenState {
    object Idle : FileDownloadScreenState()
    data class Downloading(val progress: Int) : FileDownloadScreenState()
}

sealed interface Event {
    object FinishWithSuccess : Event
    object FinishWithNoContent : Event
    class ShowError(val resId: Int) : Event
}

class ExportTransactionLogViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    private lateinit var api: CcdScanRestApi

    private sealed class DownloadState {
        data class Downloading(val progress: Int, val bytesProgress: Long, val bytesTotal: Long) :
            DownloadState()

        object Finished : DownloadState()
        data class Failed(val error: Throwable? = null) : DownloadState()
    }

    val downloadState = MutableLiveData<FileDownloadScreenState>()
    val events = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    private var downloadJob: Job? = null

    init {
        createRetrofitApi()
    }

    fun onIdleRequested() {
        downloadJob?.cancel()
        downloadState.value = FileDownloadScreenState.Idle
    }

    fun downloadFile(destinationFolder: Uri) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Emit 0 progress at the start, before making a connection.
                downloadState.postValue(
                    FileDownloadScreenState.Downloading(
                        progress = 0
                    )
                )

                val response = api.getStatement(
                    accountAddress = account.address,
                )

                val statusCode = response.code()
                if (statusCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    events.tryEmit(Event.FinishWithNoContent)
                    return@launch
                } else if (statusCode != HttpURLConnection.HTTP_OK || response.body() == null) {
                    events.tryEmit(Event.ShowError(R.string.export_transaction_log_failed))
                    this@ExportTransactionLogViewModel.downloadState.postValue(
                        FileDownloadScreenState.Idle
                    )
                    return@launch
                }

                response.body()!!.saveFile(destinationFolder)
                    .collect { downloadState ->
                        // Add visual delay and ensure the coroutine is active.
                        delay(300)

                        when (downloadState) {
                            is DownloadState.Downloading -> {
                                this@ExportTransactionLogViewModel.downloadState.postValue(
                                    FileDownloadScreenState.Downloading(
                                        progress = downloadState.progress,
                                    )
                                )
                            }

                            is DownloadState.Failed -> {
                                events.tryEmit(Event.ShowError(R.string.export_transaction_log_failed))
                                this@ExportTransactionLogViewModel.downloadState.postValue(
                                    FileDownloadScreenState.Idle
                                )
                            }

                            DownloadState.Finished -> {
                                events.tryEmit(Event.FinishWithSuccess)
                            }
                        }
                    }
            } catch (ex: Exception) {
                events.tryEmit(Event.ShowError(R.string.export_transaction_log_failed))
                this@ExportTransactionLogViewModel.downloadState.postValue(
                    FileDownloadScreenState.Idle
                )
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

    @Suppress("KotlinConstantConditions")
    private fun createRetrofitApi() {
        val baseUrl =
            if (BuildConfig.ENV_NAME == "production")
                "https://api-ccdscan.mainnet.concordium.software/rest/"
            else
                "https://api-ccdscan.testnet.concordium.com/rest/"

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
            .create(CcdScanRestApi::class.java)
    }

    fun getExplorerUrl(): String =
        BuildConfig.URL_EXPLORER_BASE
}
