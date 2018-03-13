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
     * 跳转到分页查询路径
     */
    listByKeyword: function (pageNo) {

        var keyword = $('#keyword').val();

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
            return;
        // window.location.href = url.listByKeywordUrl + pageNo  + "?isMustContain=" + isMustContain + "&orderType=" + orderType + "&keyword=" + keyword ;
        window.open(common.url.listByKeywordUrl + pageNo + "?isMustContain=" + isMustContain + "&orderType=" + orderType + "&keyword=" + keyword);
    },

};

$(function () {
    common.init();
});
