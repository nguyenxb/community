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


    // 创建索引
    @Test
    public void testCreateIndices() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("users");
        // 创建索引
        CreateIndexResponse createIndexResponse = esRestClient.indices().create(request, EsConfig.COMMON_OPTIONS);

        // 获取响应状态
        boolean acknowledged = createIndexResponse.isAcknowledged();
        System.out.println("索引操作"+acknowledged);
    }

    // 查询索引
    @Test
    public void testGetIndices() throws IOException {
        // 查询索引
        GetIndexRequest request = new GetIndexRequest("users");

        GetIndexResponse getIndexResponse = esRestClient.indices().get(request, EsConfig.COMMON_OPTIONS);
        /// 获取别名
        System.out.println(getIndexResponse.getAliases());
        // 获取索引的结构
        System.out.println(getIndexResponse.getMappings());
        System.out.println(getIndexResponse.getSettings());// 获取设置


    }

    // 删除索引
    @Test
    public void testDeleteIndices() throws IOException {
        // 删除索引
        DeleteIndexRequest deleteRequest = new DeleteIndexRequest("users");

        AcknowledgedResponse acknowledgedResponse = esRestClient.indices().delete(deleteRequest, EsConfig.COMMON_OPTIONS);

        // 响应状态
        System.out.println(acknowledgedResponse.isAcknowledged());


    }

    // 给索引添加数据
    @Test
    public void testInsertData() throws IOException {
        IndexRequest request = new IndexRequest();
        // 设置 索引, 及数据的uuid
        request.index("users").id("1001");

        User user = new User();
        user.setUserName("张三");
        user.setAge(30);
        user.setGender("男");
        // 将对象转成json
        String jsonString = JSONObject.toJSONString(user);
        System.out.println(jsonString); //{"age":30,"gender":"男","userName":"张三"}

//        将数据放入requst中
        request.source(jsonString,XContentType.JSON);

        // 执行插入数据
        IndexResponse indexResponse = esRestClient.index(request, EsConfig.COMMON_OPTIONS);

        System.out.println(indexResponse.getResult());

    }

    // 修改某个索引下的数据
    @Test
    public void testUpdateData() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("users").id("1001");
        // 局部修改, 当字段不存在时, 会将该字段插入到索引中
        updateRequest.doc(XContentType.JSON,"gender","女");

        UpdateResponse updateResponse = esRestClient.update(updateRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(updateResponse.getResult());

        esRestClient.close();


    }

    // 查询某个索引下的数据
    @Test
    public void testGetData() throws IOException {
        GetRequest getRequest = new GetRequest();
        getRequest.index("users").id("1003");

        GetResponse getResponse = esRestClient.get(getRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(getResponse.getSourceAsString());


        esRestClient.close();

    }

    // 删除某个索引下的数据
    @Test
    public void testDeleteData() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index("users").id("1001");

        DeleteResponse deleteResponse = esRestClient.delete(deleteRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(deleteResponse.toString());
        esRestClient.close();
    }
    // 批量增加数据
    @Test
    public void testBulkAdd() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.add(new IndexRequest().index("users").id("1003").source(XContentType.JSON,"name","李四","gender","男","age",30));
        bulkRequest.add(new IndexRequest().index("users").id("1004").source(XContentType.JSON,"name","王五","gender","男","age",40));
        bulkRequest.add(new IndexRequest().index("users").id("1005").source(XContentType.JSON,"name","赵六1","gender","男","age",30));
        bulkRequest.add(new IndexRequest().index("users").id("1006").source(XContentType.JSON,"name","赵六2","gender","女","age",50));
        bulkRequest.add(new IndexRequest().index("users").id("1007").source(XContentType.JSON,"name","赵六3","gender","男","age",80));
        bulkRequest.add(new IndexRequest().index("users").id("1008").source(XContentType.JSON,"name","赵六4","gender","女","age",20));
        bulkRequest.add(new IndexRequest().index("users").id("1009").source(XContentType.JSON,"name","赵六5","gender","男","age",23));
        bulkRequest.add(new IndexRequest().index("users").id("1009").source(XContentType.JSON,"name","赵六5asd","gender","男","age",23));
        bulkRequest.add(new IndexRequest().index("users").id("1009").source(XContentType.JSON,"name","赵六d5","gender","男","age",23));


        BulkResponse bulkResponse = esRestClient.bulk(bulkRequest, EsConfig.COMMON_OPTIONS);
        System.out.println(bulkResponse.getTook());
        System.out.println(bulkResponse.getItems());


    }


    // 批量删除数据
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


    //查询数据
    @Test
    public void testSearch() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");

        SearchSourceBuilder query = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        searchRequest.source(query);

        SearchResponse searchResponse = esRestClient.search(searchRequest, EsConfig.COMMON_OPTIONS);
        System.out.println(searchResponse);

        SearchHits hits = searchResponse.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(searchResponse.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();


    }

    // 条件查询 termQuery
    @Test
    public void testSearchTerm() throws IOException {
        SearchRequest request = new SearchRequest();
        // 查询年龄字段为30的数据
        request.source(new SearchSourceBuilder().query(QueryBuilders.termQuery("age",30)));
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();


    }

    // 分页查询
    @Test
    public void testSearch1() throws IOException {
        SearchRequest request = new SearchRequest();
        // 查询年龄字段为30的数据
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        // 跟 limit ?,? 用法一样
        builder.from(1); // 查询第几页
        builder.size(4); // 每页数据
        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();

    }

    // 查询排序
    @Test
    public void testSearch2() throws IOException {
        SearchRequest request = new SearchRequest();
        // 查询年龄字段为30的数据
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        // 设置排序 年龄, 倒序
        builder.sort("age", SortOrder.DESC);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();

    }


    // 过滤字段
    @Test
    public void testSearch3() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        // 查询年龄字段为30的数据
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        // 设置 过滤字段
        String[] excludes = {"age"}; // 排除
        String[] includes = {}; // 包含字段
        builder.fetchSource(includes,excludes);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();

    }

    // 组合查询
    @Test
    public void testSearch4() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        /// 组合查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 必须满足的条件 : 年龄必须是30 ,性别必须男
//        boolQueryBuilder.must(QueryBuilders.matchQuery("age",30));
//        boolQueryBuilder.must(QueryBuilders.matchQuery("sex","男"));
        // 性别必须不是男
//        boolQueryBuilder.mustNot(QueryBuilders.matchQuery("sex","男"));

        // 应该满足的条件 : 年龄应该是30 或者 40
        boolQueryBuilder.should(QueryBuilders.matchQuery("age",30));
        boolQueryBuilder.should(QueryBuilders.matchQuery("age",40));


        builder.query(boolQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // 范围查询
    @Test
    public void testSearch5() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        /// 组合查询
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("age");

        // 大于30
        rangeQueryBuilder.gte(30);
        // lte : 小于等于40 ,,,  lt : 小于 40
        rangeQueryBuilder.lte(40);


        builder.query(rangeQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }


    // 模糊查询
    @Test
    public void testSearch6() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        /// 模糊查询, 允许相差一个字符
//        FuzzyQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyQuery("name","赵六").fuzziness(Fuzziness.ONE);

        // 允许相差两个字符
        FuzzyQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyQuery("name","赵六").fuzziness(Fuzziness.ONE);


        builder.query(fuzzyQueryBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // 高亮查询
    @Test
    public void testSearch7() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("name", "赵");

        // 高亮查询
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

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            Collection<HighlightField> values = highlightFields.values();
            System.out.println(values);

            // 获取高亮字段
            Map<String, HighlightField> highlightFields1 = searchHit.getHighlightFields();
            HighlightField name = highlightFields1.get("name");
            Text[] fragments = name.getFragments();
            String name1 = name.getName();
            System.out.println(fragments[0].toString());
            System.out.println(name1);


        }

        esRestClient.close();
    }

    // 聚合查询
    @Test
    public void testSearch8() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // 对 字段, age , 取最大的年龄
        AggregationBuilder aggregationBuilder = AggregationBuilders.max("maxAge").field("age");

        builder.aggregation(aggregationBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // 分组查询
    @Test
    public void testSearch9() throws IOException {
        SearchRequest request = new SearchRequest();

        request.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // 对 字段, age , 取最大的年龄
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("ageGroup").field("age");

        builder.aggregation(aggregationBuilder);

        request.source(builder);
        SearchResponse response = esRestClient.search(request, EsConfig.COMMON_OPTIONS);

        System.out.println(response);

        SearchHits hits = response.getHits();

        // 获取查询条数
        System.out.println(hits.getTotalHits());
        // 查询时间
        System.out.println(response.getTook());

        // 获取每一条的查询信息
        for (SearchHit searchHit : hits){
            System.out.println(searchHit.getSourceAsString());
        }

        esRestClient.close();
    }

    // 保存数据
    @Test
    public void testEsServiceSaveDiscussPost() throws IOException {
        DiscussPost discussPost = discussMapper.selectDiscussPostById(200);
        esService.saveDiscussPost(discussPost);
        DiscussPost discussPost1 = discussMapper.selectDiscussPostById(201);
        esService.saveDiscussPost(discussPost1);
        DiscussPost discussPost2 = discussMapper.selectDiscussPostById(202);
        esService.saveDiscussPost(discussPost2);
    }

    // 删除数据
    @Test
    public void testEsServiceDeleteDiscussPost() throws IOException {
        esService.deleteDiscussPost(200);
    }

    // 查询数据
    @Test
    public void testEsServiceSearchDiscussPost() throws IOException {
        List<DiscussPost> discussPosts = esService.searchDiscussPost("互联网寒冬", 0, 2);
        for (DiscussPost discussPost: discussPosts) {
            System.out.println(discussPost);
        }
    }

    // 设置分词器
    @Test
    public void testAnalyzeRequest() throws IOException {
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle("求offer!");
        discussPost.setContent("你好,我是阮某某,我是一名应届生,我希望我的努力能够让我拿到offer");
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
//        AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer("ik_smart","我想要offer,我想努力学习,提高自己的能力");
        AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer("ik_smart","互联网寒冬");
//        analyzeRequest.explain(true);
        // 设置需要分词的中文字
//        analyzeRequest.
//        analyzeRequest.analyzer("ik_smart");       // 设置使用什么分词器  也可以使用 ik_max_word 它是细粒度分词

        try {
            AnalyzeResponse analyzeResponse = esRestClient.indices().analyze(analyzeRequest, EsConfig.COMMON_OPTIONS);
            List<AnalyzeResponse.AnalyzeToken> tokens = analyzeResponse.getTokens(); // 获取所有分词的内容
            // 使用Java 8 语法获取分词内容
            tokens.forEach(token -> {
                System.out.println(token.getTerm());

                // 过滤内容，如果文字小于2位也过滤掉
//                if (!"<NUM>".equals(token.getType()) || token.getTerm().length() > 2) {
//                    String term = token.getTerm(); // 分词内容
//                    System.out.println(term );
//                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 查找数据
    @Test
    public void searchData() throws IOException {
        // 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        // 指定索引
        searchRequest.indices("users");
        // 指定DSL, 检索条件
        // searchSourceBuilder
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 构造检索条件
        SearchSourceBuilder query = sourceBuilder.query(QueryBuilders.matchQuery("userName", "zhangsan"));
        System.out.println(query);


        // 将数据存入请求
        searchRequest.source(sourceBuilder);

        // 执行检索
        SearchResponse search = esRestClient.search(searchRequest, EsConfig.COMMON_OPTIONS);

        System.out.println(search);
        SearchHits hits = search.getHits();
        System.out.println(hits);
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1){
            System.out.println(hit);
        }


    }

    // 存储数据 或者更新数据
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
        indexRequest.source(o, XContentType.JSON); // 要保存的内容

        // 执行保存操作
        IndexResponse index = esRestClient.index(indexRequest, EsConfig.COMMON_OPTIONS);

        // 查询数据s
        System.out.println(index);
    }


}
