package com.concordium.wallet.ui.common

import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.ErrorParser
import com.concordium.wallet.util.Log
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object BackendErrorHandler {

    fun getExceptionStringRes(e: Throwable): Int {
        when (e) {
            is StatusRuntimeException -> {
                // GRPC exception
                return getGrpcExceptionStringRes(e)
            }

            is UnknownHostException -> {
                // Default situation when no internet connection
                return R.string.app_error_backend_unknown_host_exception
            }

            is ConnectException -> {
                return R.string.app_error_backend_connect_exception
            }

            is SocketTimeoutException -> {
                return R.string.app_error_backend_unknown_sockettimeout_exception
            }

            is BackendErrorException -> {
                return getExceptionStringRes(e)
            }

            else -> {
                Log.e("Exception from backend communication", e)
                return R.string.app_error_backend_unknown
            }
        }
    }

    fun getExceptionStringRes(backendErrorException: BackendErrorException): Int {
        return when (backendErrorException.error.error) {
            0 -> {
                R.string.app_error_backend_internal_server
            }

            2 -> {
                R.string.app_error_backend_account_does_not_exist
            }

            else -> {
                Log.e(
                    "Exception from backend communication - unknown error code",
                    backendErrorException
                )
                R.string.app_error_backend_unknown_error_code
            }
        }
    }

    fun getExceptionStringResOrNull(backendError: BackendError): Int? {
        return when (backendError.error) {
            0 -> {
                R.string.app_error_backend_internal_server
            }

            else -> {
                null
            }
        }
    }

    fun getCoroutineBackendException(e: Exception): Exception? {
        if (e is CancellationException) {
            // When the coroutines are cancelled, there should not be shown an error
            return null
        }
        var ex = e
        if (e is HttpException) {
            val response = e.response()
            if (response != null) {
                val error = ErrorParser.parseError(response)
                if (error != null) {
                    ex = BackendErrorException(error)
                }
            }
        }
        return ex
    }

    private fun getGrpcExceptionStringRes(e: StatusRuntimeException): Int {
        return when (e.status.code) {
            Status.Code.UNAVAILABLE,
            Status.Code.DEADLINE_EXCEEDED,
            ->
                R.string.app_error_backend_unknown_host_exception

            else -> {
                Log.e("Exception from GRPC", e)
                R.string.app_error_backend_unknown
            }
        }
    }
}
