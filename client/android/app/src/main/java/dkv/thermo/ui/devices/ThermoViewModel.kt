package dkv.thermo.ui.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.core.text.util.LocalePreferences
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ThermoViewModel : ViewModel() {
    private val tempUnit = LocalePreferences.getTemperatureUnit()
    private fun tempUnits() = when (tempUnit) {
        LocalePreferences.TemperatureUnit.CELSIUS -> "°C"
        LocalePreferences.TemperatureUnit.FAHRENHEIT -> "°F"
        LocalePreferences.TemperatureUnit.KELVIN -> "°K"
        else -> ""
    }

    private fun localTemp(tempC: Double) = when (tempUnit) {
        LocalePreferences.TemperatureUnit.CELSIUS -> tempC
        LocalePreferences.TemperatureUnit.FAHRENHEIT -> (tempC - 32) * 5 / 9;
        LocalePreferences.TemperatureUnit.KELVIN -> tempC + 283;
        else -> 0
    }

    private val _temp = MutableLiveData<String>().apply {
        value = "Temperature"
    }
    private val _humid = MutableLiveData<String>().apply {
        value = "Humidity"
    }

    // Temperature in Celsius
    private var tempC = 0.0
        set(t) {
            _temp.value = "%.2f %s".format(localTemp(t), tempUnits())
        }

    // relative humidity in [0 .. 1.0] range
    private var humidity = 0.0
        set(h) {
            _humid.value = "%.2f %%".format(humidity * 100)
        }

    private fun update() {
        tempC = Random.nextDouble(0.0, 40.0)
        humidity = Random.nextDouble(0.0, 100.0)
    }

    val location = MutableLiveData<String>().apply {
        value = "Room"
    }
    val tempLabel: LiveData<String> = _temp
    val humidLabel: LiveData<String> = _humid
    val visible = MutableLiveData<Boolean>().apply {
        value = false
    }

    private lateinit var updateJob: Job
    private var deviceId: Int = 0
        get() = deviceId

    fun launch(deviceId: Int) {
        this.deviceId = deviceId
        location.value = "Room $deviceId"
        val updateJob = viewModelScope.launch {
            while (true) {
                update()
                delay(5_000)
            }
        }
    }

    fun stop() {
        updateJob.cancel()
    }
}