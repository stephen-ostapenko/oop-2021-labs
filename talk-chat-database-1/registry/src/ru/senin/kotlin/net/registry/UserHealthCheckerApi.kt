package ru.senin.kotlin.net.registry

import retrofit2.Call
import retrofit2.http.GET

interface UserHealthCheckerApi {
    fun checkHealth(): Call<Map<String, String>>
}

interface HttpUserHealthCheckerApi : UserHealthCheckerApi {
    @GET("/v1/health")
    override fun checkHealth(): Call<Map<String, String>>
}