package com.adrianosilva.githubexplorer.domain.util

/**
 * A generic result wrapper class.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T): Result<T>()
    data class Error<T>(val reason: ErrorReason): Result<T>()
}

sealed class ErrorReason {
    data object NoData: ErrorReason()
    data object NoConnection: ErrorReason()
    data class NetworkError(val message: String): ErrorReason()
    data class Unknown(val exception: Throwable): ErrorReason()
}