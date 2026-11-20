package com.example.galarpe_labact32.model

import com.squareup.moshi.Json

/**
 * Step 5: Define the model.
 * Classes that will contain the OpenWeatherMap server response.
 */
data class WeatherResponse(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "weather") val weather: List<WeatherDescription> = emptyList(),
)

data class WeatherDescription(
    @param:Json(name = "main") val main: String = "",
    @param:Json(name = "description") val description: String = "",
    @param:Json(name = "icon") val icon: String = "",
)
