package com.example.galarpe_labact32

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.example.galarpe_labact32.databinding.ActivityMainBinding
import com.example.galarpe_labact32.model.WeatherResponse
import com.example.galarpe_labact32.network.OpenWeatherMapService
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val refreshWeather = Runnable { fetchWeather() }
    private var activeCall: Call<WeatherResponse>? = null
    private var hasWeather = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safeArea = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            binding.statusBarBackground.updateLayoutParams { height = safeArea.top }
            view.updatePadding(left = safeArea.left, right = safeArea.right, bottom = safeArea.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        binding.cityName.setText(R.string.city_name)
        if (BuildConfig.OPENWEATHERMAP_API_KEY.isBlank()) {
            binding.weatherMain.setText(R.string.default_weather_main)
            binding.weatherDescription.setText(R.string.default_weather_description)
            binding.weatherIcon.setImageResource(R.drawable.ic_sample_clouds)
            binding.weatherStatus.setText(R.string.weather_sample)
        } else {
            binding.weatherStatus.setText(R.string.weather_loading)
        }
    }

    override fun onStart() {
        super.onStart()
        if (BuildConfig.OPENWEATHERMAP_API_KEY.isNotBlank()) fetchWeather()
    }

    override fun onStop() {
        handler.removeCallbacks(refreshWeather)
        activeCall?.cancel()
        activeCall = null
        super.onStop()
    }

    private fun fetchWeather() {
        val call = weatherService.getWeather(
            latitude = 40.7128,
            longitude = -74.0060,
            apiKey = BuildConfig.OPENWEATHERMAP_API_KEY,
        )
        activeCall = call
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>,
            ) {
                if (call !== activeCall) return
                val weather = response.body()?.weather?.firstOrNull()
                when {
                    !response.isSuccessful -> showError(
                        when (response.code()) {
                            401, 403 -> getString(R.string.weather_auth_error)
                            404 -> getString(R.string.weather_city_error)
                            429 -> getString(R.string.weather_rate_error)
                            else -> getString(R.string.weather_server_error, response.code())
                        },
                    )
                    weather == null || weather.main.isBlank() || weather.description.isBlank() ->
                        showError(getString(R.string.weather_data_error))
                    else -> {
                        hasWeather = true
                        binding.weatherMain.text = weather.main
                        binding.weatherDescription.text = weather.description
                        binding.weatherStatus.isVisible = false
                        if (weather.icon.matches(ICON_CODE)) {
                            Glide.with(this@MainActivity)
                                .load("https://openweathermap.org/payload/api/media/file/${weather.icon}@2x.png")
                                .into(binding.weatherIcon)
                        } else {
                            Glide.with(this@MainActivity).clear(binding.weatherIcon)
                        }
                    }
                }
                scheduleRefresh()
            }

            override fun onFailure(call: Call<WeatherResponse>, error: Throwable) {
                if (call !== activeCall) return
                val message = if (error is IOException && error !is JsonEncodingException) {
                    R.string.weather_network_error
                } else {
                    R.string.weather_data_error
                }
                showError(getString(message))
                scheduleRefresh()
            }
        })
    }

    private fun showError(message: String) {
        binding.weatherStatus.text = if (hasWeather) {
            getString(R.string.weather_previous, message)
        } else {
            message
        }
        binding.weatherStatus.isVisible = true
    }

    private fun scheduleRefresh() {
        activeCall = null
        handler.postDelayed(refreshWeather, TimeUnit.MINUTES.toMillis(10))
    }

    companion object {
        private val ICON_CODE = Regex("[0-9]{2}[dn]")
        private val weatherService by lazy {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OpenWeatherMapService::class.java)
        }
    }
}
