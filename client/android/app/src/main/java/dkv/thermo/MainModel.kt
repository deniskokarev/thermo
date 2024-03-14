package dkv.thermo

import androidx.lifecycle.ViewModel
import dkv.thermo.db.DevicesDb

class MainModel : ViewModel() {
    val db = DevicesDb()
}
