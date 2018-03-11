var url = {
    listByKeywordUrl: '/list/',//根据关键词分页查询
};

var param = {
  //默认起始页
  defaultPageNo:1
};

var index = {
    init: function() {
        /**
         * 搜索框失去焦点
         */
        $('#keyword').keydown(function (event) {
            if(event.keyCode !== 13)
                return
            var keyword = $(this).val();
            index.listByKeyword(keyword, param.defaultPageNo);
        });
    },
    listByKeyword: function (keyword,pageNo) {
        if(!keyword.trim())
            return;
        window.location.href = "/" + keyword + url.listByKeywordUrl + pageNo;
    },

};

$(function () {
    index.init();
});
