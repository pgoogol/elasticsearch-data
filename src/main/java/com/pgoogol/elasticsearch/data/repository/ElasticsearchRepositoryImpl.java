package com.pgoogol.elasticsearch.data.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgoogol.elasticsearch.data.model.IndexModel;
import lombok.SneakyThrows;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pgoogol.elasticsearch.data.repository.QueryBuilderFunction.FIELDS_REQUEST_FUNCTION;
import static com.pgoogol.elasticsearch.data.repository.QueryBuilderFunction.PAGE_REQUEST_REQUEST_FUNCTION;

@Repository
public class ElasticsearchRepositoryImpl implements ElasticsearchRepository, ElasticsearchIndiciesRepository {

    private final ElasticsearchClient client;

    public ElasticsearchRepositoryImpl(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public <T> HitsMetadata<T> getAll(String indexName, Class<T> clazz) {
        return getAll(indexName, PageRequest.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE), clazz);
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public <T> HitsMetadata<T> getAll(String indexName, PageRequest pageRequest, Class<T> clazz) {
        SearchRequest.Builder builder = new SearchRequest
                .Builder()
                .index(indexName);

        PAGE_REQUEST_REQUEST_FUNCTION.apply(pageRequest, builder);

        SearchResponse<T> search = client.search(builder.build(), clazz);
        return search.hits();
    }

    @Override
    public <T> Optional<List<T>> getByIds(String indexName, List<String> ids, List<String> fields, Class<T> clazz) {
        return getByIds(indexName, ids, fields, PageRequest.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE), clazz);
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public <T> Optional<List<T>> getByIds(String indexName, List<String> ids,
                                          @NotNull List<String> fields, PageRequest pageRequest,
                                          Class<T> clazz
    ) {
        SearchRequest.Builder builder = new SearchRequest
                .Builder()
                .index(indexName)
                .query(qb -> qb
                        .ids(idBuilder -> idBuilder
                                .queryName("idQuery" + ids)
                                .values(ids)
                        )
                );

        PAGE_REQUEST_REQUEST_FUNCTION.apply(pageRequest, builder);
        FIELDS_REQUEST_FUNCTION.apply(fields, builder);

        SearchResponse<T> search = client.search(builder.build(), clazz);
        return Optional.of(search.hits().hits().stream().map(Hit::source).collect(Collectors.toList()));
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public <T> T save(String indexName, String id, T document) {
        client.create(builder -> builder
                .index(indexName)
                .document(document)
                .id(id)
        );
        return document;
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public <T> T update(String indexName, String id, T document) {
        client.update(
                builder -> builder.index(indexName)
                        .id(id)
                        .doc(document),
                Object.class
        );
        return document;

    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public void delete(String indexName, List<String> ids) {
        client.deleteByQuery(builder -> builder.index(indexName)
                .query(queryBuilder -> queryBuilder.ids(
                        IdsQuery.of(idsBuilder -> idsBuilder.values(ids))
                ))
        );
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public boolean existsById(String indexName, String id) {
        SearchResponse<Object> search = client.search(
                builder -> builder
                        .index(indexName)
                        .query(builder1 -> builder1.ids(builder2 -> builder2.values(id))),
                Object.class
        );
        return (search.hits().total() != null ? search.hits().total().value() : 0) == 1;
    }

    @Override
    public <T> List<T> saveAll(String indexName, Function<T, String> id, List<T> items) throws IOException {
            if (items.size() <= 10000) {
                List<BulkOperation> bulkOperations = new ArrayList<>();
                items.forEach(
                        item -> bulkOperations.add(
                                new BulkOperation.Builder()
                                        .create(builder -> builder.document(item).id(id.apply(item)))
                                        .build()
                        )
                );
                BulkRequest bulkRequest = new BulkRequest.Builder()
                        .index(indexName)
                        .operations(bulkOperations)
                        .build();
                BulkResponse bulk = client.bulk(bulkRequest);
                ObjectMapper mapper = new ObjectMapper();
                Class<T> clazz = (Class<T>) items.get(0).getClass();
                return bulk.items()
                        .stream()
                        .map(BulkResponseItem::get)
                        .filter(Objects::nonNull)
                        .map(InlineGet::source)
                        .map(stringJsonDataMap -> mapper.convertValue(stringJsonDataMap, clazz))
                        .collect(Collectors.toList());
            } else {
                List<T> result = new ArrayList<>();
                for (List<T> ts : partitionListBasedOnSize(items)) {
                    result.addAll(saveAll(indexName, id, ts));
                }
                return result;
            }
    }

    private <T> List<List<T>> partitionListBasedOnSize(List<T> items) {
        List<List<T>> chunkList = new LinkedList<>();
        for (int i = 0; i < items.size(); i += 10000) {
            chunkList.add(items.subList(i, i + 10000 >= items.size() ? items.size() - 1 : i + 10000));
        }
        return chunkList;
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public boolean create(String dictionaryName, List<IndexModel> modelDictionary) {
        CreateIndexRequest.Builder request = new CreateIndexRequest.Builder().index(dictionaryName);
        if (!modelDictionary.isEmpty()) {
            request.mappings(builder -> builder.properties(MappingIndexUtils.prepareMapping(modelDictionary)));
        }
        CreateIndexResponse createIndexResponse = client.indices().create(request.build());
        return createIndexResponse.acknowledged();
    }

    @Override
    @SneakyThrows({IOException.class, ElasticsearchException.class})
    public boolean exists(String dictionaryName) {
        BooleanResponse exists = client.indices().exists(builder -> builder.index(dictionaryName));
        return exists.value();
    }
}
