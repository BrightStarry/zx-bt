package com.zx.bt.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.enums.LengthUnitEnum;
import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.store.CommonCache;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.common.vo.PageVO;
import joptsimple.internal.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * author:ZhengXing
 * datetime:2018-03-10 10:47
 * elasticSearch 的 index: metadata type:metadata
 */
@Slf4j
public class MetadataService {

    private static final String LOG = "[MetadataService]";
    private static final String KEY_SEPARATOR = "-";

    //es索引名
    private static final String ES_INDEX = "metadata";
    //es类型名
    private static final String ES_TYPE = "metadata";
    //要查询的字段
    private static final String[] SELECT_FIELD_NAMES = new String[]{"infoHash", "name", "hot", "_id", "createTime"};

    private final TransportClient transportClient;
    private final ObjectMapper objectMapper;


    /**
     * 此处是因为 spider模块中，无需这两个缓存类
     */
    @Autowired(required = false)
    private CommonCache<String> hotCache;
    @Autowired(required = false)
    private CommonCache<PageVO<MetadataVO>> listCache;

    /**
     * infoHashFilter是否可用标识
     * 之所以在该类也建立一个该标识,是因为如果不建立,会引发Spring Bean循环引用创建异常
     */
    private volatile boolean infoHashFilterAvailable = false;

    public MetadataService(TransportClient transportClient, ObjectMapper objectMapper) {
        this.transportClient = transportClient;
        this.objectMapper = objectMapper;
    }


    /**
     * 新增
     */
    @SneakyThrows
    public void insert(Metadata metadata) {
        //当过滤器不可用时,不允许新增数据,防止数据重复
        if (!infoHashFilterAvailable)
            return;
        IndexResponse result = transportClient.prepareIndex(ES_INDEX, ES_TYPE)
                .setSource(objectMapper.writeValueAsBytes(metadata), XContentType.JSON)
                .get();
        if (!result.status().equals(RestStatus.CREATED)) {
            log.error("{}[insert]新增失败.状态码错误,当前状态码:{}",LOG,result.status());
            throw new BTException(LOG + "[insert]新增失败.状态码错误,当前状态码:" + result.status());
        }
    }

    /**
     * 批量增加
     */
    public void batchInsert(Collection<Metadata> metadatas) {
        BulkRequestBuilder bulkRequest = transportClient.prepareBulk();
        for (Metadata item : metadatas) {
            try {
                bulkRequest.add(transportClient.prepareIndex(ES_INDEX, ES_TYPE)
                        .setSource(objectMapper.writeValueAsBytes(item), XContentType.JSON));
            } catch (JsonProcessingException e) {
                throw new BTException(LOG + "[batchInsert]批量新增失败,json解析异常:" + e.getMessage());
            }
        }
        BulkResponse result = bulkRequest.get();
        if (!result.status().equals(RestStatus.OK))
            throw new BTException(LOG + "[batchInsert]批量新增失败,状态码错误,当前状态码:" + result.status());
    }

    /**
     * 递增热度
     * 允许并发导致的误差
     */
    @SneakyThrows
    public void incrementHot(String esId, long currentHot) {
        if(hotCache == null)
            return;
        if(hotCache.containKey(esId))
            return;
        UpdateResponse result = transportClient.prepareUpdate(ES_INDEX, ES_TYPE, esId)
                .setDoc(XContentFactory.jsonBuilder().startObject().field("hot", ++currentHot).endObject())
                .get();
        if (!result.status().equals(RestStatus.OK)){
            log.error("{}[incrementHot]递增热度失败,状态码错误,当前状态码:{}",LOG,result.status());
//            throw new BTException(ErrorEnum.UNKNOWN_ERROR);
        }
        //存入以esId为key,值为空串的缓存
        hotCache.put(esId, Strings.EMPTY);
    }


    /**
     * 预先分页(scroll)
     * 返回总条数/scrollId/该次数据
     */
    public ScrollResult<String> preListFindInfoHash(int size, int timeoutSecond) {
        String field = "infoHash";
        SearchResponse response = transportClient.prepareSearch(ES_INDEX)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                //增加这句话可以只返回单个字段
                .addDocValueField(field)
                //每次返回条数
                .setSize(size)
                // 这个游标维持多长时间
                .setScroll(TimeValue.timeValueSeconds(timeoutSecond))
                .get();
        if (!response.status().equals(RestStatus.OK))
            throw new BTException(LOG + "[preListFindInfoHash]预分页查询失败,状态码错误,当前状态码:" + response.status());
        List<String> infoHashs = new LinkedList<>();
        for (SearchHit item : response.getHits()) {
            infoHashs.add((String) item.field(field).getValues().get(0));
        }
        return new ScrollResult<>(infoHashs, response.getScrollId(), response.getHits().totalHits);
    }

    /**
     * 统计总条数
     */
    public long count() {
        SearchResponse result = transportClient.prepareSearch(ES_INDEX)
                .setTypes(ES_TYPE)
                .execute().actionGet();
        return result.getHits().totalHits;
    }

    /**
     * scroll分页查询
     */
    public List<String> listFindInfoHash(String scrollId, int timeoutSecond) {
        String field = "infoHash";
        SearchResponse response = transportClient.prepareSearchScroll(scrollId)
                .setScroll(TimeValue.timeValueSeconds(timeoutSecond)).get();
        List<String> infoHashs = new LinkedList<>();
        for (SearchHit item : response.getHits()) {
            infoHashs.add((String) item.field(field).getValues().get(0));
        }
        return infoHashs;
    }

    /**
     * 清除分页
     */
    public void clearScroll(String scrollId) {
        transportClient.prepareClearScroll().addScrollId(scrollId).get();
    }


    /**
     * 根据 搜索词 排序类型 是否必须包含 分页查询
     * <p>
     * 当 OrderTypeEnum == OrderTypeEnum.NONE && isMustContain == false , 则默认为 根据相关性排序, 不必须包含.
     *
     * @param keyword       搜索关键词
     * @param orderTypeEnum 排序类型 see{@link OrderTypeEnum}
     * @param isMustContain 是否必须包含该关键字
     * @param pageNo        当前页,从1开始
     * @param pageSize      每页条数
     * @return see {@link PageVO}对象
     */
    @SneakyThrows
    public PageVO<MetadataVO> listByKeyword(String keyword, OrderTypeEnum orderTypeEnum, boolean isMustContain, int pageNo, int pageSize) {
        PageVO<MetadataVO> result;
        /**
         * 如果是默认查询条件（排序为默认，不必须包含，页数为1）
         * 尝试从缓存中读取
         */
        // 是否启用缓存
        boolean cacheable;
        if ((cacheable = EnumUtil.equals(orderTypeEnum.getCode(), OrderTypeEnum.NONE) && !isMustContain && pageNo == 1)) {
            result = listCache.get(keyword);
            if(result != null)
                return result;
        }


        // 分词匹配查询
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", keyword);
        //是否必须包含该关键字,增加.operator(Operator.AND),表示必须包含这个词, 不加,则是普通的根据keyword的分词查询
        if (isMustContain)
            matchQueryBuilder.operator(Operator.AND);

        //总查询
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(ES_INDEX).setTypes(ES_TYPE)
                // 设置上分词匹配查询
                .setQuery(matchQueryBuilder);
        // 判断排序规则
        switch (orderTypeEnum) {
            case NONE:
                //什么也不做
                break;
            case EXPLAIN:
                // 如下表示,根据相关性(权重)排序,但其优先级比addSort低
                searchRequestBuilder.setExplain(true);
                break;
            default:
                searchRequestBuilder.addSort(orderTypeEnum.getFieldName(), SortOrder.DESC);
                break;

        }

//        //设置要查询的字段
//        for (int i = 0; i < SELECT_FIELD_NAMES.length; i++) {
//            searchRequestBuilder.addDocValueField(SELECT_FIELD_NAMES[i]);
//        }


        //设置分页
        SearchResponse response = searchRequestBuilder
                .setFrom((pageNo - 1) * pageSize).setSize(pageSize)
                .get();
        if (!response.status().equals(RestStatus.OK))
            throw new BTException(LOG + "[listByKeyword]分页查询失败,状态码错误,当前状态码:" + response.status());
        SearchHits hits = response.getHits();
        //总条数
        long totalElement = hits.totalHits;
        //总页数
        int totalPage = (int) (totalElement + pageSize - 1) / pageSize;
        //防止总页数为0
        totalPage = totalPage == 0 ? 1 : totalPage;
        //返回的list
        List<MetadataVO> metadatas = new LinkedList<>();
        Metadata metadata;
        for (SearchHit item : hits) {
            metadata = objectMapper.readValue(item.getSourceAsString(), Metadata.class).set_id(item.getId());
            metadatas.add(new MetadataVO(metadata,LengthUnitEnum.convert(metadata.getLength())).clearNotMustProperty());
        }
        result = new PageVO<>(pageNo, pageSize, totalElement, totalPage, metadatas, keyword);
        if(cacheable)
            listCache.put(keyword,result);
        return result;
    }

    /**
     * 根据 es的_id查询详细信息
     */
    @SneakyThrows
    public MetadataVO findOneByEsId(String esId) {
        GetResponse response = transportClient.prepareGet(ES_INDEX, ES_TYPE, esId).get();
        if(!response.isExists())
            return null;
        //解析返回结果
        Metadata metadata = objectMapper.readValue(response.getSourceAsString(), Metadata.class);
        MetadataVO metadataVO = new MetadataVO(metadata, LengthUnitEnum.convert(metadata.getLength()),
                objectMapper.readValue(metadata.getInfoString(), getCollectionType(List.class, MetadataVO.Info.class)));
        List<MetadataVO.Info> infos = metadataVO.getInfos();
        //转换文件属性
        for (int i = 0; i < infos.size(); i++) {
            infos.get(i).setLengthStr(LengthUnitEnum.convert(infos.get(i).getLength()));
        }
        //给该资源增加热度
        incrementHot(esId, metadata.getHot());
        return metadataVO.clearNotMustProperty();
    }

    /**
     * 查询创建时间大于等于某个时间的记录数
     */
    public long countByCreateTimeGE(Date date) {
        String fieldName = "createTime";
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(fieldName)
                .from(date.getTime());
        SearchResponse result = transportClient.prepareSearch(ES_INDEX).setTypes(ES_TYPE)
                .setQuery(rangeQuery)
                .execute()
                .actionGet();
        return result.getHits().totalHits;
    }


    /**
     * 设置infoHashFilterAvailable
     */
    public void setInfoHashFilterAvailable(boolean infoHashFilterAvailable) {
        this.infoHashFilterAvailable = infoHashFilterAvailable;
    }

    /**
     * 获取泛型的Collection Type
     */
    public <C extends Collection, E> JavaType getCollectionType(Class<C> collectionClass, Class<E> elementClass) {
        return objectMapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }


    /**
     * scroll 返回的数据 和 最新的scrollId
     */
    @Data
    @AllArgsConstructor
    public static class ScrollResult<T> {
        private List<T> list;
        private String scrollId;
        private Long totalSize;
    }
}
