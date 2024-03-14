package dkv.thermo.ui.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.core.text.util.LocalePreferences
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dkv.thermo.db.Device
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ThermoModel(private val device: Device) : ViewModel() {
    companion object {
        fun create(device: Device): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return ThermoModel(device) as T
                }
            }
    }

    private val tempUnit = LocalePreferences.getTemperatureUnit()
    private fun tempUnits() = when (tempUnit) {
        LocalePreferences.TemperatureUnit.CELSIUS -> "°C"
        LocalePreferences.TemperatureUnit.FAHRENHEIT -> "°F"
        LocalePreferences.TemperatureUnit.KELVIN -> "°K"
        else -> ""
    }

    private fun localTemp(tempC: Double) = when (tempUnit) {
        LocalePreferences.TemperatureUnit.CELSIUS -> tempC
        LocalePreferences.TemperatureUnit.FAHRENHEIT -> tempC * 9 / 5 + 32;
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
            _temp.value = "%.2f%s".format(localTemp(t), tempUnits())
        }

    // relative humidity in [0 .. 1.0] range
    private var humidity = 0.0
        set(h) {
            _humid.value = "%.2f%%".format(h * 100)
        }

    private fun update() {
        tempC = device.tempC
        humidity = device.humid
    }

    val location = MutableLiveData<String>().apply {
        value = device.location
    }.apply { observeForever { updatedLocation -> device.location = updatedLocation } }
    val tempLabel: LiveData<String> = _temp
    val humidLabel: LiveData<String> = _humid
    val enabledCheckbox = MutableLiveData<Boolean>().apply {
        value = device.enabled
    }.apply { observeForever { enabled -> device.enabled = enabled } }

    private lateinit var updateJob: Job

    fun launchUpdater() {
        updateJob = viewModelScope.launch {
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
