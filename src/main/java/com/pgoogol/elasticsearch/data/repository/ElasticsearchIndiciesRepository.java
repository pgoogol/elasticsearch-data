package com.pgoogol.elasticsearch.data.repository;

import com.pgoogol.elasticsearch.data.model.IndexModel;

import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;

public interface ElasticsearchIndiciesRepository {

    default boolean create(@NotBlank String dictionaryName) {
        return create(dictionaryName, Collections.emptyList());
    }
    boolean create(@NotBlank String dictionaryName, List<IndexModel> modelDictionary);
    boolean exists(@NotBlank String dictionaryName);

}
