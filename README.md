# QPrint — Phase 1 (Student Client)

QPrint is a student-focused print shop automation platform. This repository contains the Phase 1 student client stack: Spring Boot microservices, Kafka/Redis/Postgres/MinIO infrastructure, and a Vite/React frontend.

## Architecture (high level)
```
[Frontend] -> [Gateway] -> /auth | /api/* routes
                        \-> Redis rate limiting

Kafka <-> Auth <-> Cart <-> Orders <-> Transactions <-> Checkout
Redis: cart, checkout, orders, otp
Postgres: auth/users, objects, transactions
MinIO: file storage
```

## Prerequisites
- Java 21
- Maven 3.9+
- Docker + Docker Compose
- Node 18+

## Quick start (Docker)
1. Copy `.env.example` to `.env` and fill secrets.
2. From repo root:
     - `docker compose up --build`
3. Services listen on 8080-8088; Postgres on 5432; Redis on 6379; Kafka on 9092; MinIO on 9000/9001.

## Frontend dev
```
cd QPrint-frontend
npm install
npm run dev
```

## Module ports (default)
- Gateway: 8080
- Auth: 8081
- Objects: 8082
- Cart: 8083
- Checkout: 8084
- Orders: 8085
- OTP: 8086
- Transactions: 8087
- Shops: 8088

## Environment variables
```
POSTGRES_URL=jdbc:postgresql://postgres:5432/qprint
POSTGRES_USER=qprint
POSTGRES_PASSWORD=...
REDIS_HOST=redis
REDIS_PORT=6379
KAFKA_BOOTSTRAP=kafka:9092
MINIO_URL=http://minio:9000
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
JWT_SECRET=...
RAZORPAY_KEY_ID=...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=...
SMTP_PASSWORD=...
SMTP_FROM=noreply@qprint.app
```

## API summary

### Auth (`/auth/**`)
- POST /auth/register
- POST /auth/verify-email
- POST /auth/resend-verification
- POST /auth/login
- POST /auth/refresh
- POST /auth/logout
- POST /auth/forgot-password
- POST /auth/reset-password
- GET /auth/me
- PUT /auth/profile
- PUT /auth/change-password

### Objects (`/api/objects/**`)
- POST /api/objects/upload
- GET /api/objects/{objectId}
- DELETE /api/objects/{objectId}

### Cart (`/api/cart/**`)
- GET /api/cart
- POST /api/cart/add
- DELETE /api/cart/item/{objectId}
- DELETE /api/cart
- GET /api/cart/count

### Checkout (`/api/checkout/**`)
- POST /api/checkout/initiate
- POST /api/checkout/webhook
- GET /api/checkout/status/{razorpayOrderId}

### Orders (`/api/orders/**`)
- POST /api/orders
- GET /api/orders
- GET /api/orders/active
- GET /api/orders/{orderId}
- GET /api/orders/active/{orderId}
- PATCH /api/orders/{orderId}/status

### Transactions (`/api/transactions/**`)
- GET /api/transactions
- GET /api/transactions/{id}

### Shops (`/api/shops/**`)
- GET /api/shops/nearby

## Kafka event flow
| Topic | Publisher | Consumer |
| --- | --- | --- |
| user.registered | Auth | - |
| user.verified | Auth | - |
| user.login | Auth | Cart |
| order.confirmed | Checkout | Orders |
| order.status.updated | Shops (Phase 2) | Orders |
| order.completed | Orders | Transactions |
| payment.failed | Checkout | - |

## Run services without Docker
- Each module is a Spring Boot app. Example:
    - `cd QPrint-auth`
    - `mvn spring-boot:run`
    - Repeat for any module

## Known limitations (Phase 1)
- Shops service is a stub returning a single hardcoded shop
- OTP service is internal-only and used for demo flows
- Cart warmup on login uses the cart_items table

## Phase 2 roadmap
- Shop operator portal
- Real-time order status updates from shops
- Multi-shop discovery and selection
- Live inventory and capacity tracking

## GitHub push readiness
This repo is prepared for GitHub with:
- CI workflow: `.github/workflows/ci.yml`
- Docker image publish workflow: `.github/workflows/publish-images.yml`
- Server deploy workflow: `.github/workflows/deploy.yml`
- Production compose stack: `deploy/docker-compose.prod.yml`
- Production env template: `deploy/.env.prod.example`
- Frontend container build: `QPrint-frontend/Dockerfile`

Initialize git locally (if needed):
```
git init -b main
git add .
git commit -m "Prepare QPrint for GitHub CI/CD"
git remote add origin https://github.com/<owner>/<repo>.git
git push -u origin main
```

## GitHub deployment setup

### 1) Configure repository variables
- `VITE_API_BASE_URL`: Public backend gateway URL used while building frontend container image.

### 2) Configure repository secrets
- `DEPLOY_HOST`: SSH host (VM/public server)
- `DEPLOY_PORT`: SSH port (set to `22` if default SSH port is used)
- `DEPLOY_USER`: SSH username
- `DEPLOY_SSH_KEY`: private SSH key for deploy user
- `GHCR_USERNAME`: GitHub username/org with package pull access
- `GHCR_TOKEN`: GitHub PAT with `read:packages`
- `DEPLOY_ENV_FILE`: full contents of your production `.env` (use `deploy/.env.prod.example` as base)

### 3) Publish images
- Push to `main` (or run `Publish Docker Images` workflow manually).
- Images are pushed to `ghcr.io/<owner>/qprint-*`.

### 4) Deploy from GitHub
- Run `Deploy to Server` workflow from Actions.
- It copies `deploy/docker-compose.prod.yml` + `init.sql` to `~/qprint` on your server, writes `.env`, pulls GHCR images, and starts the stack.
