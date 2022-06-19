package com.pgoogol.elasticsearch.data.repository;

import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.springframework.data.domain.PageRequest;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public interface PagingAndSortingElasticsearchRepository {

    <T> HitsMetadata<T> getAll(@NotBlank String indexName, @NotNull PageRequest pageRequest, @NotNull Class<T> clazz);


}
