package com.myblog.image.service;

import java.util.List;

/**
 * 기록을 지운 이미지의 파일을 지울 차례다 (D-5 A: DB 먼저, 파일은 커밋 뒤). 이미지 모듈 안에서만 쓴다.
 * 트랜잭션이 취소되면 파일은 그대로 남는다 (기록도 남으므로 맞다).
 */
record ImageFilesReleased(List<String> storageKeys) {
}
