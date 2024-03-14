package dkv.thermo.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import dkv.thermo.MainModel
import dkv.thermo.R
import dkv.thermo.databinding.FragmentDevicesBinding

class DevicesFragment() : Fragment() {

    private var _binding: FragmentDevicesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val mainModel: MainModel by activityViewModels()

    private fun addDeviceFragments(root: View) {
        val tableLayout = root.findViewById<TableLayout>(R.id.devices_table)
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        mainModel.db.allDevices.forEach { device ->
            val tableRow = TableRow(context)
            tableRow.id = View.generateViewId()
            val tf = ThermoEditFragment(device)
            fragmentTransaction.add(tableRow.id, tf)
            tableLayout.addView(tableRow)
        }
        fragmentTransaction.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        addDeviceFragments(root)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
