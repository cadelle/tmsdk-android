package com.techxmind.tmsdk

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.techxmind.el.Agent
import com.techxmind.el.PageInfo
import com.techxmind.el.Referer

class Goods : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goods)

        val goodsId = intent.getIntExtra("goods_id", 0)

        Agent.setPageInfo(this, "goods_detail", goodsId.toString())

        val t = findViewById<TextView>(R.id.goodsid)
        t.text = "goods id : $goodsId"
        t.setOnClickListener() {
            Agent.event("custom_event", this, mutableMapOf(
                "module_id" to "text",
                "k1" to "v1",
                "k2" to "v2"
            )).submit()
        }

    }
}