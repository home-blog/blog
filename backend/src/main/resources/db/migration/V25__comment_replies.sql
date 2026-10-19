-- 답글·비밀 댓글 (specs/019 data-model, ERD E-16). 005 D-7이 "칸만 두고 쓰지 않는다"고 한 parent_id·is_secret을 쓰기 시작한다.
--   parent_id는 늘 원댓글(부모가 없는 댓글)을 가리킨다: 답글은 한 단계만 (D-1).
--   deleted_at: 답글이 남은 원댓글을 지우면 줄을 남기고 내용을 비운 "지운 칸"이 된다 (D-2). 마지막 답글이 지워지면 함께 지운다.
--   notification.kind에 reply(내 댓글에 답글)를 더한다 (D-4).

ALTER TABLE comment ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE comment ADD CONSTRAINT ck_comment_parent_not_self CHECK (parent_id <> comment_id);
CREATE INDEX idx_comment_parent ON comment (parent_id) WHERE parent_id IS NOT NULL;

COMMENT ON COLUMN comment.parent_id IS '원댓글 번호 (답글이면). 답글은 한 단계만';
COMMENT ON COLUMN comment.is_secret IS '비밀 댓글: 쓴 사람·블로그 주인·원댓글 쓴 사람만 읽는다';
COMMENT ON COLUMN comment.deleted_at IS '지운 칸: 답글이 남은 원댓글을 지운 시각. 내용은 빈 글자';

ALTER TABLE notification DROP CONSTRAINT ck_notification_kind;
ALTER TABLE notification ADD CONSTRAINT ck_notification_kind
    CHECK (kind IN ('post', 'comment', 'board', 'accepted', 'editorial', 'reply'));
COMMENT ON COLUMN notification.kind IS 'post(받아 보는 블로그의 새 글) · comment(내 글에 달린 댓글) · board(내 글마당 글에 달린 댓글) · accepted(내 글마당 댓글이 채택됨) · editorial(편집실이 내 글을 내리거나 지움) · reply(내 댓글에 답글)';
