package com.pgoogol.elasticsearch.data.repository;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.function.BiFunction;

public interface QueryBuilderFunction {

    BiFunction<PageRequest, SearchRequest.Builder, SearchRequest.Builder> PAGE_REQUEST_REQUEST_FUNCTION =
            (pageRequest, builder) -> {
                builder.from(pageRequest.getPageNumber() * pageRequest.getPageSize()).size(pageRequest.getPageSize());
                return builder;
            };

    BiFunction<List<String>, SearchRequest.Builder, SearchRequest.Builder> FIELDS_REQUEST_FUNCTION = (fields, builder) -> {
        if (!fields.isEmpty()) {
            builder.source(sourceBuilder -> sourceBuilder.filter(filterBuilder -> filterBuilder.includes(fields)));
        }
        return builder;
    };

}
