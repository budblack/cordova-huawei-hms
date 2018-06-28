华为推送 HMS SDK，由于华为应用市场审核应用必须使用最新版的hms sdk，为方便区分，插件版本与HMS SDK版本号保持一致。

(注：原作者的插件一年前已停止更新，旧版插件无法通过华为应用市场审核，请使用本分支的插件)

`更新于2018年6月28号` 

# cordova-huawei-hms
华为推送cordova插件，目前仅适配安卓手机
## 安装
```shell
cordova plugin add https://github.com/waitaction/cordova-huawei-hms.git --variable APPID=YOURAPPID --variable  PACKAGENAME=YOURPACKAGENAME --variable cpid=YOURCPID
```
例如`cordova plugin add https://github.com/waitaction/cordova-huawei-hms.git  --variable APPID=10111111 --variable  PACKAGENAME=com.lifang123.www --variable cpid=350841385123`
## 怎么用

### 初始 hms 连接

```javascript
cordova.plugins.huaweipush.init();
```

### 获取设备token

```javascript
document.addEventListener('huaweipush.receiveRegisterResult', function (event) {
    console.log(event) // 设备token包含在event对像
}.bind(this), false);
```
你可以使用 `event.token` 获取到设备token

### 停止推送服务

```javascript
cordova.plugins.huaweipush.stop();
```

### 接收到推送通知时，点击通知栏打开应用后会触发下面的事件

```javascript
document.addEventListener('huaweipush.notificationOpened', function (event) {
    console.log(event) // event包含了你自定义的额外字段的数据
}.bind(this), false)
```

### 接收透传通知
```javascript
document.addEventListener('huaweipush.pushMsgReceived', function (event) {
    console.log(event) // event包含了你自定义的额外字段的数据
}.bind(this), false)
```

### 尽早定义事件
`document.addEventListener`事件要尽早定义，在调用`cordova.plugins.huaweipush.init()`之前.

```html
    <html>
    <head>...</head>
    <body>
    <script>
        document.addEventListener('huaweipush.receiveRegisterResult', function (event) {
            console.log(event) // 设备token包含在event对像
        }.bind(this), false);
    
        document.addEventListener('huaweipush.notificationOpened', function (event) {
            console.log(event) // event包含了你自定义的额外字段的数据
        }.bind(this), false)
    
        document.addEventListener('huaweipush.pushMsgReceived', function (event) {
            console.log(event) // event包含了你自定义的额外字段的数据
        }.bind(this), false)
    </script>
    <div>内容</div>
    </body>
    </html>
```