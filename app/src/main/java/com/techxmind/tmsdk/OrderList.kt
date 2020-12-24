package com.techxmind.tmsdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.techxmind.el.Agent
import com.techxmind.el.PageInfo
import com.techxmind.el.Referer

class OrderList : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        Agent.setPageInfo(this, "order_list")

        findViewById<Button>(R.id.order1).setOnClickListener() {
            val intent = Intent(this, Order::class.java)
            intent.putExtra("order_id", 1)
            Agent.setReferer(intent, this, "order_id_btn")
            startActivity(intent)
        }
    }
}