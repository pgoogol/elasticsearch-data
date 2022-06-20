package com.pgoogol.elasticsearch.data.repository;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.pgoogol.elasticsearch.data.model.IndexModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MappingIndexUtils {

    static Map<String, Property> prepareMapping(List<IndexModel> indexModels) {
        Map<String, Property> propertyMap = new HashMap<>();
        indexModels.forEach(indexModel -> {
            propertyMap.put(indexModel.getField(), prepareProperty(indexModel));
        });
        return propertyMap;
    }

    private static Property prepareProperty(IndexModel indexModel) {
        switch (indexModel.getFieldType().toLowerCase()) {
            case "boolean":
                return new Property.Builder().boolean_(builder -> builder).build();
            case "array":
                return new Property.Builder().nested(builder -> builder.properties(prepareMapping(indexModel.getItems()))).build();
            case "object":
                return new Property.Builder().object(builder -> builder.properties(prepareMapping(indexModel.getProperties()))).build();
            case "long":
                return new Property.Builder().long_(builder -> builder).build();
            case "text":
            default:
                return new Property.Builder().text(builder -> builder).build();
        }
    }

}
