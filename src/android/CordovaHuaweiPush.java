package com.iflamed.huaweipush;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiAvailability.OnUpdateListener;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.support.api.client.Status;
import com.huawei.hms.support.api.entity.pay.HwPayConstant;
import com.huawei.hms.support.api.entity.pay.PayReq;
import com.huawei.hms.support.api.entity.pay.PayStatusCodes;
import com.huawei.hms.support.api.pay.HuaweiPay;
import com.huawei.hms.support.api.pay.PayResult;
import com.huawei.hms.support.api.push.HuaweiPush;
import com.huawei.hms.support.api.push.TokenResult;
import com.huawei.hms.support.api.client.PendingResult;
import com.huawei.hms.support.api.client.ResultCallback;
import com.huawei.hms.support.api.entity.push.TokenResp;

import android.app.ProgressDialog;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.Thread;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaHuaweiPush extends CordovaPlugin implements HuaweiApiClient.ConnectionCallbacks,
        HuaweiApiClient.OnConnectionFailedListener,
        OnUpdateListener {
    public static HuaweiApiClient huaweiApiClient;
    // 接收Push消息
    public static final int RECEIVE_PUSH_MSG = 0x100;

    // 接收Push Token消息
    public static final int RECEIVE_TOKEN_MSG = 0x101;

    // 接收Push 自定义通知消息内容
    public static final int RECEIVE_NOTIFY_CLICK_MSG = 0x102;

    public static final int RECEIVE_TAG_MSG = 0x103;

    public static final int RECEIVE_STATUS_MSG = 0x104;

    public static final int OTHER_MSG = 0x105;

    public static final String NORMAL_MSG_ENABLE = "normal_msg_enable";

    public static final String NOTIFY_MSG_ENABLE = "notify_msg_enable";

    public static String TAG = "HuaweiPushPlugin";
    public static String token = "";
    public static int openNotificationId = 0;
    public static String openNotificationExtras;

    private static CordovaHuaweiPush instance;
    private static Activity activity;
    private CallbackContext initCallback;


    private static String userId;
    private static String appId;
    private static String rsaKeyPrivate;
    private static String rsaKeyPublic;

    private final int REQ_CODE_PAY = 4001;

    //private Lock lock = new ReentrantLock();// 锁对象

    public CordovaHuaweiPush() {
        instance = this;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        activity = cordova.getActivity();
        //如果是首次启动，并且点击的通知消息，则处理消息
        if (openNotificationExtras != null || openNotificationExtras != "") {
            notificationOpened(openNotificationId, openNotificationExtras);
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("init")) {
            this.init(callbackContext);
            return true;
        }
        //连接到hms
        if (action.equals("connect")) {
            this.init(callbackContext);
            return true;
        }

        //调用支付
        if (action.equals("pay")) {
            JSONObject jsPayReq = args.getJSONObject(0);
            PayReq aPayReq = new PayReq();
            aPayReq.amount = jsPayReq.getString("amount");
            aPayReq.productDesc = jsPayReq.getString("productDesc");
            aPayReq.requestId = jsPayReq.getString("requestId");
            aPayReq.merchantName = jsPayReq.getString("merchantName");
            aPayReq.extReserved = jsPayReq.getString("extReserved");
            aPayReq.productName = jsPayReq.getString("productName");
            this.log("付款金额:" + aPayReq.amount);
            this.log("商品名称:" + aPayReq.productName);
            this.log("商品描述:" + aPayReq.productDesc);
            this.log("商户名:" + aPayReq.merchantName);
            this.log("订单号:" + aPayReq.requestId);
            this.log("商户备注:" + aPayReq.extReserved);
            this.log("开始付款操作");
            this.pay(callbackContext, aPayReq);
            return true;
        }

        //是否已连接到hms服务
        if (action.equals("isConnected")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", huaweiApiClient.isConnected());
            callbackContext.success(jsonObject);
            return true;
        }
        //配置hms
        if (action.equals("config")) {
            JSONObject configInfo = args.getJSONObject(0);
            userId = configInfo.getString("userId");
            appId = configInfo.getString("appId");
            rsaKeyPrivate = configInfo.getString("rsaKeyPrivate");
            rsaKeyPublic = configInfo.getString("rsaKeyPublic");
            this.log("基础配置hms");
            return true;
        }

        if (action.equals("stop")) {
            this.delToken(callbackContext);
        }
        return false;
    }


    private void log(String msg) {
        if (instance == null) {
            return;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("log", msg);
            String format = "window.cordova.plugins.huaweipush.log(%s);";

            final String js = String.format(format, object.toString());

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instance.webView.loadUrl("javascript:" + js);
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        } finally {

        }
    }


    private void init(CallbackContext callbackContext) {
        this.log("初始化hms连接");
        if (huaweiApiClient != null && huaweiApiClient.isConnected()) {

        } else {
            this.log("正在连接hms");
            huaweiApiClient = new HuaweiApiClient.Builder(this.cordova.getActivity())
                    .addApi(HuaweiPush.PUSH_API)
                    .addApi(HuaweiPay.PAY_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            huaweiApiClient.connect();
        }

        this.initCallback = callbackContext;
    }

    public void pay(CallbackContext callbackContext, PayReq aPayReq) {

        if (!huaweiApiClient.isConnected()) {
            // TODO:支付接口必须在连接成功以后调用
            this.log("未连接到hms服务");
            return;
        }
        this.log("已连接到hms服务");
        String productName = aPayReq.productName;
        String productDesc = aPayReq.productDesc;
        //DateFormat format = new java.text.SimpleDateFormat("yyyyMMddhhmmssSSS");
        String requestId = aPayReq.requestId;

        // 将需要参与签名的字段 存储。
        // 注意参与签名的字段必须同时也要上传给交易服务器，否则签名会匹配失败
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(HwPayConstant.KEY_MERCHANTID, userId);
        params.put(HwPayConstant.KEY_APPLICATIONID, appId);
        params.put(HwPayConstant.KEY_AMOUNT, aPayReq.amount);
        params.put(HwPayConstant.KEY_PRODUCTNAME, productName);
        params.put(HwPayConstant.KEY_PRODUCTDESC, productDesc);
        params.put(HwPayConstant.KEY_REQUESTID, requestId);
        params.put(HwPayConstant.KEY_SDKCHANNEL, 1);
        params.put(HwPayConstant.KEY_URLVER, "2");
        // params.put(HwPayConstant.KEY_URL, "http://domain.com/path");
        // params.put(HwPayConstant.KEY_COUNTRY, "CN");
        // params.put(HwPayConstant.KEY_CURRENCY, "CNY");

        // it should be signed with RSA algorithm in Customer Server
        // 强烈建议在 商户服务端做签名处理，且私钥存储在服务端
        this.log("计算签名");
        String noSign = CipherUtil.getSignData(params);
        String sign = CipherUtil.rsaSign(noSign, rsaKeyPrivate);
        this.log("计算前:" + noSign);
        this.log("计算后:" + sign);
        Log.i("rsa sign", "pre noSign: " + noSign + "  sign: " + sign);

        final PayReq payReq = new PayReq();
        // 商品名称 必填。 此名称将会在支付时显示给用户确认 注意：该字段中不能包含特殊字符，包括# " & / ? $ ^ *:) \ < > ,
        // |
        payReq.productName = (String) params.get(HwPayConstant.KEY_PRODUCTNAME);

        // 商品描述 必填。注意：该字段中不能包含特殊字符，包括# " & / ? $ ^ *:) \ < > , |
        payReq.productDesc = (String) params.get(HwPayConstant.KEY_PRODUCTDESC);

        // 商户ID 必填。 由华为开发者联盟分配
        payReq.merchantId = userId;// (String)
        // params.get(HwPayConstant.KEY_MERCHANTID);

        // 应用ID 必填 由华为开发者联盟分配
        payReq.applicationID = (String) params.get(HwPayConstant.KEY_APPLICATIONID);

        // 待支付金额 必填 。格式为：元.角分，最小金额为分， 例如：20.00，此金额将会在支付时显示给用户确认)，保留到小数点后两位
        payReq.amount = (String) params.get(HwPayConstant.KEY_AMOUNT);

        // 请求订单号 必填。其值由商户定义生成，用于标识一次支付请求，每次请求需唯一，不可重复。
        // 支付平台在服务器回调接口中会原样返回requestId的值。
        // 注意：该字段中不能包含特殊字符，包括# " & / ? $ ^ *:) \ < > , |
        payReq.requestId = (String) params.get(HwPayConstant.KEY_REQUESTID);

        // 支付结果回调URL 选填， 华为服务器收到后检查该应用有无在开发者联盟配置回调URL，如果配置了则使用应用配置的URL，否则使用此url
        // 作为该次支付的回调URL
        // 建议直接 以配置在 华为开发者联盟的回调URL为准
        // payReq.url = (String) params.get(HwPayConstant.KEY_URL);
        //
        // // 渠道信息，选填。 取值如下：0 代表自有应用，无渠道 1 代表智汇云渠道 2 代表预装渠道 3 代表游戏吧
        payReq.sdkChannel = (Integer) params.get(HwPayConstant.KEY_SDKCHANNEL);

        // 回调接口版本号， 选填。 建议传值2， 额外回调信息，具体参考接口文档
        payReq.urlVer = (String) params.get(HwPayConstant.KEY_URLVER);

        // // 国家码 选填。建议无特殊需要，不传
        // payReq.country = (String) params.get(HwPayConstant.KEY_COUNTRY);
        // // 币种 选填。建议无特殊需要不传此参数。目前仅支持CNY，默认CNY
        // payReq.currency = (String) params.get(HwPayConstant.KEY_CURRENCY);

        /* 以上字段皆需要参与签名 * */

        // 签名字段 必填。采用华为开发者联盟分配的私钥进行签名，强烈建议在服务端进行签名 .
        // 【注意】以下参数不参与签名：
        // a）sign参数
        // b)参数说明中标识不参与签名的参数
        // c)没有值的参数，包括null和“”两种情况
        // 2、排序完成之后，再把所有参数名和参数值的键值对以“&”字符连接起来，得到的字符串即为待签名串。如：
        // amount=XXX&applicationID=XXX&country=XXX&currency=XXX&merchantId=XXX&productDesc=XXX&productName=XXX&requestId=XXX&sdkChannel=XXX&url=XXX&urlver=XXX
        // 3、将待签名字符串使用RSA私钥进行签名，采用 SHA256WithRSA签名算法 得到的字符串即为sign参数的值
        payReq.sign = sign;

        // 商户名称，必填，不参与签名。会显示在支付结果页面
        payReq.merchantName = aPayReq.merchantName;

        // 分类，选填，不参与签名。该字段会影响风控策略
        // X4：主题
        // X5：应用商店
        // X6：游戏
        // X7：天际通
        // X8：云空间
        // X9：电子书
        // X10：华为学习
        // X11：音乐
        // X12 视频
        // X31 话费充值
        // X32 机票/酒店
        // X33 电影票
        // X34 团购
        // X35 手机预购
        // X36 公共缴费
        // X39 流量充值
        payReq.serviceCatalog = "1";

        // 商户保留信息，选填不参与签名，支付成功后会华为支付平台会原样 回调CP服务端
        payReq.extReserved = aPayReq.extReserved;
        this.log("开始支付");
        showProgress("Authing, please wait...");
        PendingResult<PayResult> payResultPending = HuaweiPay.HuaweiPayApi.pay(huaweiApiClient, payReq);
        this.log("设置调用支付回调");

        payResultPending.setResultCallback(new ResultCallback<PayResult>() {
            @Override
            public void onResult(PayResult payResult) {
                instance.log("已运行调用支付回调方法");
                cancleProgress();
                Status status = payResult.getStatus();
                if (PayStatusCodes.PAY_STATE_SUCCESS == status.getStatusCode()) {
                    instance.log("调用支付界面成功(此时还未支付)");
                    try {
                        status.startResolutionForResult(activity, REQ_CODE_PAY);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("hwpay", "SendIntentException e");
                    }
                } else {
                    // TODO 根据返回码处理错误信息
                    instance.log("支付失败:" + status.getStatusCode() + "，错误描述："  + status.getStatusMessage());

                }

            }
        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        huaweiApiClient = null;
    }

    public void onConnectionFailed(ConnectionResult result) {
    }

    public void onConnected() {
        this.getToken();
    }

    @Override
    public void onUpdateFailed(ConnectionResult result) {
    }


    private void getToken() {
        if (!huaweiApiClient.isConnected()) {
            initCallback.error("{status:\"failed\"}");
            return;
        }
        // 异步调用方式
        try {
            // 异步调用方式
            PendingResult<TokenResult> tokenResult = HuaweiPush.HuaweiPushApi.getToken(huaweiApiClient);
            tokenResult.setResultCallback(new ResultCallback<TokenResult>() {

                @Override
                public void onResult(TokenResult result) {
                }

            });
            initCallback.success("{status:\"success\"}");
        } catch (Exception e) {
            initCallback.error("{status:\"failed\"}");
            Log.e(TAG, e.toString(), e);
        }
    }

    public void onConnectionSuspended(int cause) {
    }

    public static void onTokenRegistered(String regId) {
        Log.e(TAG, "-------------onTokenRegistered------------------" + regId);
        if (instance == null) {
            return;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("token", regId);
            String format = "window.cordova.plugins.huaweipush.tokenRegistered(%s);";
            final String js = String.format(format, object.toString());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instance.webView.loadUrl("javascript:" + js);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void delToken(final CallbackContext callbackContext) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (token != "" && null != huaweiApiClient) {
                        HuaweiPush.HuaweiPushApi.deleteToken(huaweiApiClient, token);
                        callbackContext.success();
                    } else {
                        Log.w(TAG, "delete token's params is invalid.");
                        callbackContext.error("token not exists");
                    }
                } catch (Exception e) {
                    callbackContext.error("error occered when delete token");
                    Log.e("PushLog", "delete token exception, " + e.toString());
                }
            }
        }.start();
    }

    public static void pushMsgReceived(String msg) {
        Log.e(TAG, "-------------onTokenRegistered------------------" + msg);
        if (instance == null) {
            return;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("extras", msg);
            String format = "window.cordova.plugins.huaweipush.pushMsgReceived(%s);";
            final String js = String.format(format, object.toString());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instance.webView.loadUrl("javascript:" + js);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void notificationOpened(int notifyId, String msg) {
        CordovaHuaweiPush.openNotificationId = notifyId;
        CordovaHuaweiPush.openNotificationExtras = msg;
        Log.e(TAG, "-------------onTokenRegistered------------------" + msg);
        if (instance == null) {
            return;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("extras", msg);
            String format = "window.cordova.plugins.huaweipush.notificationOpened(%s);";
            final String js = String.format(format, object.toString());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instance.webView.loadUrl("javascript:" + js);
                    CordovaHuaweiPush.openNotificationId = 0;
                    CordovaHuaweiPush.openNotificationExtras = "";
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public static class CipherUtil {
        private static final String SIGN_ALGORITHMS = "SHA256WithRSA";

        public static String rsaSign(String content, String privateKey) {
            if (null == content || null == privateKey) {
                return null;
            }
            String charset = "UTF-8";
            try {
                PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT));
                KeyFactory keyf = KeyFactory.getInstance("RSA");
                PrivateKey priKey = keyf.generatePrivate(priPKCS8);
                java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
                signature.initSign(priKey);
                signature.update(content.getBytes(charset));
                byte[] signed = signature.sign();
                return Base64.encodeToString(signed, Base64.DEFAULT);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "sign NoSuchAlgorithmException");
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "sign InvalidKeySpecException");
            } catch (InvalidKeyException e) {
                Log.e(TAG, "sign InvalidKeyException");
            } catch (SignatureException e) {
                Log.e(TAG, "sign SignatureException");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "sign UnsupportedEncodingException");
            }
            return null;
        }

        public static boolean doCheck(String content, String sign, String publicKey) {
            if (TextUtils.isEmpty(publicKey)) {
                Log.e(TAG, "publicKey is null");
                return false;
            }

            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] encodedKey = Base64.decode(publicKey, Base64.DEFAULT);
                PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

                java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

                signature.initVerify(pubKey);
                signature.update(content.getBytes("utf-8"));

                boolean bverify = signature.verify(Base64.decode(sign, Base64.DEFAULT));
                return bverify;

            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "doCheck NoSuchAlgorithmException" + e);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "doCheck InvalidKeySpecException" + e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "doCheck InvalidKeyException" + e);
            } catch (SignatureException e) {
                Log.e(TAG, "doCheck SignatureException" + e);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "doCheck UnsupportedEncodingException" + e);
            }
            return false;
        }

        /**
         * 对数组里的每一个值从a到z的顺序排序
         *
         * @param Map <String, String>
         * @return String
         */
        public static String getSignData(Map<String, Object> params) {
            StringBuffer content = new StringBuffer();
            // 按照key做排序
            List<String> keys = new ArrayList<String>(params.keySet());
            Collections.sort(keys);
            String value = null;
            Object object = null;
            for (int i = 0; i < keys.size(); i++) {
                String key = (String) keys.get(i);
                object = params.get(key);
                if (object instanceof String) {
                    value = (String) object;
                } else {
                    value = String.valueOf(object);
                }

                if (value != null) {
                    content.append((i == 0 ? "" : "&") + key + "=" + value);
                } else {
                    continue;
                }
            }
            return content.toString();
        }
    }

    private void showProgress(String msg) {
        showProgress(msg, false);
    }

    private ProgressDialog progressDialog;

    private void showProgress(String msg, boolean cancelAble) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(cancelAble);
        progressDialog.show();
    }

    private void cancleProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
