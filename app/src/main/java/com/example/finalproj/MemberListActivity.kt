package com.example.finalproj

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.data.AppDatabase
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MemberListActivity : AppCompatActivity() {

    private lateinit var adapter: MemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerMembers)
        val btnAddMember = findViewById<Button>(R.id.btnAddMember)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val db = AppDatabase.getDatabase(this)
        val memberDao = db.familyMemberDao()

        adapter = MemberAdapter(mutableListOf()) { member, position ->
            val removedMember = adapter.removeItem(position)

            val snackbar = Snackbar.make(
                recyclerView,
                "Member deleted",
                Snackbar.LENGTH_LONG
            )

            snackbar.setAction("UNDO") {
                adapter.restoreItem(removedMember, position)
            }

            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(snackbar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            memberDao.delete(member)
                        }
                    }
                }
            })

            snackbar.show()
        }

        recyclerView.adapter = adapter

        // Observe Room database for real-time updates
        lifecycleScope.launch {
            memberDao.getAllMembers().collectLatest { members ->
                adapter.updateData(members)
            }
        }

        btnAddMember.setOnClickListener {
            startActivity(Intent(this, AddMemberActivity::class.java))
        }
    }
}
