package com.myblog.post.related;

import com.myblog.post.config.RelatedProperties;
import java.util.ArrayList;
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
 * 내 서버의 Ollama 임베딩 (specs/014 D-17): 돈이 들지 않고 글을 밖으로 보내지 않는다. 모델은 bge-m3(다국어, 1024칸).
 * 배포는 서버 자원이 넉넉할 때만 Ollama 컨테이너를 켜고 주소(related.ollama-url)를 넣는다. 모델을 아직 받는 중이거나
 * 컨테이너가 멈추면 호출이 실패하고, 정리 작업이 연달아 실패를 보고 멈췄다가 다음 차례에 다시 해 본다.
 */
public class OllamaEmbedder implements Embedder {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbedder.class);

    private final RestClient restClient;
    private final RelatedProperties properties;
    private final JsonMapper jsonMapper;

    public OllamaEmbedder(RelatedProperties properties, JsonMapper jsonMapper) {
        this(RestClient.builder().requestFactory(timeouts(properties)).build(), properties, jsonMapper);
    }

    OllamaEmbedder(RestClient restClient, RelatedProperties properties, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    /** ollama-모델@차원 (예: ollama-bge-m3@1024). 글자 조각이나 Voyage에서 바꾸면 모두 다시 만든다. */
    @Override
    public String model() {
        return "ollama-" + properties.ollamaModel() + "@" + properties.dimensions();
    }

    @Override
    public boolean available() {
        return true;
    }

    record Response(List<List<Double>> embeddings) {
    }

    @Override
    public List<float[]> embed(List<String> documents) {
        String body;
        try {
            body = restClient.post()
                    .uri(properties.ollamaUrl() + "/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonMapper.writeValueAsString(Map.of(
                            "model", properties.ollamaModel(),
                            "input", documents,
                            "truncate", true)))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("비슷한 글: Ollama 임베딩 호출 실패 ({})", e.getClass().getSimpleName());
            throw new UnavailableException("Ollama 임베딩 호출 실패", e);
        }
        return read(body, documents.size());
    }

    private List<float[]> read(String body, int expected) {
        Response response;
        try {
            response = jsonMapper.readValue(body, Response.class);
        } catch (JacksonException e) {
            throw new UnavailableException("Ollama 임베딩 답을 읽지 못했습니다", e);
        }
        if (response == null || response.embeddings() == null || response.embeddings().size() != expected) {
            throw new UnavailableException("Ollama 임베딩 답의 개수가 다릅니다");
        }
        List<float[]> vectors = new ArrayList<>();
        for (List<Double> embedding : response.embeddings()) {
            if (embedding == null || embedding.size() != properties.dimensions()) {
                throw new UnavailableException("Ollama 임베딩 차원이 설정과 다릅니다");
            }
            float[] vector = Vectors.usable(embedding);
            if (vector == null) {
                throw new UnavailableException("Ollama 임베딩 답에 쓸 수 없는 숫자(빈 값·무한대·모두 0)가 있습니다");
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private static SimpleClientHttpRequestFactory timeouts(RelatedProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.ollamaTimeout());
        return factory;
    }
}
