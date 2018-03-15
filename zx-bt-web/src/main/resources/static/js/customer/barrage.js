/**
 * 弹幕相关js
 */
var webSocket;
var barrage = {
    body: $('body'),
    url: {
        webSocketUrl: 'ws://' + document.location.host + '/websocket',
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
            if(hex_md5(webSocketMessage.code + webSocketMessage.data.sessionId + webSocketMessage.timestamp) !== webSocketMessage.hash)
                return;
            console.log(webSocketMessage);
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
            webSocket = barrage.webSocketConnect();
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
};

$(function () {
   webSocket = barrage.webSocketConnect();
    $('body').barrager({'img':'img/unknownUser.png','info':'Hello world!','href':'/'});
});

