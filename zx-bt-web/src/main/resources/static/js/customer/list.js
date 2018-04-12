/**
 * 列表页js
 */
var list = {
    url:{
        incrementHot: '/hot/',
    },
    /**
     * 初始化方法
     */
    init : function () {
        /**
         * 是否必须包含 开关样式加载
         */
        $("#isMustContain").show().bootstrapSwitch({
            size: 'mini',
            onText: '必须包含关键字',
            offText: '不必须包含关键字',
        });

        /**
         * 选择框样式加载
         */
        $('.selectpicker').selectpicker({
            style: 'btn-primary  thin-line-8 font-thin-line-8',
            size: 5//选项个数(超过该size会出现滚动条)
        });


    },
    /**
     * 单击复制
     *
     * 很笨但很有效的方法，创建出一个dom，将要复制的文本赋值给dom，将dom的value复制到剪切板。
     * 其他的一些复制方法无法复制隐藏域的内容。
     */
    clickCopy : function (thisA,_id,magnet) {
        var oInput = document.createElement('input');
        oInput.value = magnet;
        document.body.appendChild(oInput);
        oInput.select(); // 选择对象
        document.execCommand("Copy"); // 执行浏览器复制命令
        oInput.className = 'oInput';
        oInput.style.display='none';
        $(thisA).text('复制成功');
        $.post(list.url.incrementHot + _id, {}, function () {
        });
    }
};

$(function () {
    list.init();
});