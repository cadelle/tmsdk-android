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
