package com.example.galarpe_labact32.network

import com.example.galarpe_labact32.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Step 6: Add the Retrofit service for the OpenWeatherMap API,
 * https://api.openweathermap.org/data/2.5/weather
 */
interface OpenWeatherMapService {
    @GET("data/2.5/weather")
    fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
    ): Call<WeatherResponse>
}
