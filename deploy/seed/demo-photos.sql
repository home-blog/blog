-- 예시 글 사진 (자동 생성: deploy/seed/build_seed.py, 원본 deploy/seed/photos.json). 손으로 고치지 않는다
-- demo-content.sql 다음에 실행한다. 몇 번 실행해도 같다 (처음 모양 그대로인 글만 바꾼다)
\set ON_ERROR_STOP on
\if :{?schema}
\else
\set schema public
\endif
set search_path = :"schema";
begin;
do $$ begin
  if not exists (select 1 from users where password = '!demo-account-no-login' and email like 'demo.%@example.com') then
    raise exception '예시 데이터가 없습니다. 먼저 demo-content.sql을 실행하세요';
  end if;
end $$;

update post set content = '지난주 화요일 점심시간에 정산 조회 API가 30초씩 멈추다가 500을 뱉었다. 로그에 찍힌 건 이거였다.

![책상 위 노트북](https://picsum.photos/id/0/1200/800)

```text
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

## 무슨 뜻인가

HikariCP 풀에서 커넥션을 빌리려고 했는데, **30초(`connectionTimeout` 기본값) 동안 남는 커넥션이 없었다**는 뜻이다. 풀 크기는 기본값 10 그대로였다.

DB가 느린 건지, 커넥션을 누가 오래 쥐고 있는 건지부터 나눠야 했다. DB 쪽 `pg_stat_activity`를 보니 우리 앱 커넥션 대부분이 `idle in transaction` 상태였다. **트랜잭션을 연 채로 아무것도 안 하고 있다**는 뜻.

## 원인

```java
@Transactional
public SettlementResponse confirm(Long settlementId) {
    Settlement settlement = settlementRepository.findById(settlementId).orElseThrow();
    settlement.confirm();

    // 외부 송금 API 호출 (여기서 몇 초씩 걸림)
    TransferResult result = transferClient.request(settlement.toTransferRequest());

    settlement.markTransferred(result.transactionId());
    return SettlementResponse.from(settlement);
}
```

트랜잭션 안에서 **외부 API를 호출**하고 있었다. 평소엔 200ms 정도라 문제가 없었는데, 그날 외부 API가 느려지면서 호출 하나가 5~10초씩 걸렸다. 그동안 커넥션을 쥐고 있으니 점심 트래픽에 10개가 금방 바닥났다.

## 대응

### 당일
외부 API 호출에 **타임아웃**이 아예 없었다. 급하게 연결/응답 타임아웃을 넣어 배포했다.

### 근본 수정
트랜잭션을 두 개로 쪼개고, 외부 호출은 트랜잭션 **밖**으로 뺐다.

```java
public SettlementResponse confirm(Long settlementId) {
    TransferRequest request = settlementTxService.confirm(settlementId);   // 트랜잭션 1
    TransferResult result = transferClient.request(request);               // 트랜잭션 밖
    return settlementTxService.markTransferred(settlementId, result);      // 트랜잭션 2
}
```

`settlementTxService`는 별도 빈이다. 같은 클래스에 두면 내부 호출이라 `@Transactional`이 안 먹는다는 건 1년 차 때 이미 배웠다.

중간에 실패하면 "확정됐지만 송금 안 된" 상태가 남으니까, 그 상태를 조회해서 재처리하는 배치를 같이 만들었다.

## 같이 켠 설정

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 5000
  jpa:
    open-in-view: false
```

- `leak-detection-threshold`: 커넥션을 5초 넘게 쥐고 있으면 **빌려 간 위치의 스택 트레이스**를 경고로 남긴다. 다음엔 원인을 훨씬 빨리 찾을 수 있다.
- `open-in-view`: 켜져 있으면 영속성 컨텍스트가 요청 끝까지 살아 있어서, DB 커넥션을 처음 쓴 뒤 **응답이 나갈 때까지** 쥐고 있을 수 있다. 이번 원인은 아니었지만 같이 껐다. 대신 지연 로딩이 컨트롤러에서 터지는 곳 두 군데를 고쳐야 했다.

## 배운 것

- 풀 크기를 늘리는 건 답이 아니었다. 20으로 늘렸으면 20개가 다 묶였을 거다.
- 트랜잭션 안에는 **DB 일만** 둔다.
- 외부 호출에는 **반드시 타임아웃**.

> 30초 타임아웃 로그 한 줄 뒤에, 타임아웃 없는 외부 호출이 숨어 있었다.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길') and content = '지난주 화요일 점심시간에 정산 조회 API가 30초씩 멈추다가 500을 뱉었다. 로그에 찍힌 건 이거였다.

```text
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

## 무슨 뜻인가

HikariCP 풀에서 커넥션을 빌리려고 했는데, **30초(`connectionTimeout` 기본값) 동안 남는 커넥션이 없었다**는 뜻이다. 풀 크기는 기본값 10 그대로였다.

DB가 느린 건지, 커넥션을 누가 오래 쥐고 있는 건지부터 나눠야 했다. DB 쪽 `pg_stat_activity`를 보니 우리 앱 커넥션 대부분이 `idle in transaction` 상태였다. **트랜잭션을 연 채로 아무것도 안 하고 있다**는 뜻.

## 원인

```java
@Transactional
public SettlementResponse confirm(Long settlementId) {
    Settlement settlement = settlementRepository.findById(settlementId).orElseThrow();
    settlement.confirm();

    // 외부 송금 API 호출 (여기서 몇 초씩 걸림)
    TransferResult result = transferClient.request(settlement.toTransferRequest());

    settlement.markTransferred(result.transactionId());
    return SettlementResponse.from(settlement);
}
```

트랜잭션 안에서 **외부 API를 호출**하고 있었다. 평소엔 200ms 정도라 문제가 없었는데, 그날 외부 API가 느려지면서 호출 하나가 5~10초씩 걸렸다. 그동안 커넥션을 쥐고 있으니 점심 트래픽에 10개가 금방 바닥났다.

## 대응

### 당일
외부 API 호출에 **타임아웃**이 아예 없었다. 급하게 연결/응답 타임아웃을 넣어 배포했다.

### 근본 수정
트랜잭션을 두 개로 쪼개고, 외부 호출은 트랜잭션 **밖**으로 뺐다.

```java
public SettlementResponse confirm(Long settlementId) {
    TransferRequest request = settlementTxService.confirm(settlementId);   // 트랜잭션 1
    TransferResult result = transferClient.request(request);               // 트랜잭션 밖
    return settlementTxService.markTransferred(settlementId, result);      // 트랜잭션 2
}
```

`settlementTxService`는 별도 빈이다. 같은 클래스에 두면 내부 호출이라 `@Transactional`이 안 먹는다는 건 1년 차 때 이미 배웠다.

중간에 실패하면 "확정됐지만 송금 안 된" 상태가 남으니까, 그 상태를 조회해서 재처리하는 배치를 같이 만들었다.

## 같이 켠 설정

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 5000
  jpa:
    open-in-view: false
```

- `leak-detection-threshold`: 커넥션을 5초 넘게 쥐고 있으면 **빌려 간 위치의 스택 트레이스**를 경고로 남긴다. 다음엔 원인을 훨씬 빨리 찾을 수 있다.
- `open-in-view`: 켜져 있으면 영속성 컨텍스트가 요청 끝까지 살아 있어서, DB 커넥션을 처음 쓴 뒤 **응답이 나갈 때까지** 쥐고 있을 수 있다. 이번 원인은 아니었지만 같이 껐다. 대신 지연 로딩이 컨트롤러에서 터지는 곳 두 군데를 고쳐야 했다.

## 배운 것

- 풀 크기를 늘리는 건 답이 아니었다. 20으로 늘렸으면 20개가 다 묶였을 거다.
- 트랜잭션 안에는 **DB 일만** 둔다.
- 외부 호출에는 **반드시 타임아웃**.

> 30초 타임아웃 로그 한 줄 뒤에, 타임아웃 없는 외부 호출이 숨어 있었다.
';
update post set content = '매년 연말에 쓰다가 올해는 바빠서 새해 첫 일요일에 쓴다. 2월이면 3년 차가 된다.

![작업하던 노트북](https://picsum.photos/id/2/1200/800)

## 올해 한 일

- 주문 서비스에서 **정산 도메인**으로 옮겼다. 돈이 걸린 코드는 무게가 다르다.
- 통합 테스트를 Testcontainers로 옮기는 작업을 주도했다.
- 신입 두 명 온보딩을 맡았다.
- 블로그 글 11개. 목표(한 달 하나)는 아깝게 실패.

## 달라진 것

### 장애를 대하는 태도
1년 차 때는 장애가 나면 일단 겁부터 났다. 올해 데드락 건을 겪으면서 **로그를 처음부터 끝까지 읽는** 습관이 생겼다. 무섭다고 대충 재시작부터 하면 같은 일이 다시 난다.

### 리뷰에서 하는 말
예전엔 "이거 왜 이렇게 했어요?"가 무서운 질문이었는데, 이제는 내가 신입한테 하는 질문이 됐다. 대신 **"이렇게 하면 이런 경우가 걱정돼요"** 처럼 이유를 같이 붙이려고 한다. 1년 차 때 그 질문에 대답 못 했던 기억이 있어서.

### 코드보다 문서
정산 로직은 기획서보다 코드가 진실인 경우가 많았다. 그래서 규칙을 하나 찾을 때마다 위키에 표로 정리했다. 연말에 다른 팀이 그 표를 보고 질문 없이 연동을 끝냈다는 얘기를 듣고 꽤 뿌듯했다.

## 아쉬운 것

- 건강. 작년보다 앉아 있는 시간이 확실히 늘었다. 허리가 신호를 보낸다.
- 새 기술을 거의 못 만졌다. 회사 일만으로 한 해가 지나간 느낌.

## 2026 목표

1. 운영 지표 보는 법 공부: 커넥션 풀, 응답 시간 분포, GC
2. 사이드로 작은 서비스 하나 끝까지 배포해 보기
3. 주 3회 운동 (이웃 블로그 홈트 루틴 따라 하는 중)
4. 글은 개수 말고 **제대로 된 삽질기** 6개

> 3년 차에 필요한 건 더 많이 아는 것보다, 모르는 걸 **빨리 좁혀 가는 방법**인 것 같다.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들') and content = '매년 연말에 쓰다가 올해는 바빠서 새해 첫 일요일에 쓴다. 2월이면 3년 차가 된다.

## 올해 한 일

- 주문 서비스에서 **정산 도메인**으로 옮겼다. 돈이 걸린 코드는 무게가 다르다.
- 통합 테스트를 Testcontainers로 옮기는 작업을 주도했다.
- 신입 두 명 온보딩을 맡았다.
- 블로그 글 11개. 목표(한 달 하나)는 아깝게 실패.

## 달라진 것

### 장애를 대하는 태도
1년 차 때는 장애가 나면 일단 겁부터 났다. 올해 데드락 건을 겪으면서 **로그를 처음부터 끝까지 읽는** 습관이 생겼다. 무섭다고 대충 재시작부터 하면 같은 일이 다시 난다.

### 리뷰에서 하는 말
예전엔 "이거 왜 이렇게 했어요?"가 무서운 질문이었는데, 이제는 내가 신입한테 하는 질문이 됐다. 대신 **"이렇게 하면 이런 경우가 걱정돼요"** 처럼 이유를 같이 붙이려고 한다. 1년 차 때 그 질문에 대답 못 했던 기억이 있어서.

### 코드보다 문서
정산 로직은 기획서보다 코드가 진실인 경우가 많았다. 그래서 규칙을 하나 찾을 때마다 위키에 표로 정리했다. 연말에 다른 팀이 그 표를 보고 질문 없이 연동을 끝냈다는 얘기를 듣고 꽤 뿌듯했다.

## 아쉬운 것

- 건강. 작년보다 앉아 있는 시간이 확실히 늘었다. 허리가 신호를 보낸다.
- 새 기술을 거의 못 만졌다. 회사 일만으로 한 해가 지나간 느낌.

## 2026 목표

1. 운영 지표 보는 법 공부: 커넥션 풀, 응답 시간 분포, GC
2. 사이드로 작은 서비스 하나 끝까지 배포해 보기
3. 주 3회 운동 (이웃 블로그 홈트 루틴 따라 하는 중)
4. 글은 개수 말고 **제대로 된 삽질기** 6개

> 3년 차에 필요한 건 더 많이 아는 것보다, 모르는 걸 **빨리 좁혀 가는 방법**인 것 같다.
';
update post set content = '정산 배치가 새벽에 가끔 실패한다는 알림이 왔다. 로그를 보니 이런 게 있었다.

![책상 위 노트와 휴대폰](https://picsum.photos/id/4/1200/800)

```text
ERROR:  deadlock detected
DETAIL:  Process 21843 waits for ShareLock on transaction 9817734; blocked by process 21851.
        Process 21851 waits for ShareLock on transaction 9817731; blocked by process 21843.
HINT:  See server log for query details.
```

## 로그 읽기

- **21843**은 트랜잭션 9817734가 끝나길 기다린다. 그 트랜잭션은 21851의 것.
- **21851**은 트랜잭션 9817731이 끝나길 기다린다. 그건 21843의 것.

서로가 서로를 기다리는 원형 대기. PostgreSQL은 이걸 감지하면 **둘 중 하나를 에러로 끝내서** 풀어 준다. 그래서 하나는 실패하고 하나는 성공한다.

`ShareLock on transaction`은 보통 **다른 트랜잭션이 잡고 있는 행을 수정하려고 기다릴 때** 나온다. 행 잠금을 쥔 트랜잭션이 끝나기를 기다리는 것.

서버 로그 쪽에는 각 프로세스가 실행하던 쿼리가 같이 찍혀 있어서 그걸로 범인을 찾았다.

## 원인

두 배치 작업이 같은 계좌 잔액을 갱신하는데, **순서가 달랐다**.

- 작업 A: 계좌 1 → 계좌 2 순서로 `UPDATE`
- 작업 B: 계좌 2 → 계좌 1 순서로 `UPDATE`

A가 1을 잡고, B가 2를 잡은 상태에서 서로 상대 것을 기다리면 끝.

## 해결: 잠그는 순서를 통일

처리할 행을 **항상 같은 순서로** 먼저 잠그게 바꿨다.

```sql
SELECT id
FROM account
WHERE id IN (:ids)
ORDER BY id
FOR UPDATE;
```

JPA에서는 이렇게 썼다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id in :ids order by a.id")
List<Account> findAllForUpdate(@Param("ids") Collection<Long> ids);
```

id 오름차순으로 잠그니까 두 작업이 같은 행을 노려도 한쪽이 그냥 기다리기만 하고 원형 대기는 생기지 않는다.

## 같이 손본 설정

- `log_lock_waits = on`: `deadlock_timeout`(기본 1초)보다 오래 잠금을 기다리면 로그를 남긴다. 데드락까지 안 가도 **잠금 대기 자체**를 볼 수 있다.
- 배치 쪽에는 데드락 에러 시 **짧게 한 번 재시도**를 넣었다. 순서를 맞췄어도 다른 경로에서 생길 수 있으니 안전장치로.
- 트랜잭션 범위를 줄였다. 잠금을 오래 쥘수록 부딪칠 확률이 커진다.

> 데드락은 DB가 이상한 게 아니라, 내 코드가 **서로 다른 순서로** 문을 잠근 결과였다.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다') and content = '정산 배치가 새벽에 가끔 실패한다는 알림이 왔다. 로그를 보니 이런 게 있었다.

```text
ERROR:  deadlock detected
DETAIL:  Process 21843 waits for ShareLock on transaction 9817734; blocked by process 21851.
        Process 21851 waits for ShareLock on transaction 9817731; blocked by process 21843.
HINT:  See server log for query details.
```

## 로그 읽기

- **21843**은 트랜잭션 9817734가 끝나길 기다린다. 그 트랜잭션은 21851의 것.
- **21851**은 트랜잭션 9817731이 끝나길 기다린다. 그건 21843의 것.

서로가 서로를 기다리는 원형 대기. PostgreSQL은 이걸 감지하면 **둘 중 하나를 에러로 끝내서** 풀어 준다. 그래서 하나는 실패하고 하나는 성공한다.

`ShareLock on transaction`은 보통 **다른 트랜잭션이 잡고 있는 행을 수정하려고 기다릴 때** 나온다. 행 잠금을 쥔 트랜잭션이 끝나기를 기다리는 것.

서버 로그 쪽에는 각 프로세스가 실행하던 쿼리가 같이 찍혀 있어서 그걸로 범인을 찾았다.

## 원인

두 배치 작업이 같은 계좌 잔액을 갱신하는데, **순서가 달랐다**.

- 작업 A: 계좌 1 → 계좌 2 순서로 `UPDATE`
- 작업 B: 계좌 2 → 계좌 1 순서로 `UPDATE`

A가 1을 잡고, B가 2를 잡은 상태에서 서로 상대 것을 기다리면 끝.

## 해결: 잠그는 순서를 통일

처리할 행을 **항상 같은 순서로** 먼저 잠그게 바꿨다.

```sql
SELECT id
FROM account
WHERE id IN (:ids)
ORDER BY id
FOR UPDATE;
```

JPA에서는 이렇게 썼다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id in :ids order by a.id")
List<Account> findAllForUpdate(@Param("ids") Collection<Long> ids);
```

id 오름차순으로 잠그니까 두 작업이 같은 행을 노려도 한쪽이 그냥 기다리기만 하고 원형 대기는 생기지 않는다.

## 같이 손본 설정

- `log_lock_waits = on`: `deadlock_timeout`(기본 1초)보다 오래 잠금을 기다리면 로그를 남긴다. 데드락까지 안 가도 **잠금 대기 자체**를 볼 수 있다.
- 배치 쪽에는 데드락 에러 시 **짧게 한 번 재시도**를 넣었다. 순서를 맞췄어도 다른 경로에서 생길 수 있으니 안전장치로.
- 트랜잭션 범위를 줄였다. 잠금을 오래 쥘수록 부딪칠 확률이 커진다.

> 데드락은 DB가 이상한 게 아니라, 내 코드가 **서로 다른 순서로** 문을 잠근 결과였다.
';
update post set content = '그동안 리포지토리 테스트는 H2 인메모리 DB로 돌렸다. 빠르고 편했는데, 운영은 PostgreSQL이라 **H2에서는 통과하는데 운영에서 터지는** 일이 몇 번 있었다. `jsonb` 컬럼, `ON CONFLICT`, 윈도 함수 같은 것들.

![노트북과 커피](https://picsum.photos/id/6/1200/800)

그래서 이번 분기에 통합 테스트를 Testcontainers로 옮겼다.

## 설정

의존성은 `spring-boot-testcontainers`, `testcontainers`의 `junit-jupiter`, `postgresql` 모듈.

```java
@SpringBootTest
@Testcontainers
class PostRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PostRepository postRepository;

    @Test
    void 블로그별_최신글_20개를_가져온다() {
        // given: 테스트 데이터 저장
        // when
        List<Post> posts = postRepository.findTop20ByBlogIdOrderByCreatedAtDesc(1L);
        // then
        assertThat(posts).hasSizeLessThanOrEqualTo(20);
    }
}
```

예전엔 `@DynamicPropertySource`로 URL, 계정, 비밀번호를 하나하나 넣어 줬는데, Spring Boot 3.1부터는 `@ServiceConnection`만 붙이면 접속 정보를 알아서 연결해 준다. 코드가 확 줄었다.

## 느려진 것 대응

컨테이너를 띄우니까 당연히 느려졌다. 처음엔 테스트 클래스마다 컨테이너를 새로 띄워서 전체 테스트가 4분에서 11분이 됐다.

### 컨테이너 공유
`static` 필드 컨테이너를 **추상 베이스 클래스**에 두고 테스트 클래스들이 상속하게 했다. 스프링 테스트 컨텍스트 캐시도 같이 재사용되도록 설정을 통일했다.

```java
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestSupport {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### 단위 테스트와 나누기
서비스 로직 대부분은 리포지토리를 목으로 바꾼 **단위 테스트**로 돌리고, 쿼리가 중요한 것만 통합 테스트로 남겼다.

결과적으로 전체 6분 정도. 예전보다 느리지만 **운영과 같은 DB**에서 검증된다는 안정감이 훨씬 크다.

## 겪은 문제

- CI 러너에서 Docker를 못 찾아서 실패 → Docker가 되는 러너로 바꿈
- 테스트 간 데이터가 섞임 → 각 테스트 끝에 정리하거나 `@Transactional`로 롤백
- Flyway 마이그레이션도 테스트에서 같이 돌아가서, **마이그레이션 오류를 테스트에서 먼저** 잡게 된 건 덤

> "테스트는 통과했는데요"가 의미 있으려면, 테스트 환경이 운영을 닮아야 한다.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기') and content = '그동안 리포지토리 테스트는 H2 인메모리 DB로 돌렸다. 빠르고 편했는데, 운영은 PostgreSQL이라 **H2에서는 통과하는데 운영에서 터지는** 일이 몇 번 있었다. `jsonb` 컬럼, `ON CONFLICT`, 윈도 함수 같은 것들.

그래서 이번 분기에 통합 테스트를 Testcontainers로 옮겼다.

## 설정

의존성은 `spring-boot-testcontainers`, `testcontainers`의 `junit-jupiter`, `postgresql` 모듈.

```java
@SpringBootTest
@Testcontainers
class PostRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PostRepository postRepository;

    @Test
    void 블로그별_최신글_20개를_가져온다() {
        // given: 테스트 데이터 저장
        // when
        List<Post> posts = postRepository.findTop20ByBlogIdOrderByCreatedAtDesc(1L);
        // then
        assertThat(posts).hasSizeLessThanOrEqualTo(20);
    }
}
```

예전엔 `@DynamicPropertySource`로 URL, 계정, 비밀번호를 하나하나 넣어 줬는데, Spring Boot 3.1부터는 `@ServiceConnection`만 붙이면 접속 정보를 알아서 연결해 준다. 코드가 확 줄었다.

## 느려진 것 대응

컨테이너를 띄우니까 당연히 느려졌다. 처음엔 테스트 클래스마다 컨테이너를 새로 띄워서 전체 테스트가 4분에서 11분이 됐다.

### 컨테이너 공유
`static` 필드 컨테이너를 **추상 베이스 클래스**에 두고 테스트 클래스들이 상속하게 했다. 스프링 테스트 컨텍스트 캐시도 같이 재사용되도록 설정을 통일했다.

```java
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestSupport {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### 단위 테스트와 나누기
서비스 로직 대부분은 리포지토리를 목으로 바꾼 **단위 테스트**로 돌리고, 쿼리가 중요한 것만 통합 테스트로 남겼다.

결과적으로 전체 6분 정도. 예전보다 느리지만 **운영과 같은 DB**에서 검증된다는 안정감이 훨씬 크다.

## 겪은 문제

- CI 러너에서 Docker를 못 찾아서 실패 → Docker가 되는 러너로 바꿈
- 테스트 간 데이터가 섞임 → 각 테스트 끝에 정리하거나 `@Transactional`로 롤백
- Flyway 마이그레이션도 테스트에서 같이 돌아가서, **마이그레이션 오류를 테스트에서 먼저** 잡게 된 건 덤

> "테스트는 통과했는데요"가 의미 있으려면, 테스트 환경이 운영을 닮아야 한다.
';
update post set content = '올해 2월에 입사했으니 거의 1년이다. 연말이라 그냥 넘기기 아쉬워서 적어 둔다. 기술 정리라기보다 **일기**에 가깝다.

![키보드 위의 손](https://picsum.photos/id/8/1200/800)

## 한 일

- 사내 주문 관리 서비스 유지보수 (Spring Boot, JPA, PostgreSQL)
- 정산 배치 하나를 처음부터 맡아서 만들었다
- 장애 대응 두 번 참여 (한 번은 내가 원인 제공...)

## 잘한 것

### 모르는 걸 바로 물어봤다
처음 석 달은 질문하는 게 너무 눈치 보였다. 그런데 사수가 "30분 혼자 봐도 모르겠으면 물어봐"라는 기준을 정해 줘서 그 뒤로는 편해졌다. **30분 규칙**은 내년에도 지킨다.

### 블로그를 시작했다
삽질한 걸 적다 보니, 같은 실수를 두 번 하는 일이 줄었다. `@Transactional` 글은 사내 위키에도 옮겨 놨다.

## 아쉬운 것

### 테스트를 미뤘다
"기능 먼저 하고 테스트는 나중에"를 반복했다. 결국 정산 배치에서 경계값 버그가 났는데, 테스트 하나만 있었어도 잡혔을 문제였다.

### 이유를 설명하지 못했다
코드 리뷰에서 "왜 이렇게 했어요?"라는 질문에 "이렇게 하니까 돌아가서요"라고 답한 적이 있다. 그날 꽤 부끄러웠다. 돌아가는 것과 **설명할 수 있는 것**은 다르다.

## 내년 목표

1. 새로 만드는 기능은 테스트부터 (적어도 서비스 계층은)
2. DB 공부: 실행 계획 읽는 법, 인덱스 제대로 이해하기
3. Docker로 로컬 환경 통일해 보기
4. 한 달에 글 하나는 쓰기

> 1년 동안 제일 많이 늘어난 건 코드 실력보다 **모른다고 말하는 속도**인 것 같다.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로') and content = '올해 2월에 입사했으니 거의 1년이다. 연말이라 그냥 넘기기 아쉬워서 적어 둔다. 기술 정리라기보다 **일기**에 가깝다.

## 한 일

- 사내 주문 관리 서비스 유지보수 (Spring Boot, JPA, PostgreSQL)
- 정산 배치 하나를 처음부터 맡아서 만들었다
- 장애 대응 두 번 참여 (한 번은 내가 원인 제공...)

## 잘한 것

### 모르는 걸 바로 물어봤다
처음 석 달은 질문하는 게 너무 눈치 보였다. 그런데 사수가 "30분 혼자 봐도 모르겠으면 물어봐"라는 기준을 정해 줘서 그 뒤로는 편해졌다. **30분 규칙**은 내년에도 지킨다.

### 블로그를 시작했다
삽질한 걸 적다 보니, 같은 실수를 두 번 하는 일이 줄었다. `@Transactional` 글은 사내 위키에도 옮겨 놨다.

## 아쉬운 것

### 테스트를 미뤘다
"기능 먼저 하고 테스트는 나중에"를 반복했다. 결국 정산 배치에서 경계값 버그가 났는데, 테스트 하나만 있었어도 잡혔을 문제였다.

### 이유를 설명하지 못했다
코드 리뷰에서 "왜 이렇게 했어요?"라는 질문에 "이렇게 하니까 돌아가서요"라고 답한 적이 있다. 그날 꽤 부끄러웠다. 돌아가는 것과 **설명할 수 있는 것**은 다르다.

## 내년 목표

1. 새로 만드는 기능은 테스트부터 (적어도 서비스 계층은)
2. DB 공부: 실행 계획 읽는 법, 인덱스 제대로 이해하기
3. Docker로 로컬 환경 통일해 보기
4. 한 달에 글 하나는 쓰기

> 1년 동안 제일 많이 늘어난 건 코드 실력보다 **모른다고 말하는 속도**인 것 같다.
';
update post set content = '사이드 프로젝트 빵집 지도에 "빵집 제보하기" 폼을 붙이면서 React 19의 폼 액션을 처음 제대로 써 봤어요. 예전처럼 `onSubmit`, `preventDefault`, 로딩 상태, 에러 상태를 하나하나 만들던 것보다 확실히 코드가 줄었어요.

![노트북으로 작업하는 손](https://picsum.photos/id/1/1200/800)

## 예전 방식

```tsx
const [pending, setPending] = useState(false);
const [message, setMessage] = useState('''');

async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault();
  setPending(true);
  const formData = new FormData(event.currentTarget);
  const result = await submitReport(formData);
  setMessage(result.message);
  setPending(false);
}
```

## useActionState로 바꾸기

```tsx
import { useActionState } from ''react'';

type ReportState = { ok: boolean; message: string };

async function reportAction(
  _prevState: ReportState,
  formData: FormData,
): Promise<ReportState> {
  const name = String(formData.get(''name'') ?? '''').trim();
  if (!name) {
    return { ok: false, message: ''빵집 이름을 적어 주세요.'' };
  }
  await submitReport({ name, memo: String(formData.get(''memo'') ?? '''') });
  return { ok: true, message: ''제보 고마워요! 확인하고 지도에 올릴게요.'' };
}

export function ReportForm() {
  const [state, formAction, isPending] = useActionState(reportAction, {
    ok: false,
    message: '''',
  });

  return (
    <form action={formAction}>
      <label htmlFor="name">빵집 이름</label>
      <input id="name" name="name" />

      <label htmlFor="memo">한 줄 메모</label>
      <textarea id="memo" name="memo" />

      <button type="submit" disabled={isPending}>
        {isPending ? ''보내는 중...'' : ''제보하기''}
      </button>
      <p role="status">{state.message}</p>
    </form>
  );
}
```

- `useActionState(액션, 초기 상태)`는 `[상태, 폼에 넘길 액션, 진행 중 여부]`를 돌려줘요.
- 액션 함수는 **이전 상태**와 `FormData`를 받고, 다음 상태를 돌려줘요.
- `<form action={formAction}>`으로 넘기면 `preventDefault`도 필요 없어요.

## 버튼을 따로 뺐을 때: useFormStatus

제출 버튼을 공통 컴포넌트로 빼면 `isPending`을 props로 내려야 하는데, `useFormStatus`를 쓰면 **가장 가까운 부모 form**의 상태를 읽을 수 있어요.

```tsx
import { useFormStatus } from ''react-dom'';

function SubmitButton({ children }: { children: React.ReactNode }) {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {children}
    </button>
  );
}
```

주의할 점: `useFormStatus`는 `form`을 렌더링하는 **그 컴포넌트 안에서 부르면 안 되고**, form의 자식 컴포넌트에서 불러야 해요. 처음에 같은 컴포넌트에서 불러서 계속 `pending`이 `false`였어요.

## 써 보고 느낀 점

- 제출이 끝나면 비제어 입력창이 **자동으로 초기화**돼요. 에러가 났을 때 입력한 값이 사라지는 게 싫으면, 상태에 입력값을 같이 담아서 `defaultValue`로 되돌려 줘야 했어요.
- 상태 메시지에 `role="status"`를 주면 스크린리더가 결과를 읽어 줘요.

> 폼은 결국 "보내고, 기다리고, 결과를 보여 주는 것". 그 흐름을 React가 한 덩어리로 묶어 준 느낌이에요.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리') and content = '사이드 프로젝트 빵집 지도에 "빵집 제보하기" 폼을 붙이면서 React 19의 폼 액션을 처음 제대로 써 봤어요. 예전처럼 `onSubmit`, `preventDefault`, 로딩 상태, 에러 상태를 하나하나 만들던 것보다 확실히 코드가 줄었어요.

## 예전 방식

```tsx
const [pending, setPending] = useState(false);
const [message, setMessage] = useState('''');

async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault();
  setPending(true);
  const formData = new FormData(event.currentTarget);
  const result = await submitReport(formData);
  setMessage(result.message);
  setPending(false);
}
```

## useActionState로 바꾸기

```tsx
import { useActionState } from ''react'';

type ReportState = { ok: boolean; message: string };

async function reportAction(
  _prevState: ReportState,
  formData: FormData,
): Promise<ReportState> {
  const name = String(formData.get(''name'') ?? '''').trim();
  if (!name) {
    return { ok: false, message: ''빵집 이름을 적어 주세요.'' };
  }
  await submitReport({ name, memo: String(formData.get(''memo'') ?? '''') });
  return { ok: true, message: ''제보 고마워요! 확인하고 지도에 올릴게요.'' };
}

export function ReportForm() {
  const [state, formAction, isPending] = useActionState(reportAction, {
    ok: false,
    message: '''',
  });

  return (
    <form action={formAction}>
      <label htmlFor="name">빵집 이름</label>
      <input id="name" name="name" />

      <label htmlFor="memo">한 줄 메모</label>
      <textarea id="memo" name="memo" />

      <button type="submit" disabled={isPending}>
        {isPending ? ''보내는 중...'' : ''제보하기''}
      </button>
      <p role="status">{state.message}</p>
    </form>
  );
}
```

- `useActionState(액션, 초기 상태)`는 `[상태, 폼에 넘길 액션, 진행 중 여부]`를 돌려줘요.
- 액션 함수는 **이전 상태**와 `FormData`를 받고, 다음 상태를 돌려줘요.
- `<form action={formAction}>`으로 넘기면 `preventDefault`도 필요 없어요.

## 버튼을 따로 뺐을 때: useFormStatus

제출 버튼을 공통 컴포넌트로 빼면 `isPending`을 props로 내려야 하는데, `useFormStatus`를 쓰면 **가장 가까운 부모 form**의 상태를 읽을 수 있어요.

```tsx
import { useFormStatus } from ''react-dom'';

function SubmitButton({ children }: { children: React.ReactNode }) {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {children}
    </button>
  );
}
```

주의할 점: `useFormStatus`는 `form`을 렌더링하는 **그 컴포넌트 안에서 부르면 안 되고**, form의 자식 컴포넌트에서 불러야 해요. 처음에 같은 컴포넌트에서 불러서 계속 `pending`이 `false`였어요.

## 써 보고 느낀 점

- 제출이 끝나면 비제어 입력창이 **자동으로 초기화**돼요. 에러가 났을 때 입력한 값이 사라지는 게 싫으면, 상태에 입력값을 같이 담아서 `defaultValue`로 되돌려 줘야 했어요.
- 상태 메시지에 `role="status"`를 주면 스크린리더가 결과를 읽어 줘요.

> 폼은 결국 "보내고, 기다리고, 결과를 보여 주는 것". 그 흐름을 React가 한 덩어리로 묶어 준 느낌이에요.
';
update post set content = '회사 디자인 시스템의 모달을 다시 만드는 일을 맡았어요. 예전 모달은 `div`를 화면 위에 띄우는 방식이었는데, 키보드로 써 보니 **모달 뒤의 버튼으로 포커스가 넘어가는** 문제가 있었어요. 다시 만들면서 확인한 것들을 체크리스트로 정리해요.

![노트북 자판](https://picsum.photos/id/3/1200/800)

## 체크리스트

- [ ] 열리면 포커스가 **모달 안**으로 들어간다
- [ ] `Tab`으로 돌아도 **모달 밖으로 나가지 않는다**
- [ ] `Esc`로 닫힌다
- [ ] 닫히면 포커스가 **연 버튼**으로 돌아간다
- [ ] 스크린리더가 모달의 **제목**을 읽는다
- [ ] 모달 뒤의 내용은 스크린리더가 읽지 않는다

예전 모달은 이 중 하나만 됐어요(Esc).

## dialog 요소로 바꾸기

요즘 브라우저는 `<dialog>`와 `showModal()`을 지원해요. 이걸로 위 목록의 상당 부분을 브라우저가 해 줘요.

```tsx
function ConfirmDialog({ open, title, onClose, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      openerRef.current = document.activeElement as HTMLElement | null;
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="confirm-dialog-title"
      onClose={() => {
        onClose();
        openerRef.current?.focus();
      }}
    >
      <h2 id="confirm-dialog-title">{title}</h2>
      {children}
    </dialog>
  );
}
```

`showModal()`로 열면:

- 모달 바깥이 **inert**가 돼서 클릭도, 포커스도, 스크린리더 탐색도 안 돼요.
- `Esc`를 누르면 `cancel` 이벤트가 나고 닫혀요. 닫힐 때 `close` 이벤트가 오니까 React 상태는 `onClose`에서 맞춰요.
- 화면 맨 위 레이어에 그려져서 `z-index` 싸움이 없어져요. 배경은 `::backdrop`으로 꾸며요.

## 직접 챙긴 것

- **포커스 복귀**: 브라우저가 해 주는 경우도 있지만, 확실하게 하려고 연 버튼을 기억해 뒀다가 직접 돌려줬어요.
- **처음 포커스 위치**: 삭제 확인 모달은 `취소` 버튼에 `autoFocus`를 줘요. 실수로 `Enter` 눌러서 지우는 걸 막으려고요.
- **제목 연결**: `aria-labelledby`로 제목을 이어 줘야 "대화 상자, 글 삭제" 처럼 읽혀요.
- **스크롤 잠금**: 모달 뒤 페이지가 스크롤되는 건 따로 막아야 했어요.

```css
dialog::backdrop {
  background: rgb(0 0 0 / 0.4);
}

body:has(dialog[open]) {
  overflow: hidden;
}
```

> 모달은 "띄우는 것"보다 "**닫히고 난 뒤 포커스가 어디 있는지**"가 더 중요했어요.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소') and content = '회사 디자인 시스템의 모달을 다시 만드는 일을 맡았어요. 예전 모달은 `div`를 화면 위에 띄우는 방식이었는데, 키보드로 써 보니 **모달 뒤의 버튼으로 포커스가 넘어가는** 문제가 있었어요. 다시 만들면서 확인한 것들을 체크리스트로 정리해요.

## 체크리스트

- [ ] 열리면 포커스가 **모달 안**으로 들어간다
- [ ] `Tab`으로 돌아도 **모달 밖으로 나가지 않는다**
- [ ] `Esc`로 닫힌다
- [ ] 닫히면 포커스가 **연 버튼**으로 돌아간다
- [ ] 스크린리더가 모달의 **제목**을 읽는다
- [ ] 모달 뒤의 내용은 스크린리더가 읽지 않는다

예전 모달은 이 중 하나만 됐어요(Esc).

## dialog 요소로 바꾸기

요즘 브라우저는 `<dialog>`와 `showModal()`을 지원해요. 이걸로 위 목록의 상당 부분을 브라우저가 해 줘요.

```tsx
function ConfirmDialog({ open, title, onClose, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      openerRef.current = document.activeElement as HTMLElement | null;
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="confirm-dialog-title"
      onClose={() => {
        onClose();
        openerRef.current?.focus();
      }}
    >
      <h2 id="confirm-dialog-title">{title}</h2>
      {children}
    </dialog>
  );
}
```

`showModal()`로 열면:

- 모달 바깥이 **inert**가 돼서 클릭도, 포커스도, 스크린리더 탐색도 안 돼요.
- `Esc`를 누르면 `cancel` 이벤트가 나고 닫혀요. 닫힐 때 `close` 이벤트가 오니까 React 상태는 `onClose`에서 맞춰요.
- 화면 맨 위 레이어에 그려져서 `z-index` 싸움이 없어져요. 배경은 `::backdrop`으로 꾸며요.

## 직접 챙긴 것

- **포커스 복귀**: 브라우저가 해 주는 경우도 있지만, 확실하게 하려고 연 버튼을 기억해 뒀다가 직접 돌려줬어요.
- **처음 포커스 위치**: 삭제 확인 모달은 `취소` 버튼에 `autoFocus`를 줘요. 실수로 `Enter` 눌러서 지우는 걸 막으려고요.
- **제목 연결**: `aria-labelledby`로 제목을 이어 줘야 "대화 상자, 글 삭제" 처럼 읽혀요.
- **스크롤 잠금**: 모달 뒤 페이지가 스크롤되는 건 따로 막아야 했어요.

```css
dialog::backdrop {
  background: rgb(0 0 0 / 0.4);
}

body:has(dialog[open]) {
  overflow: hidden;
}
```

> 모달은 "띄우는 것"보다 "**닫히고 난 뒤 포커스가 어디 있는지**"가 더 중요했어요.
';
update post set content = '사이드 프로젝트 빵집 지도를 밤에 보다가 눈이 너무 부셔서 다크 모드를 넣었어요. 처음엔 컴포넌트마다 `.dark .card { ... }` 를 붙이다가 금방 감당이 안 돼서, **CSS 변수 한곳**으로 정리했어요.

![어두운 책상 위 노트북](https://picsum.photos/id/5/1200/800)

## 1. 색을 역할 이름으로 정의

색 이름(`--gray-100`)이 아니라 **역할 이름**으로 정했어요.

```css
:root {
  --color-bg: #ffffff;
  --color-surface: #f5f5f4;
  --color-text: #1c1917;
  --color-text-muted: #57534e;
  --color-border: #e7e5e4;
  color-scheme: light;
}
```

컴포넌트는 이 변수만 써요.

```css
.card {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
```

## 2. 시스템 설정 따라가기

```css
@media (prefers-color-scheme: dark) {
  :root:not([data-theme=''light'']) {
    --color-bg: #1c1917;
    --color-surface: #292524;
    --color-text: #f5f5f4;
    --color-text-muted: #a8a29e;
    --color-border: #44403c;
    color-scheme: dark;
  }
}
```

`color-scheme: dark`를 꼭 같이 줘요. 이걸 빼면 **스크롤바, 폼 입력창, 체크박스**가 여전히 밝은 색으로 나와서 어색해요.

## 3. 사용자가 직접 고르기

시스템은 라이트인데 이 사이트만 어둡게 보고 싶은 사람도 있어서 토글을 만들었어요.

```css
:root[data-theme=''dark''] {
  --color-bg: #1c1917;
  --color-surface: #292524;
  --color-text: #f5f5f4;
  --color-text-muted: #a8a29e;
  --color-border: #44403c;
  color-scheme: dark;
}
```

```ts
function setTheme(theme: ''light'' | ''dark'' | ''system'') {
  const root = document.documentElement;
  if (theme === ''system'') {
    root.removeAttribute(''data-theme'');
  } else {
    root.setAttribute(''data-theme'', theme);
  }
}
```

다크 값을 두 번 적는 게 좀 아쉽지만, 구조가 단순해서 그냥 두었어요.

## 해 보니 챙길 것

- **완전한 검정(#000)** 은 오히려 눈이 피곤했어요. 살짝 따뜻한 짙은 회색이 편해요.
- 다크에서는 글자와 배경의 **대비**를 다시 확인해야 해요. 라이트에서 괜찮던 보조 글자색이 다크에서 안 읽혔어요.
- 지도 타일은 CSS 변수로 안 바뀌어서, 다크 모드일 때만 타일에 약간 어둡게 필터를 줬어요.

> 컴포넌트가 색을 직접 알지 않게 하는 것. 그게 다크 모드의 전부였어요.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법') and content = '사이드 프로젝트 빵집 지도를 밤에 보다가 눈이 너무 부셔서 다크 모드를 넣었어요. 처음엔 컴포넌트마다 `.dark .card { ... }` 를 붙이다가 금방 감당이 안 돼서, **CSS 변수 한곳**으로 정리했어요.

## 1. 색을 역할 이름으로 정의

색 이름(`--gray-100`)이 아니라 **역할 이름**으로 정했어요.

```css
:root {
  --color-bg: #ffffff;
  --color-surface: #f5f5f4;
  --color-text: #1c1917;
  --color-text-muted: #57534e;
  --color-border: #e7e5e4;
  color-scheme: light;
}
```

컴포넌트는 이 변수만 써요.

```css
.card {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
```

## 2. 시스템 설정 따라가기

```css
@media (prefers-color-scheme: dark) {
  :root:not([data-theme=''light'']) {
    --color-bg: #1c1917;
    --color-surface: #292524;
    --color-text: #f5f5f4;
    --color-text-muted: #a8a29e;
    --color-border: #44403c;
    color-scheme: dark;
  }
}
```

`color-scheme: dark`를 꼭 같이 줘요. 이걸 빼면 **스크롤바, 폼 입력창, 체크박스**가 여전히 밝은 색으로 나와서 어색해요.

## 3. 사용자가 직접 고르기

시스템은 라이트인데 이 사이트만 어둡게 보고 싶은 사람도 있어서 토글을 만들었어요.

```css
:root[data-theme=''dark''] {
  --color-bg: #1c1917;
  --color-surface: #292524;
  --color-text: #f5f5f4;
  --color-text-muted: #a8a29e;
  --color-border: #44403c;
  color-scheme: dark;
}
```

```ts
function setTheme(theme: ''light'' | ''dark'' | ''system'') {
  const root = document.documentElement;
  if (theme === ''system'') {
    root.removeAttribute(''data-theme'');
  } else {
    root.setAttribute(''data-theme'', theme);
  }
}
```

다크 값을 두 번 적는 게 좀 아쉽지만, 구조가 단순해서 그냥 두었어요.

## 해 보니 챙길 것

- **완전한 검정(#000)** 은 오히려 눈이 피곤했어요. 살짝 따뜻한 짙은 회색이 편해요.
- 다크에서는 글자와 배경의 **대비**를 다시 확인해야 해요. 라이트에서 괜찮던 보조 글자색이 다크에서 안 읽혔어요.
- 지도 타일은 CSS 변수로 안 바뀌어서, 다크 모드일 때만 타일에 약간 어둡게 필터를 줬어요.

> 컴포넌트가 색을 직접 알지 않게 하는 것. 그게 다크 모드의 전부였어요.
';
update post set content = '카드 목록 반응형을 만들 때마다 미디어 쿼리를 3~4개씩 쓰고 있었는데, Grid 한 줄로 거의 끝난다는 걸 알고 나서 갈아탔어요. 그런데 `auto-fill`이랑 `auto-fit`이 매번 헷갈려서 정리해 둡니다.

![작업 책상](https://picsum.photos/id/7/1200/800)

## 기본 모양

```css
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
```

- `minmax(240px, 1fr)`: 카드는 **최소 240px**, 남는 공간이 있으면 균등하게 늘어나요.
- `repeat(auto-fill, ...)`: 컨테이너 너비에 **들어갈 수 있는 만큼** 열을 만들어요.

화면이 넓어지면 열이 늘고, 좁아지면 줄어요. 미디어 쿼리 없이요.

## auto-fill vs auto-fit

차이는 **아이템이 열 수보다 적을 때**만 보여요.

예를 들어 컨테이너에 열이 5개 들어갈 수 있는데 카드가 2장뿐이라면:

- `auto-fill`: 빈 열 3개를 **그대로 남겨 둬요**. 카드 2장은 240px 언저리 크기로 왼쪽에 붙어 있어요.
- `auto-fit`: 빈 열을 **0으로 접어요**. 남은 공간을 카드 2장이 나눠 가져서 엄청 넓어져요.

| 상황 | 추천 |
|---|---|
| 검색 결과처럼 개수가 들쭉날쭉한 목록 | `auto-fill` (카드 크기가 일정해서 덜 어색해요) |
| 항목 수가 거의 고정된 소개 섹션 | `auto-fit` (꽉 차게 늘어나요) |

## 좁은 화면에서 넘칠 때

컨테이너가 240px보다 좁아지면 카드가 삐져나가서 가로 스크롤이 생겨요. 이렇게 막았어요.

```css
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(240px, 100%), 1fr));
  gap: 16px;
}
```

`min(240px, 100%)`라서 컨테이너가 좁으면 최소 너비가 컨테이너 너비로 줄어들어요.

## 카드 높이 맞추기

Grid는 같은 행의 아이템 높이를 기본으로 맞춰 줘요(`align-items: stretch`). 카드 안에서 버튼을 바닥에 붙이고 싶으면 카드에 `display: flex; flex-direction: column;`을 주고 버튼에 `margin-top: auto`를 줘요.

> 미디어 쿼리 4개가 한 줄이 됐을 때의 쾌감이란...
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기') and content = '카드 목록 반응형을 만들 때마다 미디어 쿼리를 3~4개씩 쓰고 있었는데, Grid 한 줄로 거의 끝난다는 걸 알고 나서 갈아탔어요. 그런데 `auto-fill`이랑 `auto-fit`이 매번 헷갈려서 정리해 둡니다.

## 기본 모양

```css
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
```

- `minmax(240px, 1fr)`: 카드는 **최소 240px**, 남는 공간이 있으면 균등하게 늘어나요.
- `repeat(auto-fill, ...)`: 컨테이너 너비에 **들어갈 수 있는 만큼** 열을 만들어요.

화면이 넓어지면 열이 늘고, 좁아지면 줄어요. 미디어 쿼리 없이요.

## auto-fill vs auto-fit

차이는 **아이템이 열 수보다 적을 때**만 보여요.

예를 들어 컨테이너에 열이 5개 들어갈 수 있는데 카드가 2장뿐이라면:

- `auto-fill`: 빈 열 3개를 **그대로 남겨 둬요**. 카드 2장은 240px 언저리 크기로 왼쪽에 붙어 있어요.
- `auto-fit`: 빈 열을 **0으로 접어요**. 남은 공간을 카드 2장이 나눠 가져서 엄청 넓어져요.

| 상황 | 추천 |
|---|---|
| 검색 결과처럼 개수가 들쭉날쭉한 목록 | `auto-fill` (카드 크기가 일정해서 덜 어색해요) |
| 항목 수가 거의 고정된 소개 섹션 | `auto-fit` (꽉 차게 늘어나요) |

## 좁은 화면에서 넘칠 때

컨테이너가 240px보다 좁아지면 카드가 삐져나가서 가로 스크롤이 생겨요. 이렇게 막았어요.

```css
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(240px, 100%), 1fr));
  gap: 16px;
}
```

`min(240px, 100%)`라서 컨테이너가 좁으면 최소 너비가 컨테이너 너비로 줄어들어요.

## 카드 높이 맞추기

Grid는 같은 행의 아이템 높이를 기본으로 맞춰 줘요(`align-items: stretch`). 카드 안에서 버튼을 바닥에 붙이고 싶으면 카드에 `display: flex; flex-direction: column;`을 주고 버튼에 `margin-top: auto`를 줘요.

> 미디어 쿼리 4개가 한 줄이 됐을 때의 쾌감이란...
';
update post set content = '주말마다 동네 빵집을 돌아다니는 게 취미인데, 메모 앱에 적어 둔 빵집 목록이 50곳이 넘어가면서 찾기가 힘들어졌어요. 그래서 **내가 쓰려고** 지도 페이지를 만들기 시작했어요.

![노트북과 메모](https://picsum.photos/id/9/1200/800)

## 정한 범위

욕심내면 끝을 못 낼 것 같아서 1차 범위를 작게 잡았어요.

- 지도에 빵집 핀 찍기
- 핀 누르면 이름, 대표 메뉴, 쉬는 날, 한 줄 메모
- "오늘 문 연 곳만" 필터
- 로그인, 댓글, 리뷰 같은 건 **안 함**

데이터는 JSON 파일 하나로 시작했어요. 서버는 나중에 필요해지면요.

## 고른 도구

| 용도 | 고른 것 | 이유 |
|---|---|---|
| 빌드 | Vite + React + TypeScript | 회사에서 쓰는 거라 익숙해서 |
| 지도 | Leaflet + OpenStreetMap 타일 | 키 발급 없이 바로 시작 가능 |
| 배포 | 정적 호스팅 | 서버가 없으니까 |

## 데이터 모양

```ts
type Weekday = ''mon'' | ''tue'' | ''wed'' | ''thu'' | ''fri'' | ''sat'' | ''sun'';

interface Bakery {
  id: string;
  name: string;
  lat: number;
  lng: number;
  signature: string[];
  closedOn: Weekday[];
  memo?: string;
}
```

## "오늘 문 연 곳" 필터

```ts
const WEEKDAYS: Weekday[] = [''sun'', ''mon'', ''tue'', ''wed'', ''thu'', ''fri'', ''sat''];

function isOpenToday(bakery: Bakery, today = new Date()): boolean {
  const weekday = WEEKDAYS[today.getDay()];
  return !bakery.closedOn.includes(weekday);
}
```

`getDay()`는 일요일이 0이라서 배열을 일요일부터 시작했어요. 처음에 월요일부터 적었다가 하루씩 밀려서, 월요일 휴무인 집이 화요일에 닫혀 있다고 나왔어요.

## 해 보니 어려웠던 것

- 핀이 30개 넘으니까 겹쳐서 누르기 힘들었어요. 다음 편에서 묶어 보여 주는 방법을 찾아볼 거예요.
- 모바일에서 지도 스크롤이랑 페이지 스크롤이 싸워요. 지도 높이를 화면의 60%로 고정하고 아래에 목록을 뒀어요.
- 위도/경도는 손으로 찾아 넣는 게 제일 오래 걸렸어요...

> 남이 쓸 서비스가 아니라 **내가 매주 쓸 도구**라서 그런지, 오랜만에 사이드 프로젝트가 재밌어요.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지') and content = '주말마다 동네 빵집을 돌아다니는 게 취미인데, 메모 앱에 적어 둔 빵집 목록이 50곳이 넘어가면서 찾기가 힘들어졌어요. 그래서 **내가 쓰려고** 지도 페이지를 만들기 시작했어요.

## 정한 범위

욕심내면 끝을 못 낼 것 같아서 1차 범위를 작게 잡았어요.

- 지도에 빵집 핀 찍기
- 핀 누르면 이름, 대표 메뉴, 쉬는 날, 한 줄 메모
- "오늘 문 연 곳만" 필터
- 로그인, 댓글, 리뷰 같은 건 **안 함**

데이터는 JSON 파일 하나로 시작했어요. 서버는 나중에 필요해지면요.

## 고른 도구

| 용도 | 고른 것 | 이유 |
|---|---|---|
| 빌드 | Vite + React + TypeScript | 회사에서 쓰는 거라 익숙해서 |
| 지도 | Leaflet + OpenStreetMap 타일 | 키 발급 없이 바로 시작 가능 |
| 배포 | 정적 호스팅 | 서버가 없으니까 |

## 데이터 모양

```ts
type Weekday = ''mon'' | ''tue'' | ''wed'' | ''thu'' | ''fri'' | ''sat'' | ''sun'';

interface Bakery {
  id: string;
  name: string;
  lat: number;
  lng: number;
  signature: string[];
  closedOn: Weekday[];
  memo?: string;
}
```

## "오늘 문 연 곳" 필터

```ts
const WEEKDAYS: Weekday[] = [''sun'', ''mon'', ''tue'', ''wed'', ''thu'', ''fri'', ''sat''];

function isOpenToday(bakery: Bakery, today = new Date()): boolean {
  const weekday = WEEKDAYS[today.getDay()];
  return !bakery.closedOn.includes(weekday);
}
```

`getDay()`는 일요일이 0이라서 배열을 일요일부터 시작했어요. 처음에 월요일부터 적었다가 하루씩 밀려서, 월요일 휴무인 집이 화요일에 닫혀 있다고 나왔어요.

## 해 보니 어려웠던 것

- 핀이 30개 넘으니까 겹쳐서 누르기 힘들었어요. 다음 편에서 묶어 보여 주는 방법을 찾아볼 거예요.
- 모바일에서 지도 스크롤이랑 페이지 스크롤이 싸워요. 지도 높이를 화면의 60%로 고정하고 아래에 목록을 뒀어요.
- 위도/경도는 손으로 찾아 넣는 게 제일 오래 걸렸어요...

> 남이 쓸 서비스가 아니라 **내가 매주 쓸 도구**라서 그런지, 오랜만에 사이드 프로젝트가 재밌어요.
';
update post set content = '회사에 원격 근무 제도가 생겨서 처음으로 **일주일 워케이션**을 다녀왔어요. 제주 동쪽 바닷가 마을에 숙소를 잡았어요. 개발 글은 아니지만 기록해 두고 싶어서 일상 카테고리에 남겨요.

![바다 너머 산](https://picsum.photos/id/16/1200/800)

## 준비한 것

- 노트북, 충전기, **휴대용 모니터** (이거 가져간 게 신의 한 수)
- 숙소 고를 때 와이파이 속도 미리 물어보기. 화상 회의가 있어서요.
- 핫스팟 데이터 여유분. 실제로 하루 와이파이가 끊겨서 썼어요.

## 하루 루틴

| 시간 | 한 것 |
|---|---|
| 7:30 | 해안 산책 40분 |
| 9:00 ~ 12:00 | 집중 업무 (회의 없는 시간) |
| 12:00 ~ 13:30 | 동네 식당 점심 |
| 13:30 ~ 18:00 | 회의, 코드 리뷰 |
| 18:00 이후 | 노을 보러 나가기, 저녁 |

평소보다 출퇴근 시간이 없으니까 아침 산책이 가능했어요. 오전 집중 시간에 작업이 진짜 잘 됐어요. 창밖에 바다가 보이는 게 생각보다 효과가 커요.

## 좋았던 것

- 점심 먹고 10분 걸으면 바다가 나와요. 오후 회의 전에 머리 식히기 딱 좋았어요.
- 동네 하나로 반경을 좁혔더니 **관광하느라 지치는 일**이 없었어요.
- 오일장 구경. 귤 한 봉지 사서 일주일 내내 책상 옆에 두고 먹었어요.

## 아쉬웠던 것

- 의자. 숙소 식탁 의자에서 하루 8시간은 허리가 버티질 못해요. 결국 쿠션을 하나 샀어요.
- 일과 휴가의 경계가 흐려져서, 수요일엔 밤 10시까지 일하고 있었어요.
- 날씨. 바람 부는 날엔 산책이 거의 극기 훈련이었어요.

## 다음에 간다면

- 일주일보다는 **열흘**. 첫날 적응, 마지막 날 짐 싸기를 빼면 실제로는 5일이었어요.
- 금요일 오후는 연차 반차 내고 제대로 놀기.

> 장소가 바뀐다고 일이 줄진 않지만, 일 사이사이의 **10분**이 완전히 달라졌어요.
' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록') and content = '회사에 원격 근무 제도가 생겨서 처음으로 **일주일 워케이션**을 다녀왔어요. 제주 동쪽 바닷가 마을에 숙소를 잡았어요. 개발 글은 아니지만 기록해 두고 싶어서 일상 카테고리에 남겨요.

## 준비한 것

- 노트북, 충전기, **휴대용 모니터** (이거 가져간 게 신의 한 수)
- 숙소 고를 때 와이파이 속도 미리 물어보기. 화상 회의가 있어서요.
- 핫스팟 데이터 여유분. 실제로 하루 와이파이가 끊겨서 썼어요.

## 하루 루틴

| 시간 | 한 것 |
|---|---|
| 7:30 | 해안 산책 40분 |
| 9:00 ~ 12:00 | 집중 업무 (회의 없는 시간) |
| 12:00 ~ 13:30 | 동네 식당 점심 |
| 13:30 ~ 18:00 | 회의, 코드 리뷰 |
| 18:00 이후 | 노을 보러 나가기, 저녁 |

평소보다 출퇴근 시간이 없으니까 아침 산책이 가능했어요. 오전 집중 시간에 작업이 진짜 잘 됐어요. 창밖에 바다가 보이는 게 생각보다 효과가 커요.

## 좋았던 것

- 점심 먹고 10분 걸으면 바다가 나와요. 오후 회의 전에 머리 식히기 딱 좋았어요.
- 동네 하나로 반경을 좁혔더니 **관광하느라 지치는 일**이 없었어요.
- 오일장 구경. 귤 한 봉지 사서 일주일 내내 책상 옆에 두고 먹었어요.

## 아쉬웠던 것

- 의자. 숙소 식탁 의자에서 하루 8시간은 허리가 버티질 못해요. 결국 쿠션을 하나 샀어요.
- 일과 휴가의 경계가 흐려져서, 수요일엔 밤 10시까지 일하고 있었어요.
- 날씨. 바람 부는 날엔 산책이 거의 극기 훈련이었어요.

## 다음에 간다면

- 일주일보다는 **열흘**. 첫날 적응, 마지막 날 짐 싸기를 빼면 실제로는 5일이었어요.
- 금요일 오후는 연차 반차 내고 제대로 놀기.

> 장소가 바뀐다고 일이 줄진 않지만, 일 사이사이의 **10분**이 완전히 달라졌어요.
';
update post set content = '개천절 다음 날, 날씨가 너무 좋아서 아침 9시에 일어나자마자 용산역으로 갔다. ITX-청춘 타면 가평역까지 1시간 조금 넘게 걸린다.

![산과 호수](https://picsum.photos/id/11/1200/800)

## 가는 법

- 용산역 or 청량리역 → ITX-청춘 → **가평역**
- 가평역에서 자라섬까지 걸어서 20분 (평지라 걷기 좋다)
- 경춘선 전철로 가도 되는데 시간이 좀 더 걸린다

## 자라섬

북한강 안에 있는 섬인데 다리로 연결돼 있어서 그냥 걸어 들어간다. 가을이라 **남도 꽃 정원**에 코스모스랑 구절초가 한창이었다.

- 중도 → 남도 순서로 한 바퀴 약 1시간 반
- 남도 끝 쪽 강변 벤치가 명당. 강 건너 산이 슬슬 물들기 시작했다
- 캠핑장은 예약한 사람만 들어갈 수 있다

바람이 불 때마다 코스모스가 한꺼번에 고개를 숙였다 드는 게 꼭 누가 인사하는 것 같았다. 사진은 몇 장 찍고 대부분 그냥 봤다.

## 점심

가평역 근처 작은 막국숫집에서 들기름 막국수. 9천 원. 들기름 향이 진하고 김가루, 깨가 듬뿍. 반 정도 먹고 육수를 부어 먹으라고 사장님이 알려 주셨는데, 그렇게 먹으니까 전혀 다른 음식이 됐다.

## 오늘의 반나절 기록

- 출발 10:10 → 귀가 16:30
- 걸음 수 약 13,000보
- 쓴 돈 약 2만 8천 원 (ITX 왕복 포함)

> 가을은 짧다. 그래서 반나절이라도 꼭 나가야 한다.

다음 주엔 단풍 들면 남이섬 쪽도 가 볼까 생각 중.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기') and content = '개천절 다음 날, 날씨가 너무 좋아서 아침 9시에 일어나자마자 용산역으로 갔다. ITX-청춘 타면 가평역까지 1시간 조금 넘게 걸린다.

## 가는 법

- 용산역 or 청량리역 → ITX-청춘 → **가평역**
- 가평역에서 자라섬까지 걸어서 20분 (평지라 걷기 좋다)
- 경춘선 전철로 가도 되는데 시간이 좀 더 걸린다

## 자라섬

북한강 안에 있는 섬인데 다리로 연결돼 있어서 그냥 걸어 들어간다. 가을이라 **남도 꽃 정원**에 코스모스랑 구절초가 한창이었다.

- 중도 → 남도 순서로 한 바퀴 약 1시간 반
- 남도 끝 쪽 강변 벤치가 명당. 강 건너 산이 슬슬 물들기 시작했다
- 캠핑장은 예약한 사람만 들어갈 수 있다

바람이 불 때마다 코스모스가 한꺼번에 고개를 숙였다 드는 게 꼭 누가 인사하는 것 같았다. 사진은 몇 장 찍고 대부분 그냥 봤다.

## 점심

가평역 근처 작은 막국숫집에서 들기름 막국수. 9천 원. 들기름 향이 진하고 김가루, 깨가 듬뿍. 반 정도 먹고 육수를 부어 먹으라고 사장님이 알려 주셨는데, 그렇게 먹으니까 전혀 다른 음식이 됐다.

## 오늘의 반나절 기록

- 출발 10:10 → 귀가 16:30
- 걸음 수 약 13,000보
- 쓴 돈 약 2만 8천 원 (ITX 왕복 포함)

> 가을은 짧다. 그래서 반나절이라도 꼭 나가야 한다.

다음 주엔 단풍 들면 남이섬 쪽도 가 볼까 생각 중.';
update post set content = '여행 블로그지만 오늘은 운동 쪽 기록. 요즘 주말마다 근교 걷기를 하는데, 그중에 제일 좋았던 우이령길.

![숲속 나무들](https://picsum.photos/id/10/1200/800)

## 우이령길이란

북한산과 도봉산 사이 고개를 넘는 길인데, 오랫동안 출입이 막혀 있다가 열린 길이라 숲이 정말 잘 보존돼 있다. **하루 입장 인원이 정해져 있어서 사전 예약 필수.**

- 예약: 국립공원공단 예약 시스템
- 출발: 우이동(서울 쪽) 또는 교현리(양주 쪽)
- 거리: 약 6.8km 편도
- 소요: 천천히 걸어서 2시간 10분

## 걸어 보니

경사가 거의 없다. 등산이라기보다 **숲속 산책**에 가깝다. 바닥이 흙길이라 무릎에도 편했다.

중간쯤 고갯마루에서 보이는 **오봉** 바위 다섯 개가 이 길의 하이라이트. 바위가 나란히 서 있는 모습이 꼭 누가 하나씩 올려놓은 것 같다.

## 기록

| 항목 | 내용 |
|---|---|
| 거리 | 6.8km |
| 시간 | 2시간 10분 |
| 걸음 수 | 약 11,000보 |
| 평균 심박 | 102 |

심박이 100 언저리로 계속 유지돼서 유산소로 딱 좋은 강도였다.

## 준비물

- 신분증 (입구에서 예약 확인)
- 물 500ml 두 병
- 가벼운 간식 — 중간에 매점 없음
- 모기 기피제 (9월에도 있다)

> 숨이 차지 않는 운동이 오히려 오래 간다. 걷는 게 즐거워야 다음 주에도 나온다.

교현리 쪽으로 내려와서 버스 타고 구파발역으로 돌아왔다.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길') and content = '여행 블로그지만 오늘은 운동 쪽 기록. 요즘 주말마다 근교 걷기를 하는데, 그중에 제일 좋았던 우이령길.

## 우이령길이란

북한산과 도봉산 사이 고개를 넘는 길인데, 오랫동안 출입이 막혀 있다가 열린 길이라 숲이 정말 잘 보존돼 있다. **하루 입장 인원이 정해져 있어서 사전 예약 필수.**

- 예약: 국립공원공단 예약 시스템
- 출발: 우이동(서울 쪽) 또는 교현리(양주 쪽)
- 거리: 약 6.8km 편도
- 소요: 천천히 걸어서 2시간 10분

## 걸어 보니

경사가 거의 없다. 등산이라기보다 **숲속 산책**에 가깝다. 바닥이 흙길이라 무릎에도 편했다.

중간쯤 고갯마루에서 보이는 **오봉** 바위 다섯 개가 이 길의 하이라이트. 바위가 나란히 서 있는 모습이 꼭 누가 하나씩 올려놓은 것 같다.

## 기록

| 항목 | 내용 |
|---|---|
| 거리 | 6.8km |
| 시간 | 2시간 10분 |
| 걸음 수 | 약 11,000보 |
| 평균 심박 | 102 |

심박이 100 언저리로 계속 유지돼서 유산소로 딱 좋은 강도였다.

## 준비물

- 신분증 (입구에서 예약 확인)
- 물 500ml 두 병
- 가벼운 간식 — 중간에 매점 없음
- 모기 기피제 (9월에도 있다)

> 숨이 차지 않는 운동이 오히려 오래 간다. 걷는 게 즐거워야 다음 주에도 나온다.

교현리 쪽으로 내려와서 버스 타고 구파발역으로 돌아왔다.';
update post set content = '토요일 아침에 눈 떴는데 날씨가 너무 좋아서 즉흥으로 나갔다. 목표는 오후 4시 전에 집에 돌아오기.

![강이 흐르는 골짜기](https://picsum.photos/id/1015/1200/800)

## 가는 법

- 경의중앙선 **양수역** 하차 (청량리에서 약 50분)
- 양수역에서 두물머리까지 걸어서 25분 정도
- 자전거 빌려도 되는데 3월이라 바람이 차서 그냥 걸었다

## 두물머리

북한강이랑 남한강이 만나는 곳이라서 두물머리. 이름 붙인 사람 센스 좋다. 큰 느티나무 한 그루가 강 쪽으로 서 있는데, 다들 그 나무 앞에서 사진 찍으려고 줄을 섰다. 나는 줄 옆에서 그냥 나무 뒤통수를 찍었다. 그게 더 마음에 든다.

- 느티나무 → 배다리 → 세미원 쪽 연결 산책로
- **세미원**은 입장료 5천 원. 연꽃 철(7~8월)이 아니라서 이번엔 패스
- 강변 데크길 따라 쭉 걷기만 해도 40분은 금방 간다

## 점심

양수역 근처 작은 국숫집에서 잔치국수 한 그릇. 7천 원. 멸치 육수가 진하고 김가루가 많이 올라가 있었다. 근교 반나절의 핵심은 결국 이런 국수 한 그릇 아닐까.

## 오늘 쓴 돈

| 항목 | 금액 |
|---|---|
| 지하철 왕복 | 약 4,000원 |
| 국수 | 7,000원 |
| 핫도그 (두물머리 명물) | 3,500원 |
| 합계 | 14,500원 |

> 반나절 여행은 ''돌아오는 시간''을 먼저 정해 두면 마음이 편하다.

3시 반에 집 도착. 저녁에 빨래도 했다. 완벽한 토요일.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴') and content = '토요일 아침에 눈 떴는데 날씨가 너무 좋아서 즉흥으로 나갔다. 목표는 오후 4시 전에 집에 돌아오기.

## 가는 법

- 경의중앙선 **양수역** 하차 (청량리에서 약 50분)
- 양수역에서 두물머리까지 걸어서 25분 정도
- 자전거 빌려도 되는데 3월이라 바람이 차서 그냥 걸었다

## 두물머리

북한강이랑 남한강이 만나는 곳이라서 두물머리. 이름 붙인 사람 센스 좋다. 큰 느티나무 한 그루가 강 쪽으로 서 있는데, 다들 그 나무 앞에서 사진 찍으려고 줄을 섰다. 나는 줄 옆에서 그냥 나무 뒤통수를 찍었다. 그게 더 마음에 든다.

- 느티나무 → 배다리 → 세미원 쪽 연결 산책로
- **세미원**은 입장료 5천 원. 연꽃 철(7~8월)이 아니라서 이번엔 패스
- 강변 데크길 따라 쭉 걷기만 해도 40분은 금방 간다

## 점심

양수역 근처 작은 국숫집에서 잔치국수 한 그릇. 7천 원. 멸치 육수가 진하고 김가루가 많이 올라가 있었다. 근교 반나절의 핵심은 결국 이런 국수 한 그릇 아닐까.

## 오늘 쓴 돈

| 항목 | 금액 |
|---|---|
| 지하철 왕복 | 약 4,000원 |
| 국수 | 7,000원 |
| 핫도그 (두물머리 명물) | 3,500원 |
| 합계 | 14,500원 |

> 반나절 여행은 ''돌아오는 시간''을 먼저 정해 두면 마음이 편하다.

3시 반에 집 도착. 저녁에 빨래도 했다. 완벽한 토요일.';
update post set content = '첫 해외 혼자 여행. 가까운 데부터 가 보자 해서 후쿠오카로 정했다. 인천에서 1시간 20분이면 도착이라 제주 가는 거랑 별 차이 없었다.

![멀리 보이는 산줄기](https://picsum.photos/id/1018/1200/800)

## 숙소 위치

**하카타역 근처**로 잡았다. 공항에서 지하철로 두 정거장이고, 어디로 가든 하카타에서 출발하니까 편하다. 텐진 쪽도 좋다는데 처음이라 교통 편한 쪽으로.

## 1일차 (오후 도착)

1. 공항 → 하카타역 (지하철 5분)
2. 숙소에 짐 맡기고 캐널시티 구경
3. 나카스 강변 포장마차 거리 — 라멘 한 그릇이랑 꼬치 몇 개
4. 숙소 복귀, 편의점 푸딩

포장마차는 자리가 좁아서 옆 사람이랑 어깨가 닿는다. 옆자리 아저씨가 손짓으로 뭘 시키라고 추천해 줬는데 그게 명란 계란말이였다. 맛있었다.

## 2일차

1. 오전: 다자이후 (니시테츠 전철, 텐진에서 약 40분)
2. 다자이후 텐만구 참배길에서 우메가에모치 먹기
3. 오후: 텐진 돌아와서 오호리 공원 한 바퀴 (약 2km)
4. 저녁: 모츠나베

오호리 공원은 호수 둘레를 사람들이 다 같이 뛰고 걷는 분위기라 좋았다. 벤치에 앉아서 1시간 그냥 멍하니 있었다.

## 3일차

- 오전에 하카타역 지하 쇼핑, 오후 비행기로 귀국

## 쓴 돈 대충

- 항공권 왕복 약 28만 원 (특가)
- 숙소 2박 약 16만 원
- 현지 교통+식비+쇼핑 약 25만 원

> 첫 혼자 해외는 ''많이 보기''보다 ''헤매지 않기''를 목표로 하면 덜 지친다.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리') and content = '첫 해외 혼자 여행. 가까운 데부터 가 보자 해서 후쿠오카로 정했다. 인천에서 1시간 20분이면 도착이라 제주 가는 거랑 별 차이 없었다.

## 숙소 위치

**하카타역 근처**로 잡았다. 공항에서 지하철로 두 정거장이고, 어디로 가든 하카타에서 출발하니까 편하다. 텐진 쪽도 좋다는데 처음이라 교통 편한 쪽으로.

## 1일차 (오후 도착)

1. 공항 → 하카타역 (지하철 5분)
2. 숙소에 짐 맡기고 캐널시티 구경
3. 나카스 강변 포장마차 거리 — 라멘 한 그릇이랑 꼬치 몇 개
4. 숙소 복귀, 편의점 푸딩

포장마차는 자리가 좁아서 옆 사람이랑 어깨가 닿는다. 옆자리 아저씨가 손짓으로 뭘 시키라고 추천해 줬는데 그게 명란 계란말이였다. 맛있었다.

## 2일차

1. 오전: 다자이후 (니시테츠 전철, 텐진에서 약 40분)
2. 다자이후 텐만구 참배길에서 우메가에모치 먹기
3. 오후: 텐진 돌아와서 오호리 공원 한 바퀴 (약 2km)
4. 저녁: 모츠나베

오호리 공원은 호수 둘레를 사람들이 다 같이 뛰고 걷는 분위기라 좋았다. 벤치에 앉아서 1시간 그냥 멍하니 있었다.

## 3일차

- 오전에 하카타역 지하 쇼핑, 오후 비행기로 귀국

## 쓴 돈 대충

- 항공권 왕복 약 28만 원 (특가)
- 숙소 2박 약 16만 원
- 현지 교통+식비+쇼핑 약 25만 원

> 첫 혼자 해외는 ''많이 보기''보다 ''헤매지 않기''를 목표로 하면 덜 지친다.';
update post set content = '추석 연휴 마지막 날, 다들 서울로 올라올 때 반대로 강원도 고성에 갔어요. 하행선이 텅 비어 있어서 양양까지 2시간 반 만에 도착했어요.

![잔잔한 바다](https://picsum.photos/id/14/1200/800)

## 화진포 해변

고성에서도 북쪽 끝, 통일전망대 가는 길에 있어요. 9월 말이라 해수욕장은 이미 문을 닫았고, 백사장에 저 말고 두 가족 정도만 있었어요.

- 모래가 굵고 밟으면 ''뽀득'' 소리가 나요
- 해변 바로 뒤에 **화진포 호수**가 있어서, 바다와 호수를 한 번에 볼 수 있어요
- 호수 둘레길 약 16km. 저는 일부만 1시간 걸었어요

## 화진포의 성

해변 북쪽 언덕에 ''화진포의 성''이라는 오래된 건물이 있어요. 입장료 3천 원(주변 기념관 통합권). 건물 자체보다 2층 창문에서 내려다보는 바다가 좋아요. 창틀이 액자처럼 바다를 잘라 줘요.

## 거진항에서 늦은 점심

화진포에서 차로 10분 거리 거진항 앞 작은 식당 ''거진할매집''에서 도치알탕을 먹었어요. 원래 겨울 음식이라 메뉴에 없을 줄 알았는데 냉동해 둔 걸로 끓여 주셨어요. 1만 5천 원. 알이 톡톡 터지는 식감이 재밌어요.

## 이번 여행에서 느낀 것

1. 연휴 마지막 날 하행선은 **최고의 비밀 시간**이에요
2. 바다는 성수기가 지나야 비로소 바다 소리가 들려요
3. 강원도 북쪽 끝은 생각보다 가깝다

> 여름 바다가 사람들의 바다라면, 가을 바다는 바다 자신의 것 같아요.

돌아오는 길엔 역시 상행선이 막혀서 5시간 걸렸어요. 그래도 괜찮았어요.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다') and content = '추석 연휴 마지막 날, 다들 서울로 올라올 때 반대로 강원도 고성에 갔어요. 하행선이 텅 비어 있어서 양양까지 2시간 반 만에 도착했어요.

## 화진포 해변

고성에서도 북쪽 끝, 통일전망대 가는 길에 있어요. 9월 말이라 해수욕장은 이미 문을 닫았고, 백사장에 저 말고 두 가족 정도만 있었어요.

- 모래가 굵고 밟으면 ''뽀득'' 소리가 나요
- 해변 바로 뒤에 **화진포 호수**가 있어서, 바다와 호수를 한 번에 볼 수 있어요
- 호수 둘레길 약 16km. 저는 일부만 1시간 걸었어요

## 화진포의 성

해변 북쪽 언덕에 ''화진포의 성''이라는 오래된 건물이 있어요. 입장료 3천 원(주변 기념관 통합권). 건물 자체보다 2층 창문에서 내려다보는 바다가 좋아요. 창틀이 액자처럼 바다를 잘라 줘요.

## 거진항에서 늦은 점심

화진포에서 차로 10분 거리 거진항 앞 작은 식당 ''거진할매집''에서 도치알탕을 먹었어요. 원래 겨울 음식이라 메뉴에 없을 줄 알았는데 냉동해 둔 걸로 끓여 주셨어요. 1만 5천 원. 알이 톡톡 터지는 식감이 재밌어요.

## 이번 여행에서 느낀 것

1. 연휴 마지막 날 하행선은 **최고의 비밀 시간**이에요
2. 바다는 성수기가 지나야 비로소 바다 소리가 들려요
3. 강원도 북쪽 끝은 생각보다 가깝다

> 여름 바다가 사람들의 바다라면, 가을 바다는 바다 자신의 것 같아요.

돌아오는 길엔 역시 상행선이 막혀서 5시간 걸렸어요. 그래도 괜찮았어요.';
update post set content = '추석 연휴 전 주말에 통영을 다녀왔어요. 서울 남부터미널에서 고속버스로 4시간 10분. 길긴 하지만 창밖으로 남해가 보이기 시작하면 다 잊혀요.

![바위 해안](https://picsum.photos/id/12/1200/800)

## 첫째 날 동선

1. 통영종합버스터미널 도착 (오후 1시)
2. 시내버스로 강구안 이동, 점심으로 충무김밥
3. 동피랑 벽화마을
4. 서피랑 99계단과 서포루
5. 강구안 야경 보고 숙소

동피랑은 유명한 만큼 사람이 정말 많았어요. 좁은 골목에서 사진 찍으려고 줄을 서 있는 걸 보고 조금 지쳐서, 큰 기대 없이 건너편 서피랑으로 넘어갔는데 **여기가 이번 여행의 진짜 발견**이었어요.

## 서피랑 골목이 좋았던 이유

- 사람이 적어요. 토요일 오후 4시였는데 마주친 사람이 열 명도 안 됐어요.
- 99계단 끝 서포루에서 보는 강구안 쪽 풍경이 동피랑보다 넓게 트여 있어요.
- 골목마다 동네 할머니들이 내놓은 화분이 많아서 ''사는 동네'' 느낌이 나요.

서포루 정자에 앉아서 해 지는 걸 40분 넘게 봤어요. 바다 위로 작은 배들이 하나둘 불을 켜는데, 그 순서가 꼭 누가 정해 놓은 것 같았어요.

## 둘째 날

아침에 통영 중앙시장에서 해산물 구경하고, 미륵산 케이블카를 타려고 했는데 바람 때문에 운행이 멈춰서 못 탔어요. 대신 해저터널을 걸어서 지나갔는데 생각보다 짧았어요(483m).

> 통영은 계획한 곳보다 계획 안 한 곳이 더 기억에 남는 도시였어요.

다음엔 꼭 케이블카 타러 다시 가야겠어요.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목') and content = '추석 연휴 전 주말에 통영을 다녀왔어요. 서울 남부터미널에서 고속버스로 4시간 10분. 길긴 하지만 창밖으로 남해가 보이기 시작하면 다 잊혀요.

## 첫째 날 동선

1. 통영종합버스터미널 도착 (오후 1시)
2. 시내버스로 강구안 이동, 점심으로 충무김밥
3. 동피랑 벽화마을
4. 서피랑 99계단과 서포루
5. 강구안 야경 보고 숙소

동피랑은 유명한 만큼 사람이 정말 많았어요. 좁은 골목에서 사진 찍으려고 줄을 서 있는 걸 보고 조금 지쳐서, 큰 기대 없이 건너편 서피랑으로 넘어갔는데 **여기가 이번 여행의 진짜 발견**이었어요.

## 서피랑 골목이 좋았던 이유

- 사람이 적어요. 토요일 오후 4시였는데 마주친 사람이 열 명도 안 됐어요.
- 99계단 끝 서포루에서 보는 강구안 쪽 풍경이 동피랑보다 넓게 트여 있어요.
- 골목마다 동네 할머니들이 내놓은 화분이 많아서 ''사는 동네'' 느낌이 나요.

서포루 정자에 앉아서 해 지는 걸 40분 넘게 봤어요. 바다 위로 작은 배들이 하나둘 불을 켜는데, 그 순서가 꼭 누가 정해 놓은 것 같았어요.

## 둘째 날

아침에 통영 중앙시장에서 해산물 구경하고, 미륵산 케이블카를 타려고 했는데 바람 때문에 운행이 멈춰서 못 탔어요. 대신 해저터널을 걸어서 지나갔는데 생각보다 짧았어요(483m).

> 통영은 계획한 곳보다 계획 안 한 곳이 더 기억에 남는 도시였어요.

다음엔 꼭 케이블카 타러 다시 가야겠어요.';
update post set content = '금요일 퇴근하고 바로 강릉행 KTX를 탔어요. 강릉역에 밤 10시 반쯤 도착해서 주문진까지는 택시로 30분 정도 걸렸고, 항구 바로 앞 작은 모텔에서 잤어요. 이번 여행의 목적은 딱 하나, **새벽 경매 구경**이었거든요.

![새벽 바닷가](https://picsum.photos/id/13/1200/800)

## 새벽 5시의 주문진항

알람을 4시 50분에 맞춰 놓고 나갔는데 이미 항구는 불이 환했어요. 오징어 철은 아니라서 가자미, 도루묵, 문어가 대부분이었고요. 경매사 아저씨가 손가락으로 신호하는 걸 한참 보고 있었는데, 무슨 말인지는 하나도 못 알아들었어요. 그래도 그 소리랑 비린내랑 갈매기 소리가 섞인 분위기가 정말 좋았어요.

- 경매는 보통 새벽 5시~6시 사이 (배 들어오는 날에 따라 달라요)
- 관광객은 경매장 안쪽 줄 밖에서만 구경 가능
- 장화 신은 분들 사이로 다니니까 **미끄럽지 않은 신발** 필수

## 소돌 아들바위 공원

해 뜨는 시간에 맞춰 항구에서 북쪽으로 20분쯤 걸어서 소돌 아들바위 공원으로 갔어요. 바위 모양이 신기하게 깎여 있어서 사진 찍는 분들이 많았는데, 아침 6시에는 저 혼자였어요. 파도가 바위 틈으로 들어왔다 나가는 소리를 듣고 있으니까 한 주 동안 쌓인 메일 생각이 조금씩 지워지더라고요.

> 혼자 여행의 좋은 점은 어디서 얼마나 오래 앉아 있을지 아무하고도 상의하지 않아도 된다는 것.

## 아침밥

시장 안쪽 ''바다식당''이라는 작은 집에서 곰치국을 먹었어요. 1만 4천 원이었고 국물이 생각보다 맑아서 놀랐어요. 흐물흐물한 살이 처음엔 낯설었는데 김치랑 같이 먹으니까 금방 한 그릇 비웠어요.

다음에는 오징어 철인 가을에 다시 와 보고 싶어요.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록') and content = '금요일 퇴근하고 바로 강릉행 KTX를 탔어요. 강릉역에 밤 10시 반쯤 도착해서 주문진까지는 택시로 30분 정도 걸렸고, 항구 바로 앞 작은 모텔에서 잤어요. 이번 여행의 목적은 딱 하나, **새벽 경매 구경**이었거든요.

## 새벽 5시의 주문진항

알람을 4시 50분에 맞춰 놓고 나갔는데 이미 항구는 불이 환했어요. 오징어 철은 아니라서 가자미, 도루묵, 문어가 대부분이었고요. 경매사 아저씨가 손가락으로 신호하는 걸 한참 보고 있었는데, 무슨 말인지는 하나도 못 알아들었어요. 그래도 그 소리랑 비린내랑 갈매기 소리가 섞인 분위기가 정말 좋았어요.

- 경매는 보통 새벽 5시~6시 사이 (배 들어오는 날에 따라 달라요)
- 관광객은 경매장 안쪽 줄 밖에서만 구경 가능
- 장화 신은 분들 사이로 다니니까 **미끄럽지 않은 신발** 필수

## 소돌 아들바위 공원

해 뜨는 시간에 맞춰 항구에서 북쪽으로 20분쯤 걸어서 소돌 아들바위 공원으로 갔어요. 바위 모양이 신기하게 깎여 있어서 사진 찍는 분들이 많았는데, 아침 6시에는 저 혼자였어요. 파도가 바위 틈으로 들어왔다 나가는 소리를 듣고 있으니까 한 주 동안 쌓인 메일 생각이 조금씩 지워지더라고요.

> 혼자 여행의 좋은 점은 어디서 얼마나 오래 앉아 있을지 아무하고도 상의하지 않아도 된다는 것.

## 아침밥

시장 안쪽 ''바다식당''이라는 작은 집에서 곰치국을 먹었어요. 1만 4천 원이었고 국물이 생각보다 맑아서 놀랐어요. 흐물흐물한 살이 처음엔 낯설었는데 김치랑 같이 먹으니까 금방 한 그릇 비웠어요.

다음에는 오징어 철인 가을에 다시 와 보고 싶어요.';
update post set content = '여름휴가 3일 중 하루를 장호항에 썼어요. ''한국의 나폴리''라는 말은 좀 과하다고 생각했는데, 막상 가 보니 물이 정말 맑아서 할 말이 없더라고요.

![깊은 협곡](https://picsum.photos/id/1016/1200/800)

## 장호항 기본 정보

- 서울에서 자차로 약 3시간 반 (동해고속도로 → 7번 국도)
- 주차는 항구 근처 공영주차장, 성수기 주말엔 오전 9시 전에 차요
- 스노클링 장비 대여: 항구 앞 대여점들, 구명조끼+마스크+오리발 세트 1만 5천 원 정도
- 투명 카누도 있어요 (2인 30분 기준 2~3만 원대)

## 스노클링 해 보니

물 깊이가 허리에서 가슴 정도인 곳이 넓게 있어서 초보도 괜찮았어요. 바위 근처로 가면 작은 물고기 떼가 지나가고, 운 좋으면 성게도 보여요(만지면 안 돼요!). 저는 수영을 잘 못하는데 **구명조끼 입고 엎드려서 둥둥 떠 있기만 해도** 충분히 재밌었어요.

다만 8월 초인데도 물이 생각보다 차가웠어요. 동해는 냉수대가 들어오면 갑자기 수온이 확 떨어진다고 하더라고요. 30분 넘게 있으니까 입술이 파래져서 나왔어요.

## 챙겨 가면 좋은 것

- **아쿠아슈즈**: 바닥이 자갈이랑 바위라 맨발은 아파요
- 래시가드 상하의: 햇볕이 정말 세요
- 방수팩: 휴대폰 넣고 물속 사진 찍기
- 큰 수건이랑 얇은 담요: 물에서 나오면 추워요
- 따뜻한 물 담은 보온병

> 다음엔 냉수대 예보를 꼭 확인하고 가기. 국립해양조사원 누리집에서 볼 수 있어요.

오후엔 근처 용화해변 레일바이크를 타려고 했는데 매진이라 못 탔어요. 성수기에는 미리 예약하세요.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것') and content = '여름휴가 3일 중 하루를 장호항에 썼어요. ''한국의 나폴리''라는 말은 좀 과하다고 생각했는데, 막상 가 보니 물이 정말 맑아서 할 말이 없더라고요.

## 장호항 기본 정보

- 서울에서 자차로 약 3시간 반 (동해고속도로 → 7번 국도)
- 주차는 항구 근처 공영주차장, 성수기 주말엔 오전 9시 전에 차요
- 스노클링 장비 대여: 항구 앞 대여점들, 구명조끼+마스크+오리발 세트 1만 5천 원 정도
- 투명 카누도 있어요 (2인 30분 기준 2~3만 원대)

## 스노클링 해 보니

물 깊이가 허리에서 가슴 정도인 곳이 넓게 있어서 초보도 괜찮았어요. 바위 근처로 가면 작은 물고기 떼가 지나가고, 운 좋으면 성게도 보여요(만지면 안 돼요!). 저는 수영을 잘 못하는데 **구명조끼 입고 엎드려서 둥둥 떠 있기만 해도** 충분히 재밌었어요.

다만 8월 초인데도 물이 생각보다 차가웠어요. 동해는 냉수대가 들어오면 갑자기 수온이 확 떨어진다고 하더라고요. 30분 넘게 있으니까 입술이 파래져서 나왔어요.

## 챙겨 가면 좋은 것

- **아쿠아슈즈**: 바닥이 자갈이랑 바위라 맨발은 아파요
- 래시가드 상하의: 햇볕이 정말 세요
- 방수팩: 휴대폰 넣고 물속 사진 찍기
- 큰 수건이랑 얇은 담요: 물에서 나오면 추워요
- 따뜻한 물 담은 보온병

> 다음엔 냉수대 예보를 꼭 확인하고 가기. 국립해양조사원 누리집에서 볼 수 있어요.

오후엔 근처 용화해변 레일바이크를 타려고 했는데 매진이라 못 탔어요. 성수기에는 미리 예약하세요.';
update post set content = '가을 하프까지 정확히 2주. 오늘이 계획표상 **마지막 장거리**였다. 20km, 마지막 5km는 레이스 페이스.

![풀밭 사이로 난 길](https://picsum.photos/id/17/1200/800)

## 오늘 기록

- 거리: 20.1km
- 시간: 1:58:44
- 평균 페이스: 5''54"/km
- 마지막 5km 평균: **5''33"/km**
- 평균 심박: 151bpm
- 기온: 출발 14도, 도착 17도 — 드디어 가을이다

## 구간 메모

**0~15km (존2 + 살짝 위)**
6''05" 전후로 편하게. 7km, 14km에 젤 하나씩. 작년 하프 때처럼 초반에 욕심내지 않으려고 계속 시계만 봤다.

**15~20km (레이스 페이스)**
페이스를 올리니까 다리가 처음엔 무겁다가 2km쯤 지나니 오히려 리듬이 생겼다. 18km에서 작년처럼 다리가 풀릴까 걱정했는데, **이번엔 버텼다.** 롱런 20km를 세 번 한 게 확실히 다르다.

## 12주 돌아보기

- 계획한 훈련 72회 중 **66회** 완료
- 빠진 6회: 출장 3회, 장염 2회, 태풍 1회
- 부상 없음 (이게 제일 크다)
- 주간 최대 거리: 52km

## 남은 2주

1. 이번 주 주간 거리 **35km**, 다음 주 **22km**로 줄이기
2. 화요일 RP 3km × 2만 가볍게
3. 대회 사흘 전부터 탄수화물 비중 늘리기 (밥, 떡, 바나나)
4. 대회 신발은 지금 160km 신은 트레이너 그대로

> 2시간 안쪽 롱런 20km. 같은 거리를 2주 뒤엔 1km 더 길게, 몇 분 더 빨리.

작년엔 막연했던 서브2가 이제 숫자로 보인다. 남은 건 잘 쉬는 것뿐.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km') and content = '가을 하프까지 정확히 2주. 오늘이 계획표상 **마지막 장거리**였다. 20km, 마지막 5km는 레이스 페이스.

## 오늘 기록

- 거리: 20.1km
- 시간: 1:58:44
- 평균 페이스: 5''54"/km
- 마지막 5km 평균: **5''33"/km**
- 평균 심박: 151bpm
- 기온: 출발 14도, 도착 17도 — 드디어 가을이다

## 구간 메모

**0~15km (존2 + 살짝 위)**
6''05" 전후로 편하게. 7km, 14km에 젤 하나씩. 작년 하프 때처럼 초반에 욕심내지 않으려고 계속 시계만 봤다.

**15~20km (레이스 페이스)**
페이스를 올리니까 다리가 처음엔 무겁다가 2km쯤 지나니 오히려 리듬이 생겼다. 18km에서 작년처럼 다리가 풀릴까 걱정했는데, **이번엔 버텼다.** 롱런 20km를 세 번 한 게 확실히 다르다.

## 12주 돌아보기

- 계획한 훈련 72회 중 **66회** 완료
- 빠진 6회: 출장 3회, 장염 2회, 태풍 1회
- 부상 없음 (이게 제일 크다)
- 주간 최대 거리: 52km

## 남은 2주

1. 이번 주 주간 거리 **35km**, 다음 주 **22km**로 줄이기
2. 화요일 RP 3km × 2만 가볍게
3. 대회 사흘 전부터 탄수화물 비중 늘리기 (밥, 떡, 바나나)
4. 대회 신발은 지금 160km 신은 트레이너 그대로

> 2시간 안쪽 롱런 20km. 같은 거리를 2주 뒤엔 1km 더 길게, 몇 분 더 빨리.

작년엔 막연했던 서브2가 이제 숫자로 보인다. 남은 건 잘 쉬는 것뿐.';
update post set content = '회사 건강검진에서 체지방이랑 혈압 수치가 경계선이라는 말을 듣고 시작한 러닝. 오늘 드디어 **5km를 한 번도 안 걷고** 끝까지 뛰었다.

![아침 풀밭](https://picsum.photos/id/18/1200/800)

## 3주 동안 한 것

처음엔 1km도 못 뛰고 숨이 넘어갔다. 그래서 걷기랑 뛰기를 섞는 방식으로 시작했다.

- **1주차**: 1분 뛰고 2분 걷기 × 8세트 (주 3회)
- **2주차**: 3분 뛰고 1분 30초 걷기 × 6세트
- **3주차**: 8분 뛰고 1분 걷기 × 3세트, 그리고 오늘 연속 도전

## 오늘 기록

- 거리: 5.02km
- 시간: 36분 48초
- 평균 페이스: **7''20"/km**
- 평균 심박: 168bpm (좀 높다)
- 장소: 집 앞 하천 산책로, 왕복 코스

마지막 1km는 다리가 무거워서 거의 걷는 속도였는데, 그래도 멈추지 않았다는 게 중요하다.

## 느낀 점

> 숨이 찬 건 폐가 아니라 속도 문제였다.

처음엔 무조건 빨리 뛰려고 했다. 옆에서 할아버지가 걷는 속도로 천천히 뛰는 걸 보고 따라 했더니 오히려 오래 갔다. **옆 사람이랑 대화할 수 있는 속도**가 지금 나한테 맞는 속도인 것 같다.

다음 목표는 5km 35분 안쪽, 그리고 언젠가 10km 대회.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다') and content = '회사 건강검진에서 체지방이랑 혈압 수치가 경계선이라는 말을 듣고 시작한 러닝. 오늘 드디어 **5km를 한 번도 안 걷고** 끝까지 뛰었다.

## 3주 동안 한 것

처음엔 1km도 못 뛰고 숨이 넘어갔다. 그래서 걷기랑 뛰기를 섞는 방식으로 시작했다.

- **1주차**: 1분 뛰고 2분 걷기 × 8세트 (주 3회)
- **2주차**: 3분 뛰고 1분 30초 걷기 × 6세트
- **3주차**: 8분 뛰고 1분 걷기 × 3세트, 그리고 오늘 연속 도전

## 오늘 기록

- 거리: 5.02km
- 시간: 36분 48초
- 평균 페이스: **7''20"/km**
- 평균 심박: 168bpm (좀 높다)
- 장소: 집 앞 하천 산책로, 왕복 코스

마지막 1km는 다리가 무거워서 거의 걷는 속도였는데, 그래도 멈추지 않았다는 게 중요하다.

## 느낀 점

> 숨이 찬 건 폐가 아니라 속도 문제였다.

처음엔 무조건 빨리 뛰려고 했다. 옆에서 할아버지가 걷는 속도로 천천히 뛰는 걸 보고 따라 했더니 오히려 오래 갔다. **옆 사람이랑 대화할 수 있는 속도**가 지금 나한테 맞는 속도인 것 같다.

다음 목표는 5km 35분 안쪽, 그리고 언젠가 10km 대회.';
update post set content = '더위가 한풀 꺾이자마자 베란다 대정리를 했어요. 여름 동안 미뤄 둔 분갈이 네 개를 한꺼번에 하고, 겨울 준비까지 했습니다.

![초록 잎들](https://picsum.photos/id/19/1200/800)

## 분갈이 4종

| 식물 | 이전 → 새 화분 | 흙 배합 |
|---|---|---|
| 몬스테라 (3년차) | 21cm → 25cm | 상토5:펄라이트3:바크2 |
| 고무나무 | 15cm → 18cm | 상토4:펄라이트3:마사토3 |
| 스킨답서스 (물꽂이 출신) | 12cm → 15cm | 기본 배합 |
| 올리브나무 | 토분 그대로, 겉흙만 교체 | 마사토 위주 |

몬스테라는 처음 분갈이했던 그 아이예요. 3년 만에 잎이 열여섯 장이 됐고, 화분에서 빼는 데만 20분이 걸렸어요. 뿌리가 꽉 차서 화분을 결국 가위로 잘라냈어요.

## 가을에 분갈이하는 이유

- 봄보다는 덜 권장되지만, **9월 중순~10월 초**는 아직 따뜻해서 뿌리가 자리 잡을 시간이 있어요
- 한여름은 더위 스트레스, 겨울은 성장이 멈춰서 피해요
- 단, 가을 분갈이는 **화분을 한 치수만** 키워요. 너무 크면 겨울 동안 흙이 안 말라요

## 월동 준비 체크리스트

- [ ] 한파 알림 켜기 (최저 5도)
- [ ] 냉해에 약한 아이들(알로카시아, 칼라데아) 실내 자리 미리 정하기
- [ ] 화분 받침 아래 스티로폼 판 깔기
- [ ] 비료는 10월 말에 마지막
- [ ] 가습기 필터 교체

## 정리하고 나니

여름 내내 무성하게 엉켜 있던 베란다가 훤해졌어요. 마른 잎 한 봉지, 깨진 화분 두 개, 쓰다 남은 흙 세 봉지가 나왔습니다. 블로그를 시작한 지 3년 반, 화분은 열네 개, 아직도 매일 아침 흙을 찔러 봐요.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기') and content = '더위가 한풀 꺾이자마자 베란다 대정리를 했어요. 여름 동안 미뤄 둔 분갈이 네 개를 한꺼번에 하고, 겨울 준비까지 했습니다.

## 분갈이 4종

| 식물 | 이전 → 새 화분 | 흙 배합 |
|---|---|---|
| 몬스테라 (3년차) | 21cm → 25cm | 상토5:펄라이트3:바크2 |
| 고무나무 | 15cm → 18cm | 상토4:펄라이트3:마사토3 |
| 스킨답서스 (물꽂이 출신) | 12cm → 15cm | 기본 배합 |
| 올리브나무 | 토분 그대로, 겉흙만 교체 | 마사토 위주 |

몬스테라는 처음 분갈이했던 그 아이예요. 3년 만에 잎이 열여섯 장이 됐고, 화분에서 빼는 데만 20분이 걸렸어요. 뿌리가 꽉 차서 화분을 결국 가위로 잘라냈어요.

## 가을에 분갈이하는 이유

- 봄보다는 덜 권장되지만, **9월 중순~10월 초**는 아직 따뜻해서 뿌리가 자리 잡을 시간이 있어요
- 한여름은 더위 스트레스, 겨울은 성장이 멈춰서 피해요
- 단, 가을 분갈이는 **화분을 한 치수만** 키워요. 너무 크면 겨울 동안 흙이 안 말라요

## 월동 준비 체크리스트

- [ ] 한파 알림 켜기 (최저 5도)
- [ ] 냉해에 약한 아이들(알로카시아, 칼라데아) 실내 자리 미리 정하기
- [ ] 화분 받침 아래 스티로폼 판 깔기
- [ ] 비료는 10월 말에 마지막
- [ ] 가습기 필터 교체

## 정리하고 나니

여름 내내 무성하게 엉켜 있던 베란다가 훤해졌어요. 마른 잎 한 봉지, 깨진 화분 두 개, 쓰다 남은 흙 세 봉지가 나왔습니다. 블로그를 시작한 지 3년 반, 화분은 열네 개, 아직도 매일 아침 흙을 찔러 봐요.';
update post set content = '날씨가 좋아서 남편이랑 아침 일찍 **양재 화훼시장**에 다녀왔어요. 예전부터 올리브나무를 들이고 싶었는데, 사진으로만 고르기엔 수형이 너무 제각각이라 직접 보고 싶었거든요.

![꽃](https://picsum.photos/id/106/1200/800)

## 가는 길

양재역에서 버스로 갈아타고 15분 정도. **오전 9시 전**에 도착했는데 이미 화분 카트를 끄는 사람들이 꽤 있었어요. 주말 오후엔 주차도 어렵고 사람도 많다고 해서 일찍 나선 게 정답이었어요.

## 둘러본 것

- 관엽 동: 몬스테라, 필로덴드론 희귀종이 가득. 가격표 보고 조용히 내려놨어요.
- 허브·과실수 동: 레몬나무, 블루베리, 올리브. 여기서 한 시간을 보냈어요.
- 자재 동: 토분, 바크, 펄라이트 대용량. 동네보다 확실히 저렴해요.

## 올리브나무 고른 기준

1. **줄기가 곧고 가지가 고르게** 퍼진 것
2. 잎 뒷면이 은빛이 선명하고 벌레 흔적 없는 것
3. 화분 아래로 뿌리가 너무 꽉 차 있지 않은 것

사장님이 "베란다에서 키울 거면 바람 잘 통하게, 겨울엔 꼭 들여놓아라" 하셨어요. 키 80cm짜리를 골랐고, 토분과 바크 한 포대까지 함께 샀어요.

## 점심

근처 국숫집에서 비빔국수 한 그릇. 양손 가득 흙 포대를 들고 버스 타기는 무리라 택시로 돌아왔어요.

지금 올리브나무는 베란다 제일 밝은 자리에 있어요. 우리 집 식구가 열네 개가 됐습니다.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요') and content = '날씨가 좋아서 남편이랑 아침 일찍 **양재 화훼시장**에 다녀왔어요. 예전부터 올리브나무를 들이고 싶었는데, 사진으로만 고르기엔 수형이 너무 제각각이라 직접 보고 싶었거든요.

## 가는 길

양재역에서 버스로 갈아타고 15분 정도. **오전 9시 전**에 도착했는데 이미 화분 카트를 끄는 사람들이 꽤 있었어요. 주말 오후엔 주차도 어렵고 사람도 많다고 해서 일찍 나선 게 정답이었어요.

## 둘러본 것

- 관엽 동: 몬스테라, 필로덴드론 희귀종이 가득. 가격표 보고 조용히 내려놨어요.
- 허브·과실수 동: 레몬나무, 블루베리, 올리브. 여기서 한 시간을 보냈어요.
- 자재 동: 토분, 바크, 펄라이트 대용량. 동네보다 확실히 저렴해요.

## 올리브나무 고른 기준

1. **줄기가 곧고 가지가 고르게** 퍼진 것
2. 잎 뒷면이 은빛이 선명하고 벌레 흔적 없는 것
3. 화분 아래로 뿌리가 너무 꽉 차 있지 않은 것

사장님이 "베란다에서 키울 거면 바람 잘 통하게, 겨울엔 꼭 들여놓아라" 하셨어요. 키 80cm짜리를 골랐고, 토분과 바크 한 포대까지 함께 샀어요.

## 점심

근처 국숫집에서 비빔국수 한 그릇. 양손 가득 흙 포대를 들고 버스 타기는 무리라 택시로 돌아왔어요.

지금 올리브나무는 베란다 제일 밝은 자리에 있어요. 우리 집 식구가 열네 개가 됐습니다.';
update post set content = '베이킹 2년 반 만에 처음으로 홀케이크에 도전했다. 지름 15cm, 딸기 생크림. 가족 모임에 들고 가겠다고 큰소리를 쳐 버려서 물러설 데가 없었다.

![딸기](https://picsum.photos/id/1080/1200/800)

## 제누와즈 (15cm 1호)

- 계란 2개, 설탕 60g
- 박력분 60g
- 우유 15g + 버터 15g (따뜻하게 녹여 두기)

1. 계란과 설탕을 볼에 넣고 중탕으로 37도까지 데운다.
2. 핸드믹서 고속으로 5분, 저속으로 1분. 반죽을 떨어뜨렸을 때 **리본처럼 쌓였다가 천천히 사라지면** 된다.
3. 박력분을 두 번 체 쳐 넣고 바닥부터 크게 30번 정도 섞는다.
4. 반죽 한 주걱을 우유버터에 먼저 섞고, 그걸 다시 본반죽에 넣는다.
5. 170도 28분. 꺼내자마자 10cm 높이에서 한 번 떨어뜨린다.

## 아이싱

- 생크림 400ml + 설탕 32g, 단단하게 휘핑
- 딸기 한 팩, 반은 슬라이스, 반은 장식용
- 시럽: 물 50g + 설탕 25g

시트를 세 장으로 자르고, 시럽 바르고, 크림 바르고, 딸기 깔고를 반복했다.

## 솔직한 결과

- 시트는 성공. 꺼지지 않고 4.5cm까지 올라왔다.
- 아이싱은 실패에 가까웠다. 옆면이 울퉁불퉁해서 결국 딸기를 옆에도 둘러 붙여 가렸다.

그래도 조카가 "이거 진짜 이모가 만든 거야?" 하고 물었을 때 다 괜찮아졌다. 돌림판은 내년 선물 목록 1순위.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크') and content = '베이킹 2년 반 만에 처음으로 홀케이크에 도전했다. 지름 15cm, 딸기 생크림. 가족 모임에 들고 가겠다고 큰소리를 쳐 버려서 물러설 데가 없었다.

## 제누와즈 (15cm 1호)

- 계란 2개, 설탕 60g
- 박력분 60g
- 우유 15g + 버터 15g (따뜻하게 녹여 두기)

1. 계란과 설탕을 볼에 넣고 중탕으로 37도까지 데운다.
2. 핸드믹서 고속으로 5분, 저속으로 1분. 반죽을 떨어뜨렸을 때 **리본처럼 쌓였다가 천천히 사라지면** 된다.
3. 박력분을 두 번 체 쳐 넣고 바닥부터 크게 30번 정도 섞는다.
4. 반죽 한 주걱을 우유버터에 먼저 섞고, 그걸 다시 본반죽에 넣는다.
5. 170도 28분. 꺼내자마자 10cm 높이에서 한 번 떨어뜨린다.

## 아이싱

- 생크림 400ml + 설탕 32g, 단단하게 휘핑
- 딸기 한 팩, 반은 슬라이스, 반은 장식용
- 시럽: 물 50g + 설탕 25g

시트를 세 장으로 자르고, 시럽 바르고, 크림 바르고, 딸기 깔고를 반복했다.

## 솔직한 결과

- 시트는 성공. 꺼지지 않고 4.5cm까지 올라왔다.
- 아이싱은 실패에 가까웠다. 옆면이 울퉁불퉁해서 결국 딸기를 옆에도 둘러 붙여 가렸다.

그래도 조카가 "이거 진짜 이모가 만든 거야?" 하고 물었을 때 다 괜찮아졌다. 돌림판은 내년 선물 목록 1순위.';
update post set content = '토요일 오후, 망원시장 구경하고 근처 골목을 걷다가 들어간 카페예요. 이름이 ''오후세시''인데 실제로 들어간 시간이 3시 5분이라 괜히 웃겼어요.

![커피를 내리는 바리스타](https://picsum.photos/id/1060/1200/800)

## 공간

- 테이블 4개, 창가 자리 2개
- 원목 가구랑 하얀 벽, 음악은 작게
- 노트북 작업하는 분은 없고 다들 수다 떠는 분위기

## 디저트 3종

같이 간 친구랑 하나씩 다 시켜서 나눠 먹었어요.

### 1. 까눌레 (4,000원)

겉이 **진짜 단단하게 캐러멜화**돼 있어요. 포크로 자르면 ''딱'' 소리 날 정도. 속은 촉촉하고 바닐라 향이 진해요. 셋 중 1등.

### 2. 얼그레이 파운드 (5,500원)

향은 좋은데 조금 퍽퍽했어요. 커피 없이 먹으면 목이 막혀요. 크림을 곁들여 주면 좋겠다는 생각.

### 3. 딸기 쇼트케이크 (7,500원)

3월이라 딸기가 실하고 달았어요. 근데 생크림이 너무 가벼워서 금방 녹아내려요. 사진 찍을 시간이 없어요ㅎㅎ

## 커피

라떼 5,000원. 원두는 산미 있는 쪽이랑 고소한 쪽 중에 고를 수 있는데, 저는 고소한 쪽. 디저트가 달아서 산미 있는 걸로 했으면 더 잘 어울렸을 것 같아요.

## 정리

| 메뉴 | 한 줄 평 |
|---|---|
| 까눌레 | 이거 먹으러 또 감 |
| 얼그레이 파운드 | 커피 필수 |
| 딸기 쇼트케이크 | 맛있지만 빨리 먹을 것 |

> 카페 디저트는 하나만 확실하면 다시 가게 돼요. 여기는 까눌레가 그 하나.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기') and content = '토요일 오후, 망원시장 구경하고 근처 골목을 걷다가 들어간 카페예요. 이름이 ''오후세시''인데 실제로 들어간 시간이 3시 5분이라 괜히 웃겼어요.

## 공간

- 테이블 4개, 창가 자리 2개
- 원목 가구랑 하얀 벽, 음악은 작게
- 노트북 작업하는 분은 없고 다들 수다 떠는 분위기

## 디저트 3종

같이 간 친구랑 하나씩 다 시켜서 나눠 먹었어요.

### 1. 까눌레 (4,000원)

겉이 **진짜 단단하게 캐러멜화**돼 있어요. 포크로 자르면 ''딱'' 소리 날 정도. 속은 촉촉하고 바닐라 향이 진해요. 셋 중 1등.

### 2. 얼그레이 파운드 (5,500원)

향은 좋은데 조금 퍽퍽했어요. 커피 없이 먹으면 목이 막혀요. 크림을 곁들여 주면 좋겠다는 생각.

### 3. 딸기 쇼트케이크 (7,500원)

3월이라 딸기가 실하고 달았어요. 근데 생크림이 너무 가벼워서 금방 녹아내려요. 사진 찍을 시간이 없어요ㅎㅎ

## 커피

라떼 5,000원. 원두는 산미 있는 쪽이랑 고소한 쪽 중에 고를 수 있는데, 저는 고소한 쪽. 디저트가 달아서 산미 있는 걸로 했으면 더 잘 어울렸을 것 같아요.

## 정리

| 메뉴 | 한 줄 평 |
|---|---|
| 까눌레 | 이거 먹으러 또 감 |
| 얼그레이 파운드 | 커피 필수 |
| 딸기 쇼트케이크 | 맛있지만 빨리 먹을 것 |

> 카페 디저트는 하나만 확실하면 다시 가게 돼요. 여기는 까눌레가 그 하나.';
update post set content = '회사 동료들한테 나눠 주려고 구움과자를 구웠다. 휘낭시에랑 마들렌, 한 번에 둘 다. 비슷해 보이지만 만들어 보면 성격이 꽤 다르다.

![산딸기](https://picsum.photos/id/102/1200/800)

## 휘낭시에 (12개)

- 계란 흰자 100g
- 슈가파우더 90g, 아몬드가루 45g, 박력분 35g
- **브라운버터 90g**
- 꿀 10g

1. 흰자를 거품 내지 않고 풀기만 한다.
2. 가루류를 체 쳐 넣고 섞은 뒤 꿀.
3. 따뜻한 브라운버터를 세 번에 나눠 넣는다.
4. 틀에 80%, 200도 12분.

## 마들렌 (12개)

- 계란 2개, 설탕 80g, 레몬 제스트 1/2개
- 박력분 90g, 베이킹파우더 3g
- 녹인 버터 90g

1. 계란, 설탕, 제스트를 섞고 가루를 넣는다.
2. 녹인 버터를 넣고 **냉장 2시간 휴지.**
3. 버터 바르고 밀가루 턴 틀에 짜서 190도 5분, 170도로 낮춰 6분.

## 다른 점 정리

- **계란**: 휘낭시에는 흰자만, 마들렌은 전란
- **버터**: 휘낭시에는 브라운버터, 마들렌은 그냥 녹인 버터
- **맛**: 휘낭시에는 고소하고 쫀득, 마들렌은 폭신하고 상큼

마들렌 배꼽이 처음으로 봉긋하게 나왔다. 차가운 반죽을 뜨거운 오븐에 넣는 온도 차가 배꼽을 만든다고 한다. 남은 노른자 네 개는 내일 커스터드가 될 예정.' where post_id = (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘') and content = '회사 동료들한테 나눠 주려고 구움과자를 구웠다. 휘낭시에랑 마들렌, 한 번에 둘 다. 비슷해 보이지만 만들어 보면 성격이 꽤 다르다.

## 휘낭시에 (12개)

- 계란 흰자 100g
- 슈가파우더 90g, 아몬드가루 45g, 박력분 35g
- **브라운버터 90g**
- 꿀 10g

1. 흰자를 거품 내지 않고 풀기만 한다.
2. 가루류를 체 쳐 넣고 섞은 뒤 꿀.
3. 따뜻한 브라운버터를 세 번에 나눠 넣는다.
4. 틀에 80%, 200도 12분.

## 마들렌 (12개)

- 계란 2개, 설탕 80g, 레몬 제스트 1/2개
- 박력분 90g, 베이킹파우더 3g
- 녹인 버터 90g

1. 계란, 설탕, 제스트를 섞고 가루를 넣는다.
2. 녹인 버터를 넣고 **냉장 2시간 휴지.**
3. 버터 바르고 밀가루 턴 틀에 짜서 190도 5분, 170도로 낮춰 6분.

## 다른 점 정리

- **계란**: 휘낭시에는 흰자만, 마들렌은 전란
- **버터**: 휘낭시에는 브라운버터, 마들렌은 그냥 녹인 버터
- **맛**: 휘낭시에는 고소하고 쫀득, 마들렌은 폭신하고 상큼

마들렌 배꼽이 처음으로 봉긋하게 나왔다. 차가운 반죽을 뜨거운 오븐에 넣는 온도 차가 배꼽을 만든다고 한다. 남은 노른자 네 개는 내일 커스터드가 될 예정.';

commit;
\echo 예시 글 사진을 맞췄습니다: 26장
