package dkv.thermo.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dkv.thermo.db.MainModel
import dkv.thermo.R
import dkv.thermo.databinding.FragmentDevicesBinding
import dkv.thermo.db.Device

class DevicesFragment() : Fragment() {

    private var _binding: FragmentDevicesBinding? = null

    private val mainModel: MainModel by activityViewModels()

    private fun reconstructDeviceFragments(root: View, devicesMap: Map<String, Device>) {
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
}
