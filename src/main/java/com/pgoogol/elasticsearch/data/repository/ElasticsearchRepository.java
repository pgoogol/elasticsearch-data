package com.pgoogol.elasticsearch.data.repository;

import co.elastic.clients.elasticsearch.core.search.HitsMetadata;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface ElasticsearchRepository extends PagingAndSortingElasticsearchRepository {
    <T> HitsMetadata<T> getAll(@NotBlank String indexName, @NotNull Class<T> clazz);

    default <T> Optional<T> getById(@NotBlank String indexName, @NotBlank String id, Class<T> clazz) {
        return getById(indexName, id, Collections.emptyList(), clazz);
    }

    default <T> Optional<T> getById(@NotBlank String indexName, @NotBlank String id, @NotBlank String field, Class<T> clazz) {
        return getById(indexName, id, Collections.singletonList(field), clazz);
    }

    default <T> Optional<T> getById(@NotBlank String indexName, @NotBlank String id,
                                    @NotNull List<String> fields, Class<T> clazz
    ) {
        return getByIds(indexName, Collections.singletonList(id), fields, clazz).map(ts -> ts.get(0));
    }

    default <T> Optional<List<T>> getByIds(@NotBlank String indexName, @NotBlank List<String> ids, Class<T> clazz) {
        return getByIds(indexName, ids, Collections.emptyList(), clazz);
    }

    <T> Optional<List<T>> getByIds(
        @NotBlank String indexName, @NotBlank List<String> ids, @NotNull List<String> fields, Class<T> clazz
    );

    boolean existsById(@NotBlank String indexName, @NotBlank String id);

    <T> List<T> saveAll(@NotBlank String indexName, Function<T, String> id, List<T> document) throws IOException;

    <T> T save(@NotBlank String indexName, @NotBlank String id, T document);

    <T> T update(@NotBlank String indexName, @NotBlank String id, T document);

    default void delete(@NotBlank String indexName, @NotEmpty String id) {
        delete(indexName, Collections.singletonList(id));
    }

    void delete(@NotBlank String indexName, @NotEmpty List<String> id);

}
