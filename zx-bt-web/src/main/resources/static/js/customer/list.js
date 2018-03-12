var list = {
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
            size: 5
        });


    }
};

$(function () {
    list.init();
});