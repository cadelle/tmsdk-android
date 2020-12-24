package com.techxmind.tmsdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.techxmind.el.Agent
import com.techxmind.el.PageInfo
import com.techxmind.el.Referer

class GoodsList : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goods_list)

        Agent.setPageInfo(this, "goods_list")

        findViewById<Button>(R.id.goods1).setOnClickListener() {
            val intent = Intent(this, Goods::class.java)
            intent.putExtra("goods_id", 1)
            Agent.setReferer(intent, this, "goods_id_btn")
            startActivity(intent)
        }

        findViewById<Button>(R.id.goods2).setOnClickListener() {
            val intent = Intent(this, Goods::class.java)
            intent.putExtra("goods_id", 2)
            Agent.setReferer(intent, this, "goods_id_btn")
            startActivity(intent)
        }
    }
}