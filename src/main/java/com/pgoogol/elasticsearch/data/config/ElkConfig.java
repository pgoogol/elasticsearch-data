package com.pgoogol.elasticsearch.data.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.elasticsearch")
public class ElkConfig {

    private static final Logger log = LoggerFactory.getLogger(ElkConfig.class);

    private final ElasticsearchProperties elasticsearchProperties;
    private final boolean sslEnabled;

    public ElkConfig(ElasticsearchProperties elasticsearchProperties,
                     @Value("${spring.elasticsearch.ssl_enabled}") boolean sslEnabled) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.sslEnabled = sslEnabled;
    }

    @Bean
    ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    @Bean
    ElasticsearchTransport transport(RestClient restClient, JacksonJsonpMapper jacksonJsonpMapper) {
        return new RestClientTransport(restClient, jacksonJsonpMapper);
    }

    @Bean
    RestClient restClient(RestClientBuilder.HttpClientConfigCallback clientConfigCallback) {
        String scheme = sslEnabled ? "https" : "http";
        HttpHost[] hosts = elasticsearchProperties.getUris().stream()
                .map(url -> {
                    String[] hostPort = url.split(":");
                    return new HttpHost(hostPort[0], Integer.parseInt(hostPort[1]), scheme);
                })
                .toArray(HttpHost[]::new);
        RequestOptions requestOptions = RequestOptions.DEFAULT.toBuilder()
                .addHeader("Content-type", "application/json")
                .addHeader("X-Elastic-Product", "*")
                .build();
        return RestClient.builder(hosts)
                .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
                .setHttpClientConfigCallback(clientConfigCallback)
                .setDefaultHeaders(requestOptions.getHeaders().toArray(new Header[0]))
                .build();
    }

    @Bean
    RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback(CredentialsProvider credentialsProvider) {
        return httpClientBuilder -> {
            httpClientBuilder.disableAuthCaching();
            httpClientBuilder.addInterceptorFirst(new ElasticProductHeaderInterceptor());
            if (sslEnabled) {
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                try {
                    httpClientBuilder.setSSLContext(SSLContexts.custom().loadTrustMaterial(null, ((x509Certificates, s) -> true)).build());
                } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                    log.error(e.getMessage(), e);
                }
            }
            return httpClientBuilder;
        };
    }

    @Bean
    CredentialsProvider credentialsProvider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                elasticsearchProperties.getUsername(),
                elasticsearchProperties.getPassword())
        );
        return provider;
    }

    @Bean
    JacksonJsonpMapper jacksonJsonpMapper() {
        JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper();
        ObjectMapper objectMapper = jacksonJsonpMapper.objectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_DATE));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.findAndRegisterModules();

        return jacksonJsonpMapper;
    }



}
