package com.nguyenxb.community.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class EsConfig {
  public static final String DISCUSS_POST = "discusspost";

  // 通用设置项
  public static final RequestOptions COMMON_OPTIONS;
  static {
    RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
    COMMON_OPTIONS = builder.build();
  }

  @Bean
  public RestHighLevelClient esRestClient() {
    RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost("localhost", 9200, "http")
                    // new HttpHost("localhost", 9201, "http")  // 多个ES
            )
    );
    // 创建索引
//    createIndices(restHighLevelClient);

    return restHighLevelClient;
  }

  // 初始化索引
  private void createIndices(RestHighLevelClient esRestClient)  {
    CreateIndexRequest request = new CreateIndexRequest(DISCUSS_POST);
    // 创建索引
    try {
      esRestClient.indices().create(request, COMMON_OPTIONS);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
