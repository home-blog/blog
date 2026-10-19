package com.myblog.post.related;

import com.myblog.post.config.RelatedProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Voyage AI 임베딩 (specs/014 D-10). 공식 Java SDK가 없어 HTTP로 부른다.
 * 글은 {@code input_type: document}, 차원은 {@code output_dimension}으로 고정한다(저장 칸 길이와 같아야 한다).
 */
public class VoyageEmbedder implements Embedder {

    private static final Logger log = LoggerFactory.getLogger(VoyageEmbedder.class);

    private final RestClient restClient;
    private final RelatedProperties properties;
    private final JsonMapper jsonMapper;

    public VoyageEmbedder(RelatedProperties properties, JsonMapper jsonMapper) {
        this(RestClient.builder().requestFactory(timeouts(properties)).build(), properties, jsonMapper);
    }

    VoyageEmbedder(RestClient restClient, RelatedProperties properties, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    /** 모델 이름 + 차원 (예: voyage-3.5@1024). 차원만 바꿔도 모두 다시 만든다. */
    @Override
    public String model() {
        return properties.model() + "@" + properties.dimensions();
    }

    @Override
    public boolean available() {
        return true;
    }

    record Response(List<Item> data) {
    }

    record Item(List<Double> embedding, int index) {
    }

    @Override
    public List<float[]> embed(List<String> documents) {
        String body;
        try {
            body = restClient.post()
                    .uri(properties.baseUrl() + "/embeddings")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonMapper.writeValueAsString(Map.of(
                            "input", documents,
                            "model", properties.model(),
                            "input_type", "document",
                            "output_dimension", properties.dimensions())))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("비슷한 글: Voyage 임베딩 호출 실패 ({})", e.getClass().getSimpleName());
            throw new UnavailableException("Voyage 임베딩 호출 실패", e);
        }
        return read(body, documents.size());
    }

    private List<float[]> read(String body, int expected) {
        Response response;
        try {
            response = jsonMapper.readValue(body, Response.class);
        } catch (JacksonException e) {
            throw new UnavailableException("Voyage 임베딩 답을 읽지 못했습니다", e);
        }
        if (response.data() == null || response.data().size() != expected) {
            throw new UnavailableException("Voyage 임베딩 답의 개수가 다릅니다");
        }
        List<Item> items = new ArrayList<>(response.data());
        items.sort(Comparator.comparingInt(Item::index));
        List<float[]> vectors = new ArrayList<>();
        for (Item item : items) {
            if (item.embedding() == null || item.embedding().size() != properties.dimensions()) {
                throw new UnavailableException("Voyage 임베딩 차원이 설정과 다릅니다");
            }
            float[] vector = Vectors.usable(item.embedding());
            if (vector == null) {
                throw new UnavailableException("Voyage 임베딩 답에 쓸 수 없는 숫자(빈 값·무한대·모두 0)가 있습니다");
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private static SimpleClientHttpRequestFactory timeouts(RelatedProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());
        return factory;
    }
}
