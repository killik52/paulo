package com.example.myapplication.api

import com.example.myapplication.model.CnpjData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CnpjApiService {
    // 1. Define método para consultar dados de CNPJ via API
    @GET("cnpj/{cnpj}")
    fun getCnpjData(@Path("cnpj") cnpj: String): Call<CnpjData>
}