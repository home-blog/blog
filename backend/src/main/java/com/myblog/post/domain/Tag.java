package com.myblog.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 태그 (tag 표, specs/005 data-model 3). 이름은 소문자로 저장한다 (D-10 A). 글이 지워져도 태그 줄은 남는다.
 * 줄은 TagRepository.insertIfAbsent로 넣는다 (동시에 같은 새 태그가 와도 하나만). 엔티티는 읽기와 표 확인(validate)용이다.
 */
@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 15, unique = true, updatable = false)
    private String name;

    protected Tag() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
