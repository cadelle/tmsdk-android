# 埋点SDK初版
未经过严格测试及优化，仅作为设计参考

自动收集应用/页面的启动，退出及用时，如设置好页面信息，所有PV的Referer信息自动串连。

## 初始化
```kotlin
import com.techxmind.el.Agent
import com.techxmind.el.Env
import com.techxmind.el.Configure

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // 埋点配置，更多配置见Configure文件
        with(Configure) {
            // 设置打印调试日志
            setDebug(true)

            // 手动指定log server地址
            // 默认为https://l.benshar.cn/mul
            logserver = "https://l.techxmind.com/mul"

            // APP名称标识，必须设置
            appType = "tianqi"

            // 指定用户ID，可在后期用户登录获取到时，重新指定 Configure.mid = "xx"
            mid = "5555"

            // 应用的安装渠道
            appChannel = "txm"

            // 环境，默认为PRODUCT
            env = Env.DEVELOPMENT

            // oaid，因为SDK还未集成oaid获取SDK，需要应用自己设置下
            oaid = "xxx"
        }

        Agent.init(this)
    }
}
```


## 页面信息设置

方便自动采集时获取更有意义的页面名称及获取新开页面的来源信息

```kotlin
import com.techxmind.el.Agent

// 在每一个Activity内设置page信息
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置页面信息
        Agent.setPageInfo(
            this,
            "homepage" // pageId
        )

        findViewById<Button>(R.id.goodslist).setOnClickListener() {
            val intent = Intent(this, GoodsList::class.java)

            // 新开页面设置referer信息
            Agent.setReferer(intent, this, /* ref module id */ "goods_list_btn")

            startActivity(intent)
        }
    }
}
```

## 自定义事件及手动提交
```
something.setOnClickListener() {
    Agent.event("custom_event", this, mutableMapOf(
        "module_id" to "text",
        "k1" to "v1",
        "k2" to "v2"
    )).submit()
}
```


## DEMO应用采集数据解释
```js
// 应用启动即时上报
{
  "common": {
    "udid": "VjpXbpPOhsQ",
    "tkid": "Af94lFY6V26TzobE",
    "mid": "5555",
    "platform": "android",
    "appVersion": "1.0",
    "appChannel": "txm",
    "appType": "default",
    "os": "android",
    "osVersion": "11",
    "deviceModel": "sdk_gphone_x86_arm",
    "deviceVendor": "Google",
    "deviceBrand": "google",
    "androidId": "9c0a4c9d469afd70",
    "network": 4
  },
  "events": [{
    "eventId": "e:XElZGxEELM:001",
    "eventTime": "1608803352698",
    "sessionId": "s:XElZGxEELM",
    "event": "AS"
  }]
}

// 手动submit导致的批次上报
{
  "common": {
    "udid": "VjpXbpPOhsQ",
    "tkid": "Af94lFY6V26TzobE",
    "mid": "5555",
    "platform": "android",
    "appVersion": "1.0",
    "appChannel": "txm",
    "appType": "default",
    "os": "android",
    "osVersion": "11",
    "deviceModel": "sdk_gphone_x86_arm",
    "deviceVendor": "Google",
    "deviceBrand": "google",
    "screenSize": "5.6",
    "screenWidth": 1080,
    "screenHeight": 2220,
    "screenResolution": "1080x2220",
    "androidId": "9c0a4c9d469afd70",
    "network": 4
  },
  "events": [{ // 首页PV
    "eventId": "e:XElZGxEELM:002",
    "eventTime": "1608803352964",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "homepage",
    "pvId": "p:XElZGxEELM:001"
  }, { // 商品列表 PV，来源首页的goods_list_btn模块
    "eventId": "e:XElZGxEELM:003",
    "eventTime": "1608803356097",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "goods_list",
    "pvId": "p:XElZGxEELM:002",
    "refPageId": "homepage",
    "refPvId": "p:XElZGxEELM:001",
    "refModuleId": "goods_list_btn"
  }, { // 商品1 PV，来源商品列表页的goods_id_btn模块
    "eventId": "e:XElZGxEELM:004",
    "eventTime": "1608803358039",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:003",
    "pageKey": "1",
    "refPageId": "goods_list",
    "refPvId": "p:XElZGxEELM:002",
    "refModuleId": "goods_id_btn"
  }, { // 离开商品1 页面
    "eventId": "e:XElZGxEELM:005",
    "eventTime": "1608803359990",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:003",
    "pageKey": "1",
    "refPageId": "goods_list",
    "refPvId": "p:XElZGxEELM:002",
    "refModuleId": "goods_id_btn",
    "duration": "1459"
  }, { // 商品2 PV，来源商品列表页的goods_id_btn模块
    "eventId": "e:XElZGxEELM:006",
    "eventTime": "1608803361112",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:004",
    "pageKey": "2",
    "refPageId": "goods_list",
    "refPvId": "p:XElZGxEELM:002",
    "refModuleId": "goods_id_btn"
  }, { // 商品2 页面发生的自定义事件
    "eventId": "e:XElZGxEELM:007",
    "eventTime": "1608803362188",
    "sessionId": "s:XElZGxEELM",
    "event": "custom_event",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:004",
    "pageKey": "2",
    "refPageId": "goods_list",
    "refPvId": "p:XElZGxEELM:002",
    "refModuleId": "goods_id_btn",
    "extendInfo": {
      "module_id": "text",
      "k1": "v1",
      "k2": "v2"
    }
  }]
}

// App退出导致的批次上报
{
  "common": {
    "udid": "VjpXbpPOhsQ",
    "tkid": "Af94lFY6V26TzobE",
    "mid": "5555",
    "platform": "android",
    "appVersion": "1.0",
    "appChannel": "txm",
    "appType": "default",
    "os": "android",
    "osVersion": "11",
    "deviceModel": "sdk_gphone_x86_arm",
    "deviceVendor": "Google",
    "deviceBrand": "google",
    "screenSize": "5.6",
    "screenWidth": 1080,
    "screenHeight": 2220,
    "screenResolution": "1080x2220",
    "androidId": "9c0a4c9d469afd70",
    "network": 4
  },
  "events": [{ // 离开商品2页
    "eventId": "e:XElZGxEELM:008",
    "eventTime": "1608803364342",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:004",
    "pageKey": "2",
    "refPageId": "goods_list",
    "refPvId": "p:XElZGxEELM:002",
    "refModuleId": "goods_id_btn",
    "duration": "2738"
  }, { // 离开商品列表页
    "eventId": "e:XElZGxEELM:009",
    "eventTime": "1608803365496",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "goods_list",
    "pvId": "p:XElZGxEELM:002",
    "refPageId": "homepage",
    "refPvId": "p:XElZGxEELM:001",
    "refModuleId": "goods_list_btn",
    "duration": "4592"
  }, { // 订单列表页 PV，来源首页的order_list_btn模块
    "eventId": "e:XElZGxEELM:00a",
    "eventTime": "1608803366115",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "order_list",
    "pvId": "p:XElZGxEELM:005",
    "refPageId": "homepage",
    "refPvId": "p:XElZGxEELM:001",
    "refModuleId": "order_list_btn"
  }, { // 订单1页 PV，来源订单列表页的order_id_btn模块
    "eventId": "e:XElZGxEELM:00b",
    "eventTime": "1608803367473",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "order",
    "pvId": "p:XElZGxEELM:006",
    "pageKey": "1",
    "refPageId": "order_list",
    "refPvId": "p:XElZGxEELM:005",
    "refModuleId": "order_id_btn"
  }, { // 商品1 PV，来源于订单1的goods_btn模块
    "eventId": "e:XElZGxEELM:00c",
    "eventTime": "1608803369159",
    "sessionId": "s:XElZGxEELM",
    "event": "PV",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:007",
    "pageKey": "1",
    "refPageId": "order",
    "refPvId": "p:XElZGxEELM:006",
    "refPageKey": "1",
    "refModuleId": "goods_btn"
  }, { // 离开商品1页
    "eventId": "e:XElZGxEELM:00d",
    "eventTime": "1608803372074",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "goods_detail",
    "pvId": "p:XElZGxEELM:007",
    "pageKey": "1",
    "refPageId": "order",
    "refPvId": "p:XElZGxEELM:006",
    "refPageKey": "1",
    "refModuleId": "goods_btn",
    "duration": "2421"
  }, { // 离开订单1页
    "eventId": "e:XElZGxEELM:00e",
    "eventTime": "1608803373091",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "order",
    "pvId": "p:XElZGxEELM:006",
    "pageKey": "1",
    "refPageId": "order_list",
    "refPvId": "p:XElZGxEELM:005",
    "refModuleId": "order_id_btn",
    "duration": "2664"
  }, { // 离开订单列表页
    "eventId": "e:XElZGxEELM:00f",
    "eventTime": "1608803373822",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "order_list",
    "pvId": "p:XElZGxEELM:005",
    "refPageId": "homepage",
    "refPvId": "p:XElZGxEELM:001",
    "refModuleId": "order_list_btn",
    "duration": "2055"
  }, { 离开首页
    "eventId": "e:XElZGxEELM:010",
    "eventTime": "1608803374891",
    "sessionId": "s:XElZGxEELM",
    "event": "PD",
    "pageId": "homepage",
    "pvId": "p:XElZGxEELM:001",
    "duration": "4958"
  }, { // 应用退出
    "eventId": "e:XElZGxEELM:011",
    "eventTime": "1608803374891",
    "sessionId": "s:XElZGxEELM",
    "event": "AQ",
    "pvId": "p:XElZGxEELM:001",
    "duration": "22200"
  }]
}
```
