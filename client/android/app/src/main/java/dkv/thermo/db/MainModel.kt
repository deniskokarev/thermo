package dkv.thermo.db

import androidx.lifecycle.ViewModel

class MainModel : ViewModel() {
    val db: IDevicesDb = InMemoryDevicesDb()
}
