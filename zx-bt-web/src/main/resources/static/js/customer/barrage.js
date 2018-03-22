/**
 * 弹幕相关js
 */
var webSocket;
var sessionId;
var barrage = {
    body: $('body'),
    url: {
        //websocket连接地址
        webSocketUrl: 'wss://' + document.location.host + '/websocket',
        //弹幕默认头像路径
        defaultHeadImgPath: 'img/unknownUser.png',
        //自己的弹幕头像路径
        selfHeadImgPath: 'img/selfUser.png',

    },
    /**
     * 连接到WebSocket
     */
    webSocketConnect:function(){
        webSocket  = new WebSocket(barrage.url.webSocketUrl);
        /**
         * 接收到消息
         */
        webSocket.onmessage = function (event) {
            var webSocketMessage = JSON.parse(event.data);
            // console.log(webSocketMessage);
            if(webSocketMessage.code !== '0000') {
                alert(webSocketMessage.message);
                return;
            }
            switch (webSocketMessage.type){
                //握手响应(此处因为只在未关闭的首页支持弹幕,所以直接存入变量,否则,可以使用前端的啥子数据库来着)
                case 0:
                    //保存该id,以作为token
                    sessionId = webSocketMessage.data.sessionId;
                    break;
                //弹幕响应
                case 1:
                    //生成弹幕
                    barrage.generateBarrage(webSocketMessage.data.barrageMessage,
                        webSocketMessage.data.sessionId === sessionId);
                    break;
            }
        };

        /**
         * 发生异常
         */
        webSocket.onerror = function (event) {
            console.log(event.data);
            webSocket = barrage.webSocketConnect();
        };

        /**
         * 连接关闭
         */
        webSocket.onclose = function () {
            console.log("连接关闭");
        };

        /**
         * 窗口关闭时,关闭连接
         */
        window.onbeforeunload = function () {
            webSocket.close();
        };


        /**
         * 成功后发送握手
         */
        webSocket.onopen = function () {
            var handshakeRequest = {'type':0,'timestamp':new Date().getTime()};
            webSocket.send(JSON.stringify(handshakeRequest));
        };
        return webSocket;
    },

    /**
     * 在屏幕生成一条普通弹幕
     */
    generateBarrage: function (message,isSelf) {
        if(!message)
            return;
        var item={
            img: isSelf ? barrage.url.selfHeadImgPath : barrage.url.defaultHeadImgPath, //图片
            info: message, //文字
            // href:'/', //链接
            close:true, //显示关闭按钮
            speed:8, //延迟,单位秒,默认8
            // bottom:70, //距离底部高度,单位px,默认随机
            color:'#fff', //颜色,默认白色
            old_ie_color:'#000000', //ie低版兼容色,不能与网页背景相同,默认黑色
        }
        barrage.body.barrager(item);
    },

    /**
     * 初始化
     */
    init: function() {
        if (!window.WebSocket) {
            alert('你的浏览器暂不支持WebSocket');
            return;
        }
        //建立连接
        webSocket = barrage.webSocketConnect();
        //当焦点在弹幕框时,按下回车
        $('#message').keydown(function (event) {
            if(event.keyCode !== 13)
                return;
            barrage.sendBarrage();
        });

        //点击发送按钮
        $('#sendBarrageBtn').click(function () {
            barrage.sendBarrage();
        });
        // 发送欢迎语句
        barrage.generateBarrage("欢迎来到福利球,你可以在底部发送所有用户可见的弹幕~~~",false);
    },

    /**
     * 发送弹幕
     */
    sendBarrage: function () {
        var message = $('#message').val();

        if(!message || message.trim().length <= 0){
            $('#message').val('');
            return;
        }
        if(message.length > 32) {
            alert('弹幕过长');
            return;
        }
        if(webSocket.readyState !== 1) {
            alert('连接到服务器失败,请刷新重试');
            return;
        }
        var timestamp = new Date().getTime();
        var barrageRequest = {'type':1,'timestamp':timestamp,'data':{'barrageMessage':message}};
        webSocket.send(JSON.stringify(barrageRequest));
        $('#message').val('');
    },
};

$(function () {
    barrage.init();

});

