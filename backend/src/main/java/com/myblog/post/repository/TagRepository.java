package com.myblog.post.repository;

import com.myblog.post.domain.Tag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 태그와 글-태그 연결 (specs/005 T044). post_tag는 칸이 두 개뿐이라 엔티티 없이 이 저장소의 쿼리로만 다룬다.
 * 값은 파라미터로만 넘긴다.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByNameIn(Collection<String> names);

    /** 없을 때만 넣는다. 같은 새 태그가 거의 동시에 와도 UNIQUE(name)가 하나만 받고 나머지는 조용히 넘어간다 (research B-6). */
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into tag (name) values (:name) on conflict (name) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("name") String name);

    /** 글의 태그, 이름순 (DB 언어 설정과 상관없이 글자 코드 순). */
    @Query(value = "select t.name from post_tag pt join tag t on t.tag_id = pt.tag_id where pt.post_id = :postId"
            + " order by t.name collate \"C\"", nativeQuery = true)
    List<String> findNamesByPostId(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query(value = "insert into post_tag (post_id, tag_id) values (:postId, :tagId) on conflict do nothing", nativeQuery = true)
    int link(@Param("postId") Long postId, @Param("tagId") Long tagId);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from post_tag where post_id = :postId and tag_id in (select tag_id from tag where name in (:names))",
            nativeQuery = true)
    int unlink(@Param("postId") Long postId, @Param("names") Collection<String> names);

    /** 글이 지워질 때 연결을 모두 지운다. tag 줄은 남긴다. */
    @Modifying(flushAutomatically = true)
    @Query(value = "delete from post_tag where post_id = :postId", nativeQuery = true)
    int unlinkAll(@Param("postId") Long postId);
}
