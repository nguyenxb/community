package com.nguyenxb.community;

import com.alibaba.fastjson.JSONObject;
import com.nguyenxb.community.config.EsConfig;
import com.nguyenxb.community.config.EsConfig;
import com.nguyenxb.community.dao.DiscussPostMapper;
import com.nguyenxb.community.entity.DiscussPost;
import com.nguyenxb.community.service.DiscussPostService;
import com.nguyenxb.community.service.EsService;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private RestHighLevelClient esRestClient;

    @Autowired
    private EsService esService;

    @Autowired
    private DiscussPostService discussPostService;


    class User{
        private String userName;
        private String gender;
        private Integer age;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }


    // ????????????
    @Test
    public void testCreateIndices() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("users");
        // ????????????
        CreateIndexResponse createIndexResponse = esRestClient.indices().create(request, EsConfig.COMMON_OPTIONS);

        // ??????????????????
        boolean acknowledged = createIndexResponse.isAcknowledged();
        System.out.println("????????????"+acknowledged);
    }

    // ????????????
    @Test
    public void testGetIndices() throws IOException {
        // ????????????
        GetIndexRequest request = new GetIndexRequest("users");

        GetIndexResponse getIndexResponse = esRestClient.indices().get(request, EsConfig.COMMON_OPTIONS);
        /// ????????????
        System.out.println(getIndexResponse.getAliases());
        // ?????????????????????
        System.out.println(getIndexResponse.getMappings());
        System.out.println(getIndexResponse.getSettings());// ????????????


    }

    // ????????????
    @Test
    public void testDeleteIndices() throws IOException {
        // ????????????
        DeleteIndexRequest deleteRequest = new DeleteIndexRequest("users");

        AcknowledgedResponse acknowledgedResponse = esRestClient.indices().delete(deleteRequest, EsConfig.COMMON_OPTIONS);

        // ????????????
        System.out.println(acknowledgedResponse.isAcknowledged());


    }

    // ?????????????????????
    @Test
    public void testInsertData() throws IOException {
        IndexRequest request = new IndexRequest();
        // ?????? ??????, ????????????uuid
        request.index("users").id("1001");

        User user = new User();
        user.setUserName("??????");
        user.setAge(30);
        user.setGender("???");
        // ???????????????json
        String jsonString = JSONObject.toJSONString(user);
        System.out.println(jsonString); //{"age":30,"gender":"???","userName":"??????"}

//        ???????????????requst???
        request.source(jsonString,XContentType.JSON);

        // ??????????????????
        IndexResponse indexResponse = esRestClient.index(request, EsConfig.COMMON_OPTIONS);

        System.out.println(indexResponse.getResult());

    }

    // ??????????????????????????????
    @Test
    public void testUpdateData() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("users").id("1001");
        // ????????????, ?????????????????????, ?????????????????????????????????
        updateRequest.doc(XContentType.JSON,"gender","???");

        UpdateResponse updateResponse = esRestClient.update(updateRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(updateResponse.getResult());

        esRestClient.close();


    }

    // ??????????????????????????????
    @Test
    public void testGetData() throws IOException {
        GetRequest getRequest = new GetRequest();
        getRequest.index("users").id("1003");

        GetResponse getResponse = esRestClient.get(getRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(getResponse.getSourceAsString());


        esRestClient.close();

    }

    // ??????????????????????????????
    @Test
    public void testDeleteData() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index("users").id("1001");

        DeleteResponse deleteResponse = esRestClient.delete(deleteRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(deleteResponse.toString());
        esRestClient.close();
    }
    // ??????????????????
    @Test
    public void testBulkAdd() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.add(new IndexRequest().index("users").id("1003").source(XContentType.JSON,"name","??????","gender","???","age",30));
        bulkRequest.add(new IndexRequest().index("users").id("1004").source(XContentType.JSON,"name","??????","gender","???","age",40));
        bulkRequest.add(new IndexRequest().index("users").id("1005").source(XContentType.JSON,"name","??????1","gender","???","age",30));
        bulkRequest.add(new IndexRequest().index("users").id("1006").source(XContentType.JSON,"name","??????2","gender","???","age",50));
        bulkRequest.add(new IndexRequest().index("users").id("1007").source(XContentType.JSON,"name","??????3","gender","???","age",80));
        bulkRequest.add(new IndexRequest().index("users").id("1008").source(XContentType.JSON,"name","??????4","gender","???","age",20));
        bulkRequest.add(new IndexRequest().index("users").id("1009").source(XContentType.JSON,"name","??????5","gender","???","age",23));
        bulkRequest.add(new IndexRequest().index("users").id("1009").source(XContentType.JSON,"name","??????5asd","gender","???","age",23));
        bulkRequest.add(new IndexRequest().index("users").id("1009").source(XContentType.JSON,"name","??????d5","gender","???","age",23));


        BulkResponse bulkResponse = esRestClient.bulk(bulkRequest, EsConfig.COMMON_OPTIONS);
        System.out.println(bulkResponse.getTook());
        System.out.println(bulkResponse.getItems());


    }


    // ??????????????????
    @Test
    public void testBulkDelete() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.add(new DeleteRequest().index("users").id("1003"));
        bulkRequest.add(new DeleteRequest().index("users").id("1004"));
        bulkRequest.add(new DeleteRequest().index("users").id("1005"));



        BulkResponse bulkResponse = esRestClient.bulk(bulkRequest, EsConfig.COMMON_OPTIONS);
        System.out.println(bulkResponse.getTook());
        System.out.println(bulkResponse.getItems());
    }


    //????????????
    @Test
    public void testSearch() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");

        SearchSourceBuilder query = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        searchRequest.source(query);

        SearchResponse searchResponse = esRestClient.search(searchRequest, EsConfig.COMMON_OPTIONS);
        System.out.println(searchResponse);

        SearchHits hits = searchResponse.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(searchResponse.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();


    }

    // ???????????? termQuery
    @Test
    public void testSearchTerm() throws IOException {
        SearchRequest request = new SearchRequest();
        // ?????????????????????30?????????
        request.source(new SearchSourceBuilder().query(QueryBuilders.termQuery("age",30)));
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();


    }

    // ????????????
    @Test
    public void testSearch1() throws IOException {
        SearchRequest request = new SearchRequest();
        // ?????????????????????30?????????
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        // ??? limit ?,? ????????????
        builder.from(1); // ???????????????
        builder.size(4); // ????????????
        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();

    }

    // ????????????
    @Test
    public void testSearch2() throws IOException {
        SearchRequest request = new SearchRequest();
        // ?????????????????????30?????????
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        // ???????????? ??????, ??????
        builder.sort("age", SortOrder.DESC);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();

    }


    // ????????????
    @Test
    public void testSearch3() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        // ?????????????????????30?????????
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        // ?????? ????????????
        String[] excludes = {"age"}; // ??????
        String[] includes = {}; // ????????????
        builder.fetchSource(includes,excludes);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();

    }

    // ????????????
    @Test
    public void testSearch4() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        /// ????????????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // ????????????????????? : ???????????????30 ,???????????????
//        boolQueryBuilder.must(QueryBuilders.matchQuery("age",30));
//        boolQueryBuilder.must(QueryBuilders.matchQuery("sex","???"));
        // ?????????????????????
//        boolQueryBuilder.mustNot(QueryBuilders.matchQuery("sex","???"));

        // ????????????????????? : ???????????????30 ?????? 40
        boolQueryBuilder.should(QueryBuilders.matchQuery("age",30));
        boolQueryBuilder.should(QueryBuilders.matchQuery("age",40));


        builder.query(boolQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // ????????????
    @Test
    public void testSearch5() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        /// ????????????
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("age");

        // ??????30
        rangeQueryBuilder.gte(30);
        // lte : ????????????40 ,,,  lt : ?????? 40
        rangeQueryBuilder.lte(40);


        builder.query(rangeQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }


    // ????????????
    @Test
    public void testSearch6() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        /// ????????????, ????????????????????????
//        FuzzyQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyQuery("name","??????").fuzziness(Fuzziness.ONE);

        // ????????????????????????
        FuzzyQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyQuery("name","??????").fuzziness(Fuzziness.ONE);


        builder.query(fuzzyQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // ????????????
    @Test
    public void testSearch7() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("name", "???");

        // ????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font color='red'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.field("name");

        builder.highlighter(highlightBuilder);
        builder.query(termQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            Collection<HighlightField> values = highlightFields.values();
            System.out.println(values);

            // ??????????????????
            Map<String, HighlightField> highlightFields1 = searchHit.getHighlightFields();
            HighlightField name = highlightFields1.get("name");
            Text[] fragments = name.getFragments();
            String name1 = name.getName();
            System.out.println(fragments[0].toString());
            System.out.println(name1);


        }

        esRestClient.close();
    }

    // ????????????
    @Test
    public void testSearch8() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // ??? ??????, age , ??????????????????
        AggregationBuilder aggregationBuilder = AggregationBuilders.max("maxAge").field("age");

        builder.aggregation(aggregationBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // ????????????
    @Test
    public void testSearch9() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // ??? ??????, age , ??????????????????
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("ageGroup").field("age");

        builder.aggregation(aggregationBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // ??????????????????
        System.out.println(hits.getTotalHits());
        // ????????????
        System.out.println(response.getTook());

        // ??????????????????????????????
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // ????????????
    @Test
    public void testEsServiceSaveDiscussPost() throws IOException {
        DiscussPost discussPost = discussMapper.selectDiscussPostById(200);
        esService.saveDiscussPost(discussPost);
        DiscussPost discussPost1 = discussMapper.selectDiscussPostById(201);
        esService.saveDiscussPost(discussPost1);
        DiscussPost discussPost2 = discussMapper.selectDiscussPostById(202);
        esService.saveDiscussPost(discussPost2);
    }

    // ????????????
    @Test
    public void testEsServiceDeleteDiscussPost() throws IOException {
        esService.deleteDiscussPost(200);
    }

    // ????????????
    @Test
    public void testEsServiceSearchDiscussPost() throws IOException {
        List<DiscussPost> discussPosts = esService.searchDiscussPost("???????????????", 0, 2);
        for (DiscussPost discussPost: discussPosts) {
            System.out.println(discussPost);
        }
    }

    // ???????????????
    @Test
    public void testAnalyzeRequest() throws IOException {
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle("???offer!");
        discussPost.setContent("??????,???????????????,?????????????????????,???????????????????????????????????????offer");
        String  json =  JSONObject.toJSONString(discussPost);
        AnalyzeRequest ik = AnalyzeRequest.withGlobalAnalyzer("ik_smart",discussPost.getTitle(), discussPost.getContent());
        String index1 = ik.index();
        System.out.println(index1);
        ik.explain(true);
        String analyzer = ik.analyzer();
        System.out.println(analyzer);
        esRestClient.indices().analyze(ik,EsConfig.COMMON_OPTIONS);
        String index = ik.index();
        System.out.println(index);

    }
    @Test
    public void testAnalyzer(){
//        AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer("ik_smart","?????????offer,??????????????????,?????????????????????");
        AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer("ik_smart","???????????????");
//        analyzeRequest.explain(true);
        // ??????????????????????????????
//        analyzeRequest.
//        analyzeRequest.analyzer("ik_smart");       // ???????????????????????????  ??????????????? ik_max_word ?????????????????????

        try {
            AnalyzeResponse analyzeResponse = esRestClient.indices().analyze(analyzeRequest, EsConfig.COMMON_OPTIONS);
            List<AnalyzeResponse.AnalyzeToken> tokens = analyzeResponse.getTokens(); // ???????????????????????????
            // ??????Java 8 ????????????????????????
            tokens.forEach(token -> {
                System.out.println(token.getTerm());

                // ?????????????????????????????????2???????????????
//                if (!"<NUM>".equals(token.getType()) || token.getTerm().length() > 2) {
//                    String term = token.getTerm(); // ????????????
//                    System.out.println(term );
//                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ????????????
    @Test
    public void searchData() throws IOException {
        // ??????????????????
        SearchRequest searchRequest = new SearchRequest();
        // ????????????
        searchRequest.indices("users");
        // ??????DSL, ????????????
        // searchSourceBuilder
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // ??????????????????
        SearchSourceBuilder query = sourceBuilder.query(QueryBuilders.matchQuery("userName", "zhangsan"));
        System.out.println(query);


        // ?????????????????????
        searchRequest.source(sourceBuilder);

        // ????????????
        SearchResponse search = esRestClient.search(searchRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(search);
        SearchHits hits = search.getHits();
        System.out.println(hits);
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1){
            System.out.println(hit);
        }


    }

    // ???????????? ??????????????????
    @Test
    public void testIndex() throws IOException {
        System.out.println(esRestClient);
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setUserName("zhangsan");
        user.setGender("man");
        user.setAge(30);
        String  o =  JSONObject.toJSONString(user);
        indexRequest.source(o, XContentType.JSON); // ??????????????????

        // ??????????????????
        IndexResponse index = esRestClient.index(indexRequest, EsConfig.COMMON_OPTIONS);

        // ????????????s
        System.out.println(index);
    }


}
