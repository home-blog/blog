package com.myblog.community.validation;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import com.myblog.community.config.ReportProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 신고 설명 오류 문구의 글자 수를 설정값에 맞춘다 (specs/005 T036). */
@Component
public class ReportFieldErrorMessages implements FieldErrorMessages {

    private final ReportProperties properties;

    public ReportFieldErrorMessages(ReportProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        if (code == ErrorCode.REPORT_DETAIL_TOO_LONG) {
            return Optional.of("※ 신고 내용은 %d자 이하로 입력해 주세요".formatted(properties.detailMaxLength()));
        }
        return Optional.empty();
    }
}
