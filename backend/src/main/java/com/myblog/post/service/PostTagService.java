package com.myblog.post.service;

import com.myblog.post.PostDeletingEvent;
import com.myblog.post.domain.Tag;
import com.myblog.post.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글의 태그 저장 (specs/005 T044, FR-013, FR-014, FR-016, FR-028).
 * 글 쓰기·수정의 트랜잭션 안에서 부른다: 태그 저장이 실패하면 글 저장도 취소된다.
 * 받는 목록은 TagNormalizer가 다듬은 값(소문자, 겹침 없음)이다.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PostTagService {

    private final TagRepository tags;

    public PostTagService(TagRepository tags) {
        this.tags = tags;
    }

    /**
     * 보낸 목록을 이 글의 <b>새 전체 목록</b>으로 만든다: 빠진 연결은 지우고 새 이름은 붙인다.
     * 없는 태그는 새로 만든다. 바뀐 것이 있었는지를 돌려준다 (태그만 바꿔도 "수정됨", 2026-10-08 가안).
     */
    public boolean replaceTags(Long postId, List<String> names) {
        Set<String> current = new HashSet<>(tags.findNamesByPostId(postId));
        Set<String> wanted = new HashSet<>(names);
        Set<String> removed = new HashSet<>(current);
        removed.removeAll(wanted);
        Set<String> added = new HashSet<>(wanted);
        added.removeAll(current);
        if (!removed.isEmpty()) {
            tags.unlink(postId, removed);
        }
        if (!added.isEmpty()) {
            added.forEach(tags::insertIfAbsent);
            Map<String, Long> ids = tags.findByNameIn(added).stream()
                    .collect(Collectors.toMap(Tag::getName, Tag::getId, (a, b) -> a));
            added.forEach(name -> tags.link(postId, ids.get(name)));
        }
        return !removed.isEmpty() || !added.isEmpty();
    }

    /** 글의 태그, 이름순 (글 상세, 수정 화면). */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<String> tagsOf(Long postId) {
        return tags.findNamesByPostId(postId);
    }

    /** 이 글의 태그가 이 목록과 같은가 (같은 요청 번호로 다시 온 글쓰기가 처음과 같은지 볼 때). */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean sameTags(Long postId, List<String> names) {
        return new HashSet<>(tags.findNamesByPostId(postId)).equals(names.stream().collect(Collectors.toSet()));
    }

    /**
     * 글이 지워지면 그 글의 태그 연결을 먼저 지운다 (FR-028, data-model 7). 같은 트랜잭션 안에서 듣는다.
     * 탈퇴로 블로그가 닫힐 때도 글마다 이 이벤트가 온다. 태그 줄은 남긴다.
     */
    @EventListener
    public void on(PostDeletingEvent event) {
        tags.unlinkAll(event.postId());
    }
}
