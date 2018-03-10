package com.zx.bt.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.exception.BTException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-10 10:47
 * elasticSearch 的 index: metadata type:metadata
 */
@Slf4j
@Component
public class MetadataService {

    private static final String LOG = "[ESMetadataRepository]";

    private static final String ES_INDEX = "metadata";
    private static final String ES_TYPE = "metadata";

    private final TransportClient transportClient;
    private final ObjectMapper objectMapper;

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
        if(!infoHashFilterAvailable)
            return;
        IndexResponse result = transportClient.prepareIndex(ES_INDEX, ES_TYPE)
                .setSource(objectMapper.writeValueAsBytes(metadata), XContentType.JSON)
                .get();
        if (!result.status().equals(RestStatus.CREATED)) {
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
                        .setSource(objectMapper.writeValueAsBytes(item),XContentType.JSON));
            } catch (JsonProcessingException e) {
                throw new BTException(LOG + "[batchInsert]批量新增失败,json解析异常:" + e.getMessage());
            }
        }
        BulkResponse result = bulkRequest.get();
        if(!result.status().equals(RestStatus.OK))
            throw new BTException(LOG + "[batchInsert]批量新增失败,状态码错误,当前状态码:" + result.status());
    }

    /**
     * 递增热度
     * 允许并发导致的误差
     */
    @SneakyThrows
    public void incrementHot(String _id, int currentHot) {
        UpdateResponse result = transportClient.prepareUpdate(ES_INDEX, ES_TYPE, _id)
                .setDoc(XContentFactory.jsonBuilder().startObject().field("hot", ++currentHot).endObject())
                .get();
        if(!result.status().equals(RestStatus.OK))
            throw new BTException(LOG + "[incrementHot]递增热度失败,状态码错误,当前状态码:" + result.status());
    }


    /**
     * 预先分页(scroll)
     * 返回总条数/scrollId/该次数据
     */
    public ScrollResult<String> preListFindInfoHash(int size,int timeoutSecond) {
        String field = "infoHash";
        SearchResponse response = transportClient.prepareSearch(ES_INDEX)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addDocValueField(field)
                //每次返回条数
                .setSize(size)
                // 这个游标维持多长时间
                .setScroll(TimeValue.timeValueSeconds(timeoutSecond))
                .get();
        List<String> infoHashs = new LinkedList<>();
        for (SearchHit item : response.getHits()) {
            infoHashs.add((String) item.field(field).getValues().get(0));
        }
        return new ScrollResult<>(infoHashs,response.getScrollId(),response.getHits().totalHits);
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
    public List<String> listFindInfoHash(String scrollId,int timeoutSecond){
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
     * 分页查询 infoHash 字段
     */
    public List<String> listFindInfoHash(int start, int size) {
        String field = "infoHash";
        SearchResponse response = transportClient.prepareSearch(ES_INDEX)
                .setTypes(ES_TYPE)
                .setFrom(start).setSize(size)
                .setExplain(false)
                .addDocValueField(field)
                .get();
        List<String> infoHash = new LinkedList<>();
        for (SearchHit item : response.getHits()) {
            infoHash.add((String) item.field(field).getValues().get(0));
        }
        return infoHash;
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
     * 查询
     */



    /**
     * 设置infoHashFilterAvailable
     */
    public void setInfoHashFilterAvailable(boolean infoHashFilterAvailable) {
        this.infoHashFilterAvailable = infoHashFilterAvailable;
    }


    /**
     * scroll 返回的数据 和 最新的scrollId
     */
    @Data
    @AllArgsConstructor
    public static class ScrollResult<T>{
        private List<T> list;
        private String scrollId;
        private Long totalSize;
    }
}
