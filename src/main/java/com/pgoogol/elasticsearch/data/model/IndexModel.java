package com.pgoogol.elasticsearch.data.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class IndexModel {

    private String field;

    private String fieldType;

    private List<IndexModel> items = Collections.emptyList();

    private List<IndexModel> properties = Collections.emptyList();
}
