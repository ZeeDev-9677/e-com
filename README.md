# Shop – production-style Spring Boot microservices (interview learning project)

Four business services (**User, Order, Payment, Notification**) + **API Gateway** + **Eureka**, with the production
concerns interviewers ask about: JWT/RBAC, outbox, idempotency, saga, Kafka + RabbitMQ, Redis, Flyway,
indexes, stored procedures, circuit breaker, structured logging, CI/CD, Docker, Kubernetes, AWS.

> Read **docs/INTERVIEW_GUIDE.md** next: scenario questions mapped to the code in this repo.

## Architecture

```
                         Client / Postman / Swagger
                                   |  HTTPS
                          AWS ALB (TLS, WAF)
                                   |
                  +----------- API Gateway :8080 -----------+   JWT check, rate limit (Redis),
                  |   (Spring Cloud Gateway, Eureka lb://)   |   X-Correlation-Id
                  +--+----------+-----------+-----------+---+
                     |          |           |           |
              user-service  order-service payment-svc  notification-svc
                 :8081        :8082         :8083        :8084
              [userdb]      [orderdb]     [paymentdb]   [notificationdb]     (PostgreSQL, DB per service)
               Redis cache   Redis idem.
                     \           |            |            /
                      +--------- Kafka (events) ----------+
                      user.registered | order.created | payment.completed | payment.failed
                                                     |
                                notification-svc --> RabbitMQ (email queue + DLQ)

Order flow (choreographed saga):
 POST /api/orders -> order row PENDING + outbox row (ONE tx) -> outbox relay -> Kafka order.created
   -> payment-service charges (idempotent) -> payment.completed | payment.failed
   -> order-service sets CONFIRMED | CANCELLED ; notification-service e-mails the user
```

| Module | Port | Highlights |
|---|---|---|
| common-lib | – | events, JWT resource-server security, global exception handler, correlation-id + JSON logging, Kafka retry/DLT, OpenAPI |
| discovery-server | 8761 | Eureka |
| api-gateway | 8080 | routes, edge JWT validation, Redis rate limiter, correlation id |
| user-service | 8081 | register/login (issues JWT), RBAC, Redis cache, stored procedure, functional + partial index |
| order-service | 8082 | JPA, optimistic lock, transactional outbox, idempotency key, Feign + circuit breaker, DB function/procedure |
| payment-service | 8083 | idempotent Kafka consumer, UNIQUE(order_id), payment gateway strategy |
| notification-service | 8084 | Kafka -> RabbitMQ, retry + DLQ, dedupe |

## Run locally

Requirements: JDK 17, Maven 3.9+, Docker.

```bash
docker compose up -d --build        # everything (first build takes a few minutes)
# or only infra, and run services from your IDE (defaults point to localhost):
docker compose up -d postgres redis kafka rabbitmq
mvn -q -DskipTests install && mvn -pl user-service spring-boot:run   # etc.
```

* Gateway: http://localhost:8080  ·  Eureka: http://localhost:8761  ·  RabbitMQ UI: http://localhost:15672 (guest/guest)
* Swagger per service: http://localhost:8081/swagger-ui.html (8082, 8083, 8084). Click **Authorize**, paste the token.
* Import `postman/shop.postman_collection.json`, run requests 1-10 in order.
* Dev admin: `admin@shop.local` / `Admin@12345` (disable with `BOOTSTRAP_ADMIN=false`).
* Run tests: `mvn verify`

Quick try:
```bash
curl -s -XPOST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"ann@example.com","password":"Passw0rd!123","fullName":"Ann"}'
TOKEN=$(curl -s -XPOST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ann@example.com","password":"Passw0rd!123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -s -XPOST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: k1' -d '{"items":[{"sku":"BOOK-1","quantity":2,"unitPrice":499}]}'
```
Amount > 100000 makes the mock payment fail, so you can watch the compensation path (order -> CANCELLED).

## Deploy: AWS + Jenkins + Docker + Kubernetes

| Concern | AWS service |
|---|---|
| Images | ECR (repo per service: `shop/<service>`) |
| Runtime | EKS (managed node groups or Fargate), ALB Ingress Controller |
| PostgreSQL | RDS / Aurora PostgreSQL (Multi-AZ), one DB (ideally one instance) per service |
| Kafka | Amazon MSK |
| RabbitMQ | Amazon MQ for RabbitMQ |
| Redis | ElastiCache for Redis |
| Secrets | Secrets Manager + External Secrets Operator, IRSA for pod IAM |
| Logs / metrics | CloudWatch Container Insights or EFK; Prometheus + Grafana (annotations already on pods) |
| DNS / TLS | Route 53 + ACM |

Pipeline (see `Jenkinsfile`, equivalent `.gitlab-ci.yml`): checkout -> `mvn verify` -> docker build (parallel) ->
Trivy scan -> push ECR -> deploy staging (kustomize + kubectl) -> manual approval -> deploy prod -> rollout status
(auto `rollout undo` on failure). Images are tagged with the commit SHA (immutable, traceable, rollback-able).

Steps: (1) create ECR repos + EKS + RDS/MSK/MQ/ElastiCache (Terraform recommended) (2) edit `k8s/configmap.yaml`
endpoints, create `shop-secrets` from `k8s/secret.example.yaml` via your secret manager (3) run the pipeline
(4) `kubectl get pods -n prod`.
On Kubernetes **Eureka is not deployed**: Kubernetes Services + DNS already do discovery; the gateway uses
`*_SERVICE_URI=http://svc:port` and `EUREKA_CLIENT_ENABLED=false`.

## Honest limitations (good to say in an interview, they show maturity)
* Written to be read and run; **I could not compile or run it in my sandbox (no network)** – expect to fix small
  version/typo issues on first `mvn verify`.
* HS256 shared secret for simplicity; production should use an IdP (Keycloak / Cognito) with RS256 + JWKS, refresh tokens,
  revocation/short expiry.
* user-service and payment-service publish after-commit (not outbox); order-service shows the outbox pattern.
* No distributed tracing yet (add Micrometer Tracing + OpenTelemetry), no Testcontainers integration tests,
  no Terraform, client-supplied prices in orders (demo shortcut), no refund flow.
