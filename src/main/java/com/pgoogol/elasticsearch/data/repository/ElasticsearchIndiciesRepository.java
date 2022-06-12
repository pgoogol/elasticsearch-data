package com.pgoogol.elasticsearch.data.repository;

import javax.validation.constraints.NotBlank;

public interface ElasticsearchIndiciesRepository {

    boolean create(@NotBlank String dictionaryName);
    boolean exists(@NotBlank String dictionaryName);

}
