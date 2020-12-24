package com.techxmind.tmsdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.techxmind.el.Agent
import com.techxmind.el.PageInfo
import com.techxmind.el.Referer

class Order : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        val orderId = intent.getIntExtra("order_id", 0)

        Agent.setPageInfo(this, "order", orderId.toString())

        findViewById<TextView>(R.id.orderid).text = "order id: $orderId"

        findViewById<Button>(R.id.goods).setOnClickListener() {
            val intent = Intent(this, Goods::class.java)
            Agent.setReferer(intent, this, "goods_btn")
            intent.putExtra("goods_id", orderId)
            startActivity(intent)
        }
    }
}