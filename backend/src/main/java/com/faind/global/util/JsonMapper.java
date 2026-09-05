package com.faind.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JsonMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonMapper() {}

  public static String toJson(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("JSON 직렬화에 실패했습니다.", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> toMap(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return OBJECT_MAPPER.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("JSON 역직렬화에 실패했습니다.", e);
    }
  }
}
