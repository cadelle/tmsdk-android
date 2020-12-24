package com.techxmind.tmsdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.techxmind.el.Agent
import com.techxmind.el.PageInfo
import com.techxmind.el.Referer

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置页面信息
        Agent.setPageInfo(this, "homepage" )

        findViewById<Button>(R.id.goodslist).setOnClickListener() {
            val intent = Intent(this, GoodsList::class.java)

            // 新开页面设置referer信息
            Agent.setReferer(intent, this, "goods_list_btn")

            startActivity(intent)
        }

        findViewById<Button>(R.id.orderlist).setOnClickListener() {
            var intent = Intent(this, OrderList::class.java)

            // 新开页面设置referer信息
            Agent.setReferer(intent, this, "order_list_btn")

            startActivity(intent)
        }
    }
}