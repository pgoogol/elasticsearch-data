package com.pgoogol.elasticsearch.data.repository;

import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.springframework.data.domain.PageRequest;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface PagingAndSortingElasticsearchRepository {

    int DEFAULT_PAGE_SIZE = 1000;
    int DEFAULT_PAGE = 0;

    <T> HitsMetadata<T> getAll(@NotBlank String indexName, @NotNull PageRequest pageRequest, @NotNull Class<T> clazz);

    default <T> Optional<List<T>> getByIds(@NotBlank String indexName, @NotBlank List<String> ids,
                                           @NotNull PageRequest pageRequest, Class<T> clazz
    ) {
        return getByIds(indexName, ids, Collections.emptyList(), pageRequest, clazz);
    }

    <T> Optional<List<T>> getByIds(@NotBlank String indexName, @NotBlank List<String> ids,
                                   @NotNull List<String> fields, @NotNull PageRequest pageRequest,
                                   Class<T> clazz
    );

}
