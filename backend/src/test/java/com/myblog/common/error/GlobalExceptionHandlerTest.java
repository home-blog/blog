package com.myblog.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;

/** 저장소(DB, Redis)가 응답하지 않으면 500이 아니라 503 SERVICE_UNAVAILABLE로 답한다 (quickstart S-10). */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(List.of());

    @Test
    void 연결할_수_없으면_503() {
        var response = handler.handleUnavailable(new DataAccessResourceFailureException("down"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.name());
    }

    @Test
    void 연결된_채로_Redis가_꺼져_시간_초과가_나도_503() {
        var response = handler.handleUnavailable(new QueryTimeoutException("Redis command timed out"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.name());
    }
}
