/**
 * 通用js
 */
var param = {
  //默认起始页
  defaultPageNo:1
};

var common = {
    url: {
        listByKeywordUrl: '/list/',//根据关键词分页查询
    },

    /**
     * 初始化
     */
    init: function() {
        //当焦点在搜索框时,按下回车
        $('#keyword').keydown(function (event) {
            if(event.keyCode !== 13)
                return;
            common.listByKeyword(param.defaultPageNo);
        });

        //点击搜素按钮
        $('#keywordBtn').click(function () {

            common.listByKeyword(param.defaultPageNo);
        });
    },

    /**
     * 生成分页查询路径
     *
     * 不指定 关键词， 从 输入框中读取
     */
    generateListByKeywordPath: function (pageNo) {
        return common.generateListByKeywordPath(pageNo, null);
    },

    /**
     * 生成分页查询路径
     *
     * 可指定关键词
     */
    generateListByKeywordPath: function (pageNo, keyword) {
        if(!keyword)
            keyword = $('#keyword').val();

        if($('#isMustContain').length > 0) {
            var isMustContain = $('#isMustContain').is(":checked");
        }else{
            var isMustContain = false;
        }
        if($('#orderType').length > 0) {
            var orderType = $('#orderType').val();
        }else {
            var orderType = 0;
        }

        if(!keyword.trim())
            return null;
        return common.url.listByKeywordUrl + pageNo + "?isMustContain=" + isMustContain + "&orderType=" + orderType + "&keyword=" + encodeURIComponent(keyword);
    },


    /**
     * 跳转到分页查询路径
     * 原网页跳转 或 打开新网页
     *
     * 不指定关键词，从
     */
    listByKeyword: function (pageNo) {
        common.listByKeyword(pageNo,null)
    },

    /**
     * 跳转到分页查询路径
     * 原网页跳转 或 打开新网页
     *
     * 可指定 关键词
     */
    listByKeyword: function (pageNo, keyword) {
        var isBlank = $('#keywordBtn').attr('isBlank');
        var path = common.generateListByKeywordPath(pageNo,keyword);
        if(!path)
            return;
        if(isBlank === "true")
            window.open(path);
        else
            window.location.href = path;
    },


};

$(function () {
    common.init();
});
