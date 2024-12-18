package com.arangodb.internal.serde;

import com.arangodb.entity.CollectionStatus;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.InvertedIndexPrimarySort;
import com.arangodb.entity.ReplicationFactor;
import com.arangodb.util.RawJson;
import com.arangodb.internal.InternalRequest;
import com.arangodb.internal.InternalResponse;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.function.Supplier;

enum InternalModule implements Supplier<Module> {
    INSTANCE;

    private final SimpleModule module;

    InternalModule() {
        module = new SimpleModule();

        module.addSerializer(RawJson.class, InternalSerializers.RAW_JSON_SERIALIZER);
        module.addSerializer(InternalRequest.class, InternalSerializers.REQUEST);
        module.addSerializer(CollectionType.class, InternalSerializers.COLLECTION_TYPE);

        module.addDeserializer(RawJson.class, InternalDeserializers.RAW_JSON_DESERIALIZER);
        module.addDeserializer(CollectionStatus.class, InternalDeserializers.COLLECTION_STATUS);
        module.addDeserializer(CollectionType.class, InternalDeserializers.COLLECTION_TYPE);
        module.addDeserializer(ReplicationFactor.class, InternalDeserializers.REPLICATION_FACTOR);
        module.addDeserializer(InternalResponse.class, InternalDeserializers.RESPONSE);
        module.addDeserializer(InvertedIndexPrimarySort.Field.class, InternalDeserializers.INVERTED_INDEX_PRIMARY_SORT_FIELD);
    }

    @Override
    public Module get() {
        return module;
    }

}
