cordova.define("cordova-huawei-push.CordovaHuaweiPush", function(require, exports, module) {
var exec = require('cordova/exec');

var HuaweiPush = function () { }
HuaweiPush.prototype.isAndroidDevice = function () {
    return device.platform == 'Android';
}
// 获取到token
HuaweiPush.prototype.tokenRegistered = function (token) {
    try {
        this.receiveRegisterResult = token;
        cordova.fireDocumentEvent('huaweipush.receiveRegisterResult', this.receiveRegisterResult);
    } catch (exception) {
        console.log('HuaweiPush:tokenRegistered ' + exception);
    }
}
// 透传消息
HuaweiPush.prototype.pushMsgReceived = function (msg) {
    try {
        msg.extras = JSON.parse(msg.extras);
        this.receiveRegisterResult = msg;
        cordova.fireDocumentEvent('huaweipush.pushMsgReceived', this.receiveRegisterResult);
    } catch (exception) {
        console.log('HuaweiPush:pushMsgReceived ' + exception);
    }
}
//通知消息
HuaweiPush.prototype.notificationOpened = function (msg) {
    try {
        console.log(msg);
        console.log(msg.extras);
        msg.extras = JSON.parse(msg.extras);
        this.receiveRegisterResult = msg;
        cordova.fireDocumentEvent('huaweipush.notificationOpened', this.receiveRegisterResult);
    } catch (exception) {
        console.log('HuaweiPush:notificationOpened ' + exception);
    }
}
//初始
HuaweiPush.prototype.init = function (success, error) {
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "init", []);
    }
};
//停止hms服务
HuaweiPush.prototype.stop = function (success, error) {
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "stop", []);
    }
};

//连接到hms服务
HuaweiPush.prototype.connect = function (success, error) {
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "connect", []);
    }
}

//是否已连接
HuaweiPush.prototype.isConnected = function (success, error) {
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "isConnected", []);

    }
}

//付款 productName:商品名 productDesc:商品描述 amount:金额 requestId:订单号 merchantName:商户名称 extReserved:商户保留信息,回调给商户服务端
HuaweiPush.prototype.pay = function (payReq,success, error) {
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "pay", [payReq]);
    }
}
//输出调试日志
HuaweiPush.prototype.log = function (logStr) {
    if (this.isAndroidDevice()) {
        try {
            cordova.fireDocumentEvent('huaweipush.log', logStr);
        } catch (exception) {
            console.log('HuaweiPush:tokenRegistered ' + exception);
        }
    }
}
//配置 userId  appId rsaKeyPrivate rsaKeyPublic
HuaweiPush.prototype.config = function (configInfo,success, error) {
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "config", [configInfo]);
    }
}

HuaweiPush.prototype.getSignData = function(payReq,success,error){
    if (this.isAndroidDevice()) {
        exec(success, error, "CordovaHuaweiPush", "getSignData", [payReq]);
    }

}


module.exports = new HuaweiPush();

});
