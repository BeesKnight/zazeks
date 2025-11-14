package com.example.minitasker.data.repository

import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeCall(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (ex: HttpException) {
        val message = ex.response()?.errorBody()?.string() ?: ex.message()
        NetworkResult.Error(message ?: "Server error")
    } catch (ex: IOException) {
        NetworkResult.Error("Network error: ${ex.message}")
    } catch (ex: Exception) {
        NetworkResult.Error(ex.message ?: "Unknown error")
    }
}
