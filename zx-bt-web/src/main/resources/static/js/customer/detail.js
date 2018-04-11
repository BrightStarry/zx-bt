/**
 * 详情页js
 */
var detail = {
    /**
     * 初始化方法
     */
    init : function () {
        /**
         * 点击 复制按钮触发
         * 无关紧要的bug:最多复位一次,第二次就会一直在加载状态
         */
        $('#copyBtn').click(function () {
            var magnetText = $('#magnetText');
            magnetText.select();
            document.execCommand("Copy");
            $(this).button('loading').delay(1000).queue(function() {
                $(this).button('reset');
            });
        });
    },

};

$(function () {
    detail.init();
});