package com.myblog.common.error;

import java.util.Optional;

/**
 * 칸별 오류 문구를 기능 모듈이 바꿔 넣을 수 있게 한다.
 * 예: 닉네임 글자 수처럼 설정값(application.yml)에 따라 문구의 숫자가 바뀌는 경우.
 * 없으면 ErrorCode의 기본 문구를 쓴다.
 */
public interface FieldErrorMessages {

    Optional<String> messageFor(ErrorCode code);
}
