# Interview guide – scenario questions mapped to this repo

Format: **Question** -> short model answer -> *where to show it in the code*.

---
## 1. Architecture & decomposition

**Why microservices here? What are the downsides?**
Independent deploy/scale/ownership per business capability (orders scale differently from notifications), fault
isolation, polyglot freedom. Costs: distributed data consistency, network failures, observability and ops complexity.
Honest answer: start with a modular monolith unless team size / scaling needs justify it.

**How did you split services? Database per service?**
By business capability / bounded context (DDD). Each service owns its schema (`userdb`, `orderdb` ...) and nobody
reads another's tables; integration is via REST (sync) or events (async). *Show: four Flyway folders, `init-databases.sql`.*

**Sync vs async communication - when which?**
Sync (REST/Feign) when the caller needs the answer now (order -> user active check). Async events when work can
happen later or many consumers care (payment, notification). Async removes temporal coupling and survives downstream outages.
*Show: `UserClient` (sync) vs Kafka listeners (async).*

**Why an API Gateway?**
Single entry: routing, edge auth, rate limiting, correlation id, TLS offload, hides internal topology, cross-cutting
policies. Do not put business logic in it. Services still validate JWTs (zero trust). *Show: `GatewaySecurityConfig`, `application.yml` routes.*

**Eureka vs Kubernetes service discovery?**
Eureka = client-side discovery for VMs/Docker-compose. On Kubernetes the platform already provides DNS + Services +
load balancing, so Eureka is redundant; we switch it off with env vars. *Show: `USER_SERVICE_URI`, `EUREKA_CLIENT_ENABLED`, k8s/configmap.yaml.*

---
## 2. Consistency, events, reliability  (the most-asked area)

**A user places an order. Walk me through everything that happens.**
Gateway validates JWT -> order-service checks Redis idempotency key -> Feign call to user-service (circuit breaker) ->
ONE DB transaction saves order(PENDING) + outbox row -> relay publishes `order.created` -> payment-service charges ->
emits `payment.completed/failed` -> order-service confirms/cancels; notification-service e-mails via RabbitMQ.
*Show: `OrderFacade`, `OrderService.create`, `OutboxPublisher`, `PaymentService.process`, `PaymentEventsListener`.*

**Payment fails after the order exists. What now?**
Saga compensation: `payment.failed` -> order set to CANCELLED. No distributed transaction (2PC) - each step is local
and reversible. *Show: `markCancelledByPaymentFailure`; Postman request 9.*

**Payment succeeded but the user cancelled meanwhile?**
Order is already CANCELLED: handler logs a warning and ignores. Real systems then trigger a *refund* (compensating
action) - known gap, say so. *Show: `markConfirmed` else-branch.*

**Choreography vs orchestration saga?**
Here choreography (services react to events; simple, decentralised, harder to see the whole flow). Orchestration
(Temporal / Step Functions / a coordinator service) gives one place for flow, timeouts and compensation - better for long, complex flows.

**What is the dual-write problem? How did you solve it?**
Writing DB and Kafka separately can leave them inconsistent (DB commits, process dies before send). Solution:
transactional outbox - event row in same tx, relay publishes after. Gives at-least-once. Alternatives: CDC (Debezium), Kafka transactions.
*Show: `OutboxEvent`, `OutboxPublisher`, `FOR UPDATE SKIP LOCKED`.*

**Why `FOR UPDATE SKIP LOCKED`?**
Multiple pods poll the outbox concurrently; locked rows are skipped so no duplicate sends and no blocking.

**Kafka is at-least-once. How do you avoid double charging?**
Idempotent consumer: `existsByOrderId` + DB `UNIQUE(order_id)` as final guard (race-proof); duplicates are skipped.
Producer side: `enable.idempotence=true`, `acks=all`. "Exactly-once" end-to-end = at-least-once + idempotent processing.
*Show: `PaymentService`, `V1__create_payments.sql`, `PaymentServiceTest`.*

**Client retries POST /orders because of a timeout - duplicate order?**
`Idempotency-Key` header stored in Redis with `SET NX` (atomic); replay returns the original order. Key removed on failure so retry works.
*Show: `OrderFacade.placeOrder`.*

**A poison message keeps failing. What happens?**
`ErrorHandlingDeserializer` handles undeserializable payloads; `DefaultErrorHandler` retries with exponential back-off, then
publishes to `<topic>.DLT`; partition keeps moving. Monitor DLT + replay after fixing. *Show: `KafkaCommonConfig`.*

**Ordering in Kafka?**
Guaranteed only within a partition. We key by orderId so all events of one order are ordered. Consumer group: replicas
of a service share partitions (max useful consumers = partitions).

**Consumer lag grows - what do you do?**
Check lag per partition (kafka-consumer-groups / CloudWatch MSK), slow handler? DB slow? Scale replicas up to partition
count, increase partitions (re-keying caveat), tune `max.poll.records`, move slow work async, check rebalances.

**Kafka vs RabbitMQ - why both here?**
Kafka: durable replayable log, high throughput, many independent consumer groups -> domain *facts*. RabbitMQ: smart broker,
per-message ack, routing, TTL, DLX/DLQ -> *tasks* (send e-mail). Honest note: one broker would do for this scope; shown to demonstrate trade-offs.
*Show: `DomainEventsListener` -> `RabbitConfig` -> `EmailConsumer`.*

**RabbitMQ message keeps failing?**
Retry (3x, exponential) -> `default-requeue-rejected=false` -> dead-lettered to `notifications.email.dlq` instead of looping.

---
## 3. Security

**JWT vs server sessions?**
JWT: stateless, scales horizontally, no shared session store; downsides: hard revocation, token size, stolen token valid until expiry.
Mitigate: short expiry + refresh tokens, deny-list in Redis for emergencies, HTTPS only.

**How do OAuth2 and JWT relate? What is your setup?**
OAuth2 is the authorization framework (flows); JWT is a token format. Each service is an **OAuth2 Resource Server**
validating bearer JWTs. user-service plays authorization-server for the demo (password login). Production: Keycloak/Cognito/Auth0,
Authorization Code + PKCE for SPAs, client-credentials for service-to-service. *Show: common-lib `SecurityConfig`, `TokenService`.*

**HS256 vs RS256?**
HS256 = shared secret (every validator could also forge tokens). RS256 = private key signs, public JWKS verifies - safer for many services.

**How is RBAC done?**
`roles` claim -> `ROLE_*` authorities -> `@PreAuthorize("hasRole('ADMIN')")`; ownership rules in expressions (`#id.toString() == authentication.name`)
or service filters (orders return 404 for other users' orders). *Show: `UserController`, `OrderService.get`.*

**Why validate JWT again in each service if gateway did it?**
Zero trust: a pod can be reached inside the cluster bypassing the gateway; defence in depth.

**Passwords?** BCrypt cost 12, 72-byte limit validated, same error for unknown user/wrong password (no enumeration), rate limit login at gateway.

**Secrets?** Never in Git/images. K8s Secret synced from AWS Secrets Manager, IRSA for AWS access, rotate JWT keys.

---
## 4. Database, JPA/Hibernate, performance

**What is the N+1 problem and where did you fix it?**
One query for parents + N for children. Fixes: `@EntityGraph` / join fetch (`findWithItemsById`), `@BatchSize` on roles,
DTO projections for lists (`OrderListItem` never touches items), `open-in-view=false` so lazy loading outside the tx fails loudly.

**Explain your indexes.**
- `orders(user_id, created_at DESC)`: equality column first, sort column second -> "my orders newest first" with no sort step.
- Partial `orders(created_at) WHERE status='PENDING'` and `outbox_event ... WHERE published=false`: tiny, hot subset only.
- `users(lower(email))` unique functional index: case-insensitive uniqueness; query must use `lower(email)=lower(?)` to hit it
  (derived `IgnoreCase` uses `upper()` and would miss it - I wrote the `@Query` on purpose).
- FK index `order_items(order_id)`: PostgreSQL does not create it automatically.
How to verify: `EXPLAIN (ANALYZE, BUFFERS)`; look for Seq Scan vs Index Scan, rows estimate vs actual.

**A query got slow in production. Process?**
Find it (pg_stat_statements, slow query log, APM) -> `EXPLAIN ANALYZE` -> missing/unused index, bad stats (`ANALYZE`), N+1, big OFFSET,
SELECT * -> fix -> add index with `CREATE INDEX CONCURRENTLY` -> verify -> monitor.

**OFFSET pagination vs keyset?** OFFSET scans and discards rows (slow at depth, unstable under inserts). Keyset (`WHERE (created_at,id) < (?,?) ORDER BY ... LIMIT`) is constant-time.

**Stored procedures - pros/cons? Where used?**
Pros: set-based work next to data, fewer round trips, privilege encapsulation. Cons: logic outside Java (testing, versioning, portability, observability).
Used sparingly: `sp_deactivate_dormant_users`, `sp_cancel_stale_orders` (bulk maintenance), `fn_user_order_summary` (aggregation). Versioned via Flyway.

**Optimistic vs pessimistic locking?**
`@Version` (optimistic) for low-contention conflicts: detects lost updates -> 409. Pessimistic (`SELECT ... FOR UPDATE`) for high contention/critical sections
(we use it for the outbox with SKIP LOCKED).

**Transactions: any pitfalls?**
Self-invocation bypasses proxy; `@Transactional` on private methods ignored; do not hold a transaction across remote calls
(hence `OrderFacade` vs `OrderService`); rollback only on unchecked exceptions by default; events after commit via `@TransactionalEventListener(AFTER_COMMIT)`.

**How do you change the schema without downtime?**
Flyway versioned migrations, `ddl-auto=validate`. Expand/contract: add nullable column -> deploy code writing both -> backfill -> switch reads -> drop old later.
Old and new app versions must both work against the schema during a rolling deploy.

**Connection pool sizing?** Small pool per pod (10); total = pods x pool must stay below DB max_connections (use RDS Proxy / PgBouncer when scaling out). Fail fast `connection-timeout`.

---
## 5. Redis & caching

**Where is Redis used?** Cache-aside for users (TTL 10 min), idempotency keys (SET NX), gateway rate limiter (token bucket).
**Cache invalidation?** TTL bounds staleness; `@CacheEvict` on change (bulk procedure evicts all). Authorization checked *before* the cache (controller), never cached.
**Cache stampede / penetration / avalanche?** Stampede: lock/single-flight or jittered TTL; penetration (missing keys): cache nulls briefly or Bloom filter; avalanche: randomise TTLs, Redis HA.
**Redis down?** Cache = degrade to DB (short `timeout`, custom `CacheErrorHandler` to swallow errors); rate limiter/idempotency decide fail-open vs fail-closed per feature.

---
## 6. Resilience

**Service B is slow/down and A calls it.** Timeouts (1s/2s here), circuit breaker (opens at 50% failures over 20 calls, half-open probes),
retry with back-off only for idempotent calls, bulkhead, fallback. *Show: `application.yml` of order-service, `UserClientFallback`.*
**Fail open or fail closed?** Business decision: user-active check fails open (token already proves identity); payments fail closed.
**Retry storms?** Back-off + jitter, circuit breaker, budgets; never retry non-idempotent calls without an idempotency key.
**Graceful shutdown?** `server.shutdown=graceful`, readiness drops first, `terminationGracePeriodSeconds` > drain time.

---
## 7. Observability - "make defects faster to trace and fix"

**A customer says "my order is stuck". How do you debug?**
1. Get the `traceId`/correlation id (returned in every error body & `X-Correlation-Id` header) or the orderId.
2. Search logs (Kibana/CloudWatch Insights): `correlationId:"..."` -> gateway -> order -> (Kafka) -> payment.
3. Check order row status, `outbox_event.published`, Kafka consumer lag, DLT topic, RabbitMQ DLQ.
4. Check metrics/alerts (error rate, latency p99), then fix, add a test, add an alert.
*Show: `CorrelationIdFilter`, `logback-spring.xml` (JSON), `GlobalExceptionHandler` (traceId), log keys like `order_created orderId=`.*

**Structured logging - what and why?** One JSON object per line with fixed fields (service, level, correlationId, message) -> queryable; log events as `event_name key=value`; never log secrets/PII; levels: 4xx WARN, 5xx ERROR with stack trace once.
**Three pillars?** Logs, metrics (Micrometer -> Prometheus; `/actuator/prometheus`), traces (add Micrometer Tracing + OTel -> X-Ray/Jaeger; also propagate the id in Kafka headers).
**Health probes?** liveness (restart me), readiness (send me traffic), startup (slow boot). Do not put DB checks in liveness.
**Global exception handling design?** One `@RestControllerAdvice`, stable `ApiError` JSON (code, message, path, traceId, field errors), domain exceptions with status+code, no stack traces to clients.

---
## 8. Design patterns & SOLID (name them with file names)

| Pattern / principle | In this repo |
|---|---|
| API Gateway, Service Discovery | api-gateway, Eureka |
| Saga (choreography) | order <-> payment events |
| Transactional Outbox | `OutboxEvent`, `OutboxPublisher` |
| Idempotent Consumer | `PaymentService`, `NotificationProcessor` |
| Circuit Breaker / Fallback | Feign + Resilience4j, `UserClientFallback` |
| Cache-Aside | `@Cacheable` in `UserService` |
| Strategy / Adapter / Port | `PaymentGateway` (+Mock), `EmailSender` (+Logging) |
| Facade | `OrderFacade` orchestrates idempotency + remote call + local tx |
| Repository, DTO, Builder | Spring Data repos, records, `JwtClaimsSet.builder()` |
| Observer / Domain events | Spring `@TransactionalEventListener`, Kafka |
| Dead Letter Queue, DB per service | Kafka DLT, RabbitMQ DLQ, 4 databases |
| **S**ingle responsibility | `OrderService` (tx logic) vs `OrderFacade` (orchestration) vs `OutboxPublisher` (relay) vs controllers (HTTP only) |
| **O**pen/closed | add a new gateway/e-mail provider by adding a class, no change to services |
| **L**iskov | any `PaymentGateway`/`EmailSender` impl is substitutable (tests use mocks) |
| **I**nterface segregation | small ports: `PaymentGateway.charge`, `EmailSender.send` |
| **D**ependency inversion | services depend on interfaces; constructor injection everywhere (`@RequiredArgsConstructor`) |

---
## 9. Testing

**Test strategy?** Pyramid: many fast unit tests (JUnit5 + Mockito, e.g. `OrderServiceTest`, `PaymentServiceTest`), fewer slice tests
(`@WebMvcTest`, `@DataJpaTest` with **Testcontainers** PostgreSQL), few end-to-end/contract tests (Pact / Spring Cloud Contract between services), smoke tests after deploy (Postman/Newman in the pipeline).
**What do your Mockito tests prove?** Business rules: total calculation + outbox written, idempotent duplicate skipped, failure path emits the right event, wrong password gives generic error.
**Do not mock what you do not own?** Prefer Testcontainers for DB/Kafka behaviour (SKIP LOCKED, unique constraint races cannot be proven with mocks).
**Swagger/OpenAPI?** springdoc generates the contract from code; export `/v3/api-docs` for client generation and contract checks.

---
## 10. DevOps / CI-CD / Kubernetes / AWS

**Describe your pipeline.** Commit -> Jenkins/GitLab: build + unit tests -> (Sonar quality gate) -> docker build -> Trivy scan -> push ECR (tag = commit SHA) -> deploy staging via kustomize -> smoke tests -> manual approval -> prod -> `rollout status`, auto `rollout undo` on failure.
**Why multi-stage Docker, non-root, MaxRAMPercentage?** Small attack surface/image, JRE only; non-root limits blast radius; JVM must respect container memory limits (otherwise OOMKilled).
**Rolling update vs blue-green vs canary?** Rolling (`maxUnavailable: 0`) here; blue-green = instant switch/rollback at 2x cost; canary = gradual traffic shift with metrics (Argo Rollouts / ALB weighted target groups).
**Deployment is bad - how to roll back?** `kubectl rollout undo` / redeploy previous SHA; DB migrations must be backward compatible (expand/contract) so rollback of code is safe.
**Pods crash-looping?** `kubectl describe pod` (events, OOMKilled, probe failures), `kubectl logs --previous`, config/secret missing, DB unreachable, probe too aggressive, memory limit too low.
**Autoscaling?** HPA on CPU (add custom/Kafka-lag metrics via KEDA); cluster autoscaler/Karpenter for nodes; PDB keeps >=1 pod during node drains.
**Config/secrets per environment?** Same image everywhere; ConfigMap + Secret (or Secrets Manager) per namespace; 12-factor.
**GitLab vs Jenkins?** GitLab CI: pipeline-as-code in repo, built-in registry/environments/approvals, runners. Jenkins: plugin-rich, self-hosted, Groovy pipelines. Same stages either way.
**Git workflow?** Trunk-based or GitFlow-lite: short-lived feature branches -> merge request (review, pipeline must pass) -> main (deployable) -> tags for releases; conventional commits.

---
## 11. "What would you improve?" (have this ready)
Refresh tokens + JWKS/Keycloak; outbox in payment & user; refund/compensation flow; distributed tracing (OTel);
Testcontainers + contract tests; Terraform for AWS; price catalog service; DLT replay tooling; ShedLock for non-idempotent jobs;
Kafka headers for correlation id; network policies + mTLS (service mesh); load tests (k6/Gatling) and SLOs with alerts.

## 12. Rapid-fire scale questions
* **10x traffic?** Stateless services -> more replicas (HPA); cache hot reads; partitions/consumers up; read replicas; RDS Proxy; async the heavy parts; load test first, find the real bottleneck.
* **One hot Kafka partition?** Bad key distribution -> better key / more partitions / composite key.
* **Duplicate e-mails?** dedupe key + unique constraint (done).
* **Deploy breaking API change?** Version (`/v2`), expand/contract, consumer-driven contracts, keep old version until clients migrate.
* **Token leaked?** Short expiry, rotate secret/keys (invalidates all), deny-list, audit logs, force re-login.
* **Whole region down?** Multi-AZ by default; multi-region needs replicated data (Aurora Global, MSK replicator), Route 53 failover - state RPO/RTO trade-offs.
