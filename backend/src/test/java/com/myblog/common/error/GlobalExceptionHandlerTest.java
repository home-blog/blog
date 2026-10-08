package com.myblog.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 저장소(DB, Redis)가 응답하지 않으면 500이 아니라 503 SERVICE_UNAVAILABLE로 답한다 (quickstart S-10).
 * 숫자 자리에 글자가 오면 500이 아니라, 주소 안의 번호면 404, 물음표 뒤의 값이면 400이다.
 */
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

    @Test
    void 프레임워크가_막은_큰_파일도_설정값에_맞춘_문구로_400() {
        FieldErrorMessages image = code -> code == ErrorCode.INVALID_IMAGE
                ? Optional.of("이미지는 3MB 이하의 png만 올릴 수 있습니다") : Optional.empty();
        var response = new GlobalExceptionHandler(List.of(image)).handleTooLarge(new MaxUploadSizeExceededException(1));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INVALID_IMAGE.name());
        assertThat(response.getBody().message()).isEqualTo("이미지는 3MB 이하의 png만 올릴 수 있습니다");
    }

    @Test
    void 주소_안의_번호에_글자가_오면_404() throws Exception {
        var response = handler.handleTypeMismatch(mismatch(0));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    void 물음표_뒤의_숫자_자리에_글자가_오면_400() throws Exception {
        var response = handler.handleTypeMismatch(mismatch(1));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
    }

    private static MethodArgumentTypeMismatchException mismatch(int parameterIndex) throws Exception {
        Method method = Sample.class.getDeclaredMethod("list", Long.class, Integer.class);
        MethodParameter parameter = new MethodParameter(method, parameterIndex);
        return new MethodArgumentTypeMismatchException("abc", Long.class, "x", parameter, new NumberFormatException());
    }

    /** 주소 안의 번호(@PathVariable)와 물음표 뒤의 값(@RequestParam)을 가진 예시 주소. */
    static class Sample {

        void list(@PathVariable Long blogId, @RequestParam Integer page) {
        }
    }
}
