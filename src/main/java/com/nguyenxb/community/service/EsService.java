package com.nguyenxb.community.service;

import com.alibaba.fastjson.JSONObject;
import com.nguyenxb.community.config.EsConfig;
import com.nguyenxb.community.entity.DiscussPost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class EsService {

    @Autowired
    public RestHighLevelClient esRestClient;

    // 异步保存
    public void saveDiscussPost(DiscussPost discussPost)  {
        IndexRequest request = new IndexRequest();
        // 设置 索引, 及数据的uuid
        request.index(EsConfig.DISCUSS_POST).id(discussPost.getId()+"");

        // 将对象转成json
        String json = JSONObject.toJSONString(discussPost);

        request.source(json,XContentType.JSON);

        // 执行异步添加数据
        esRestClient.indexAsync(request, EsConfig.COMMON_OPTIONS,null);
//        esRestClient.close();
    }

    // 异步删除
    public void deleteDiscussPost(int id) {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index(EsConfig.DISCUSS_POST).id(id+"");

        esRestClient.deleteAsync(deleteRequest, EsConfig.COMMON_OPTIONS,null);

//        esRestClient.close();
    }


    /**
     * @param keyword 查询关键字
     * @param current 当前页
     * @param limit 页面数据
     * @return 查询结果列表
     * @throws IOException
     */
    //
    public List<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        // 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        // 指定索引
        searchRequest.indices(EsConfig.DISCUSS_POST);


        // 指定DSL, 检索条件
        // searchSourceBuilder
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 加入分词器
//        AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer("ik_smart",keyword);
        // 构造检索条件
        sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword,"title","content"));
        // 分词分析
//        AnalyzeResponse analyzeResponse = esRestClient.indices().analyze(analyzeRequest, EsConfig.COMMON_OPTIONS);

//        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeResponse.getTokens(); // 获取所有分词的内容

        FieldSortBuilder type = SortBuilders.fieldSort("type").order(SortOrder.DESC);
        FieldSortBuilder score = SortBuilders.fieldSort("score").order(SortOrder.DESC);
        FieldSortBuilder createTime = SortBuilders.fieldSort("createTime").order(SortOrder.DESC);

        sourceBuilder.from(current);
        sourceBuilder.size(limit);


        sourceBuilder.sort(type);
        sourceBuilder.sort(score);
        sourceBuilder.sort(createTime);



//        System.out.println(query);
        // 高亮查询
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<em>");
        highlightBuilder.postTags("</em>");
        highlightBuilder.field("title");
        highlightBuilder.field("content");

        sourceBuilder.highlighter(highlightBuilder);


        // 将数据存入请求
        searchRequest.source(sourceBuilder);

        // 执行检索
        SearchResponse search = null;
        try {
            search = esRestClient.search(searchRequest, EsConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(search == null){
            return null;
        }

        SearchHits hits = search.getHits();

        List<DiscussPost> discussPostList = new ArrayList<>();

        for (SearchHit hit : hits) {

            DiscussPost post = new DiscussPost();

            String id = hit.getSourceAsMap().get("id").toString();
            post.setId(Integer.valueOf(id));

            String userId = hit.getSourceAsMap().get("userId").toString();
            post.setUserId(Integer.valueOf(userId));

            String title = hit.getSourceAsMap().get("title").toString();
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields!=null){
                HighlightField title1 = highlightFields.get("title");
                if (title1 != null){
                    Text[] fragments = title1.getFragments();
                    title = fragments[0].toString();
                }
            }
            post.setTitle(title);

            String content = hit.getSourceAsMap().get("content").toString();
            Map<String, HighlightField> highlightFields2 = hit.getHighlightFields();
            if (highlightFields2!=null){
                HighlightField content1 = highlightFields2.get("content");
                if (content1 != null){
                    Text[] fragments = content1.getFragments();
                    content = fragments[0].toString();
                }
            }
            post.setContent(content);

            String status = hit.getSourceAsMap().get("status").toString();
            post.setStatus(Integer.valueOf(status));

            String createTime1 = hit.getSourceAsMap().get("createTime").toString();
            post.setCreateTime(new Date(Long.valueOf(createTime1)));

            String commentCount = hit.getSourceAsMap().get("commentCount").toString();
            post.setCommentCount(Integer.valueOf(commentCount));



            discussPostList.add(post);
        }
//        esRestClient.close();
        return discussPostList;

    }
}
