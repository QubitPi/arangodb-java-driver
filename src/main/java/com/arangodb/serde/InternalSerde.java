package com.arangodb.serde;

import com.arangodb.jackson.dataformat.velocypack.VPackMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

public interface InternalSerde extends JacksonSerde {

    /**
     * Creates a new InternalSerde with default settings for the specified data type.
     *
     * @param dataType serialization target data type
     * @return the created InternalSerde
     */
    static InternalSerde of(final DataType dataType) {
        if (dataType == DataType.JSON) {
            return new InternalSerdeImpl(new ObjectMapper());
        } else if (dataType == DataType.VPACK) {
            return new InternalSerdeImpl(new VPackMapper());
        } else {
            throw new IllegalArgumentException("Unexpected value: " + dataType);
        }
    }

    /**
     * Used for logging and debugging.
     *
     * @param content byte array
     * @return JSON string
     */
    String toJsonString(byte[] content);

    /**
     * Extract the nested content pointed by the json pointer.
     * Used for extracting nested user data.
     *
     * @param content     byte array
     * @param jsonPointer location of data to be extracted
     * @return byte array
     */
    byte[] extract(byte[] content, String jsonPointer);

    /**
     * Deserializes the parsed json node and binds it to the target data type.
     *
     * @param node  parsed json node
     * @param clazz class of target data type
     * @return deserialized object
     */
    default <T> T deserialize(JsonNode node, Class<T> clazz) {
        return deserialize(node, (Type) clazz);
    }

    /**
     * Deserializes the parsed json node and binds it to the target data type.
     *
     * @param node parsed json node
     * @param type target data type
     * @return deserialized object
     */
    <T> T deserialize(JsonNode node, Type type);

    /**
     * Parses the content.
     *
     * @param content VPack or byte encoded JSON string
     * @return root of the parsed tree
     */
    JsonNode parse(byte[] content);

    /**
     * Parses the content at json pointer.
     *
     * @param content     VPack or byte encoded JSON string
     * @param jsonPointer location of data to be parsed
     * @return root of the parsed tree
     */
    JsonNode parse(byte[] content, String jsonPointer);

    /**
     * Deserializes the content at json pointer and binds it to the target data type.
     * For data type {@link DataType#JSON}, the byte array is the JSON string encoded using the UTF-8 charset.
     *
     * @param content     byte array to deserialize
     * @param jsonPointer location of data to be deserialized
     * @param clazz       class of target data type
     * @return deserialized object
     */
    default <T> T deserialize(byte[] content, String jsonPointer, Class<T> clazz) {
        return deserialize(content, jsonPointer, (Type) clazz);
    }

    /**
     * Deserializes the content at json pointer and binds it to the target data type.
     * For data type {@link DataType#JSON}, the byte array is the JSON string encoded using the UTF-8 charset.
     *
     * @param content     byte array to deserialize
     * @param jsonPointer location of data to be deserialized
     * @param type        target data type
     * @return deserialized object
     */
    default <T> T deserialize(byte[] content, String jsonPointer, Type type) {
        return deserialize(parse(content, jsonPointer), type);
    }

}
