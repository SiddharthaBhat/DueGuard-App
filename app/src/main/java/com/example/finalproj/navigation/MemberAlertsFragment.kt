package com.example.finalproj.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.security.AlertsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemberAlertsFragment : Fragment() {

    private var memberId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            memberId = it.getInt("memberId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAlerts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadLogs(recyclerView)
    }

    private fun loadLogs(recyclerView: RecyclerView) {

        CoroutineScope(Dispatchers.IO).launch {

            val logs = AppDatabase.getDatabase(requireContext())
                .intruderLogDao()
                .getAllLogs()

            withContext(Dispatchers.Main) {
                recyclerView.adapter = AlertsAdapter(logs.toMutableList())
            }
        }
    }

    companion object {
        fun newInstance(memberId: Int): MemberAlertsFragment {
            val fragment = MemberAlertsFragment()
            val bundle = Bundle()
            bundle.putInt("memberId", memberId)
            fragment.arguments = bundle
            return fragment
        }
    }
}