/**
 * 弹幕相关js
 */
var barrage = {
    url: {
        webSocketUrl: 'ws://' + document.location.host + '/websocket',
    },
    /**
     * 连接到WebSocket
     */
    webSocketConnect:function(){
        var webSocket  = new WebSocket(barrage.url.webSocketUrl);

        /**
         * 发送握手请求
         */
        webSocket.send()

        /**
         * 接收到消息
         */
        webSocket.onmessage = function (event) {
            var webSocketMessage = JSON.parse(event.data);
            console.log(webSocketMessage);
        }
    },
};

$(function () {
   barrage.webSocketConnect();
});