package dkv.thermo.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import dkv.thermo.db.MainModel
import dkv.thermo.R
import dkv.thermo.databinding.FragmentDevicesBinding
import dkv.thermo.db.Device

class DevicesFragment() : Fragment() {

    private var _binding: FragmentDevicesBinding? = null

    private val mainModel: MainModel by activityViewModels()

    private fun reconstructDeviceFragments(root: View, devicesMap: Map<String, Device>) {
        val progressBar = root.findViewById<ProgressBar>(R.id.devices_progress_bar)
        val tableLayout = root.findViewById<TableLayout>(R.id.devices_table)
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        tableLayout.removeAllViews()
        mainModel.db.devices.values.forEach { device ->
            val tableRow = TableRow(context)
            tableRow.id = View.generateViewId()
            val tf = ThermoEditFragment(device)
            fragmentTransaction.add(tableRow.id, tf)
            tableLayout.addView(tableRow)
        }
        fragmentTransaction.commit()
        progressBar.visibility = View.GONE
        tableLayout.visibility = View.VISIBLE
    }

    private fun refreshDevicesView(devicesMap: Map<String, Device>) {
        val b = _binding
        if (b != null) {
            reconstructDeviceFragments(b.root, devicesMap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDevicesBinding.inflate(inflater, container, false)
        _binding = binding
        mainModel.db.liveDevices.observe(viewLifecycleOwner) { devicesMap ->
            // may invoke right away if scanning is already done
            refreshDevicesView(devicesMap)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        activity?.let {
            val root = _binding?.root
            if (root != null) {
                val progressBar = root.findViewById<ProgressBar>(R.id.devices_progress_bar)
                val tableLayout = root.findViewById<TableLayout>(R.id.devices_table)
                progressBar.visibility = View.VISIBLE
                tableLayout.visibility = View.GONE
                mainModel.db.checkPermissionsAndStartScanning(
                    it,
                    mainModel.viewModelScope
                )
            }
        }
    }
}
