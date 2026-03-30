# QPrint — Student Client (Phase 1) · Master Build Prompt

---

## VISION & CONTEXT

You are building **QPrint** — a print shop automation platform solving a real, painful problem faced by millions of students near Indian universities. Students today waste 30–60 minutes standing in chaotic queues just to get documents printed, shouting instructions over noise, and dealing with lost payments. QPrint eliminates all of that. Students upload files, choose print preferences, pay online, track order status in real time, and collect their prints by entering a simple OTP — all from their phone or laptop.

This prompt covers **Phase 1: Student Client only**. The shop-owner side will be built in Phase 2. Build this like a product you are proud of — every screen, every API response, every animation should feel intentional and polished.

---

## TECH STACK

### Backend
- **Language & Framework**: Java 21, Spring Boot 3.x
- **Security**: Spring Security with JWT (access token + refresh token pattern)
- **Persistence**: Spring Data JPA → PostgreSQL
- **Cache**: Spring Data Redis (Lettuce client)
- **Messaging**: Apache Kafka (Spring Kafka)
- **File Storage**: MinIO (S3-compatible, self-hosted)
- **Payments**: Razorpay Java SDK + Razorpay JS SDK
- **Email**: JavaMailSender with Gmail SMTP (or any SMTP; configurable via env vars)
- **Build**: Maven (multi-module project)
- **Infrastructure**: Docker Compose for full local dev environment

### Frontend
- **Core**: React 18 (Vite as build tool)
- **Routing**: React Router v6
- **State**: Zustand (lightweight global state)
- **Styling**: Tailwind CSS v3 + custom CSS for bespoke animations & effects
- **Animations**: Framer Motion
- **HTTP**: Axios (with interceptors for JWT auto-refresh)
- **Forms**: React Hook Form + Zod validation
- **Icons**: Lucide React
- **Notifications/Toasts**: React Hot Toast
- **Fonts**: Load from Google Fonts — pick something distinctive (e.g. "Clash Display" or "Space Mono" for headings, "DM Sans" for body — or choose a unique pairing that fits an Indian tech startup aesthetic)

### Databases & Infrastructure
- **PostgreSQL** — persistent data (users, print objects, transactions, refresh tokens)
- **Redis** — cart cache, active order cache, OTP storage, rate limiting counters, session store
- **Kafka + Zookeeper** — async event bus between services
- **MinIO** — file object storage

---

## PROJECT STRUCTURE

Use a **Maven multi-module** layout:

```
QPrint/
├── docker-compose.yml
├── README.md
├── pom.xml (parent)
├── QPrint-gateway/          ← API Gateway service
├── QPrint-auth/             ← Auth + Email verification service
├── QPrint-objects/          ← File upload + print preferences
├── QPrint-cart/             ← Cart management
├── QPrint-checkout/         ← Razorpay integration
├── QPrint-orders/           ← Active order tracking
├── QPrint-otp/              ← OTP generation + verification
├── QPrint-transactions/     ← Completed order history
├── QPrint-shops/            ← Shop selector stub
└── QPrint-frontend/         ← React (Vite) frontend
```

Each Spring Boot module has its own `application.yml`. All secrets come from environment variables defined in `.env` and loaded by Docker Compose.

---

## BACKEND SERVICES — DETAILED SPECIFICATION

---

### 1. AUTH SERVICE (`/auth/**`)

This is the most critical service. Build it with absolute correctness. Every edge case must be handled gracefully.

#### Database Tables

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    account_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING | ACTIVE | SUSPENDED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Email verification codes
CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,    -- 15 minutes from generation
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,    -- 1 hour from generation
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### Endpoints

**`POST /auth/register`**
- Request body: `{ firstName, lastName, email, password }`
- Validation rules (enforce server-side even if frontend also validates):
  - `email`: valid email format, must not already exist
  - `password`: minimum 8 characters, must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (`@$!%*?&`)
  - `firstName`, `lastName`: non-empty, 2–50 characters, no numbers
- Actions:
  1. BCrypt hash the password (strength 12)
  2. Save user with `account_status = PENDING`, `email_verified = false`
  3. Generate a secure random 6-digit numeric OTP
  4. Save OTP to `email_verification_codes` with 15-minute expiry
  5. Send **two emails** via JavaMailSender:
     - **Email A — "Welcome to QPrint"**: A beautifully HTML-formatted welcome email explaining what QPrint is, with a CTA to verify email
     - **Email B — "Your Verification Code"**: Contains the 6-digit OTP clearly displayed, valid for 15 minutes. State that if user did not register, they can ignore this email.
  6. Publish Kafka event `user.registered` with `{userId, email}`
- Response: `201 Created` with `{ success: true, data: { userId, email }, message: "Registration successful. Please check your email for the verification code." }`
- Error cases: 409 if email already exists, 400 for validation failures

**`POST /auth/verify-email`**
- Request body: `{ userId, code }`
- Validation: Check OTP exists, not expired, not already used, matches
- On success:
  1. Mark `email_verified = true`, `account_status = ACTIVE`
  2. Mark OTP as `used = true`
  3. Send **confirmation email**: "Your QPrint account is now active! 🎉" (HTML formatted)
  4. Publish Kafka event `user.verified` with `{userId}`
- Response: `200 OK` with `{ success: true, message: "Email verified successfully. You can now log in." }`
- On invalid/expired OTP: return `400` with descriptive message
- If OTP expired: automatically invalidate and prompt user to request a new one

**`POST /auth/resend-verification`**
- Request body: `{ email }`
- Rate limit: max 3 resend attempts per hour per email (track in Redis)
- Invalidate all previous unused OTPs for this user
- Generate and send a fresh OTP
- Response: `200 OK` with `{ success: true, message: "New verification code sent." }`

**`POST /auth/login`**
- Request body: `{ email, password }`
- Validation:
  1. User must exist
  2. Password must match BCrypt hash
  3. `email_verified` must be `true` — if not, return `403` with `{ success: false, message: "Please verify your email first.", data: { needsVerification: true, userId } }`
  4. `account_status` must be `ACTIVE`
- On success:
  1. Issue **JWT access token** (15-minute expiry) containing `{ userId, email, firstName, lastName, role: "STUDENT" }`
  2. Issue **refresh token** (7-day expiry) — store hash in `refresh_tokens` table, send as `HttpOnly`, `Secure`, `SameSite=Strict` cookie
  3. Publish Kafka event `user.login` with `{ userId }` — Cart Service listens to this
- Response: `200 OK` with access token in response body (never in cookie — frontend stores in memory)

**`POST /auth/refresh`**
- Read refresh token from HttpOnly cookie
- Validate against DB hash, check expiry
- Issue new access token, rotate refresh token (invalidate old, issue new)
- Response: new access token in body

**`POST /auth/logout`**
- Invalidate refresh token from DB
- Clear HttpOnly cookie
- Response: `200 OK`

**`POST /auth/forgot-password`**
- Request body: `{ email }`
- If email exists: generate secure URL-safe random token (not OTP), hash it, save to `password_reset_tokens` with 1-hour expiry
- Send email with reset link: `https://qprint.app/reset-password?token=<raw_token>`
- Always respond `200 OK` regardless of whether email exists (security: never reveal if email is registered)

**`POST /auth/reset-password`**
- Request body: `{ token, newPassword, confirmPassword }`
- Validate: token exists, not expired, not used; `newPassword == confirmPassword`; password meets strength rules
- BCrypt hash new password, update user, mark token used
- Send confirmation email: "Your QPrint password has been reset."
- Response: `200 OK`

**`GET /auth/me`** *(protected)*
- Returns `{ userId, email, firstName, lastName, role, createdAt }`

**`PUT /auth/profile`** *(protected)*
- Request body: `{ firstName, lastName }` — email is NOT changeable
- Update user record
- Response: updated profile

**`PUT /auth/change-password`** *(protected)*
- Request body: `{ currentPassword, newPassword, confirmPassword }`
- Validate current password, enforce strength rules on new password
- BCrypt hash and save

#### JWT Configuration
- Secret: loaded from env var `JWT_SECRET` (minimum 256-bit)
- Access token: 15 minutes
- Refresh token: 7 days, stored in DB as SHA-256 hash
- Claims: `{ sub: userId, email, firstName, lastName, role: "STUDENT", iat, exp }`

#### Email Templates
All emails must be proper HTML emails (not plain text). Use inline CSS for email client compatibility. Include QPrint branding — logo text, colors, clean layout. Templates to build:
1. Welcome + Verification OTP email
2. Email verified / account active confirmation
3. Forgot password / reset link email
4. Password changed confirmation

---

### 2. API GATEWAY (`QPrint-gateway`)

- Built with **Spring Cloud Gateway**
- Routes:
  - `/auth/**` → Auth Service (public, no JWT check)
  - `/api/objects/**` → Object Creation Service
  - `/api/cart/**` → Cart Service
  - `/api/checkout/**` → Checkout Service
  - `/api/orders/**` → Active Order Service
  - `/api/transactions/**` → Transaction History Service
  - `/api/shops/**` → Shop Selector Service
  - `/internal/**` → BLOCKED (internal services only, never exposed externally)
- JWT validation filter: on all `/api/**` routes, extract and validate JWT, inject `X-User-Id` header downstream
- Rate limiting: Redis-backed counter — max **100 requests/minute per IP**. On breach: `429 Too Many Requests` with `{ success: false, message: "Too many requests. Please slow down." }`
- CORS: allow `http://localhost:5173` (Vite dev), configurable for production
- Global response filter: ensure all responses have `Content-Type: application/json`

---

### 3. OBJECT CREATION SERVICE (`/api/objects/**`)

**`POST /api/objects/upload`**
- Multipart form: `file` (PDF/DOCX/JPG/PNG, max 50MB) + `preferences` (JSON string)
- Preferences schema:
  ```json
  {
    "copies": 1,
    "colorMode": "BW | COLOR",
    "sides": "SINGLE | DOUBLE",
    "pageRange": "ALL | 1-5,8,10-12",
    "paperSize": "A4 | A3 | LEGAL",
    "binding": "NONE | STAPLE | SPIRAL",
  }
  ```
- Upload file to MinIO bucket `print-files` at path `{userId}/{UUID}-{originalFilename}`
- For PDF files: extract page count using Apache PDFBox
- For images: page count = 1
- For DOCX: use Apache POI to count pages (approximation acceptable)

**Pricing Logic:**
```
base_price_per_page:
  BW  → ₹1.50/page
  COLOR → ₹8.00/page

effective_pages = page_count × copies

if sides == DOUBLE:
  effective_pages = ceil(page_count / 2) × copies (physical sheets)
  price_multiplier = 0.9   ← 10% discount for duplex

binding_cost:
  NONE   → ₹0
  STAPLE → ₹0
  SPIRAL → ₹15

calculated_price = (effective_pages × base_price_per_page × price_multiplier) + binding_cost
```

- Save to `print_objects` table:
  ```sql
  CREATE TABLE print_objects (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL,
      original_filename VARCHAR(500),
      file_ref VARCHAR(1000) NOT NULL,      -- MinIO path
      copies INT NOT NULL DEFAULT 1,
      color_mode VARCHAR(10) NOT NULL,
      sides VARCHAR(10) NOT NULL,
      page_range VARCHAR(100),
      paper_size VARCHAR(10) NOT NULL,
      binding VARCHAR(10) NOT NULL,
      page_count INT NOT NULL,
      calculated_price DECIMAL(10,2) NOT NULL,
      created_at TIMESTAMP DEFAULT NOW()
  );
  ```
- Response: `{ objectId, originalFilename, pageCount, calculatedPrice, preferences }`

**`GET /api/objects/{objectId}`**
- Return print object details for authenticated user

**`DELETE /api/objects/{objectId}`**
- Delete from DB and MinIO (only if not in an active order)

---

### 4. CART SERVICE (`/api/cart/**`)

**Kafka Consumer:** Listen to `user.login` event → fetch any existing cart items from `print_objects` table (items added but not yet checked out) → warm up Redis hash `cart:{userId}`

**`GET /api/cart`**
- Fetch `cart:{userId}` hash from Redis
- Enrich each item with current print object details from PostgreSQL
- Return: `{ items: [...], totalItems, totalPrice }`

**`POST /api/cart/add`**
- Body: `{ objectId }`
- Verify print object belongs to requesting user
- Add to Redis hash `cart:{userId}` → field = objectId, value = `{ objectId, price, addedAt }`
- Set TTL on the hash key = 24 hours (reset on each add)
- Response: updated cart summary

**`DELETE /api/cart/item/{objectId}`**
- Remove specific item from Redis hash
- Response: updated cart summary

**`DELETE /api/cart`**
- Clear entire `cart:{userId}` hash
- Response: `{ success: true }`

**`GET /api/cart/count`**
- Return just `{ count: N }` — used by home dashboard badge

---

### 5. CHECKOUT SERVICE (`/api/checkout/**`)

**`POST /api/checkout/initiate`**
- The api call `/api/checkout/initiate` when user will hit the checkout button on cart-frontend
- Read cart from Redis for authenticated user
- Validate cart is not empty
- Calculate final total (re-validate prices from DB, never trust client)
- Create Razorpay order via Razorpay Java SDK: `{ amount: totalInPaise, currency: "INR", receipt: orderId }`
- Save pending checkout to a Redis key `checkout:{userId}` with cart snapshot and Razorpay order ID (TTL = 30 minutes)
- Response: `{ razorpayOrderId, amount, currency: "INR", keyId: RAZORPAY_KEY_ID, cartSnapshot }`

**`POST /api/checkout/webhook`** *(Razorpay webhook, not JWT protected)*
- Verify HMAC-SHA256 signature using `X-Razorpay-Signature` header and webhook secret
- On `payment.captured` event:
  1. Generate internal `orderId` (UUID)
  2. Retrieve cart snapshot from `checkout:{userId}` Redis key
  3. Publish Kafka event `order.confirmed`:
     ```json
     {
       "orderId": "...",
       "userId": "...",
       "shopId": "SHOP_001",
       "items": [...cartSnapshot],
       "totalAmount": 145.50,
       "razorpayPaymentId": "...",
       "paidAt": "2025-..."
     }
     ```
  4. Clear Redis keys: `cart:{userId}` and `checkout:{userId}`
- On `payment.failed`: publish `payment.failed` Kafka event, respond `200 OK` to Razorpay

**`GET /api/checkout/status/{razorpayOrderId}`**
- Return current payment status by querying Razorpay API

**Note**: There is no vendor is added on the other side.So,as the developnment of the whole phase on shop/vendor side-beackend is 
pending also the shop selector option backend is whole pending and the demo-shop information shown on the homepage-frontend button of shop selector feature, 
bottom-up menu is just the demo representation.

---

### 6. ACTIVE ORDER SERVICE (`/api/orders/**`)

**Kafka Consumer:** Listen to `order.confirmed`
- Create entry in Redis: `order:{orderId}` →
  ```json
  {
    "orderId": "...",
    "userId": "...",
    "shopId": "SHOP_001",
    "items": [...],
    "status": "RECEIVED",
    "estimatedReadyMinutes": 20,
    "readyByTime": "<ISO timestamp>",
    "otp": null,
    "createdAt": "...",
    "updatedAt": "..."
  }
  ```
- Also add `orderId` to a Redis set `user-orders:{userId}` for fast lookup
- TTL on order key: 48 hours (auto-cleanup)

**Status state machine:** `RECEIVED → IN_PRODUCTION → READY → COMPLETED`
- Note: Status transitions from shop side (Phase 2) will come via Kafka event `order.status.updated`
- Build a consumer for `order.status.updated`: `{ orderId, newStatus }` → update Redis accordingly
- When `newStatus == READY`: call OTP Service internally to generate OTP, patch `otp` field in Redis

**`GET /api/orders/active`**
- Look up `user-orders:{userId}` set in Redis
- Fetch each order entry
- Return list of active orders with full details

**`GET /api/orders/active/{orderId}`**
- Return single order; validate it belongs to requesting user

When `status == COMPLETED`:
- Publish Kafka `order.completed` with full order snapshot
- Remove from `order:{orderId}` Redis key and from `user-orders:{userId}` set

---

### 7. OTP SERVICE (internal only — never exposed through API Gateway)

**`POST /internal/otp/generate`**
- Body: `{ orderId, userId }`
- Generate secure 6-digit numeric OTP using `SecureRandom`
- Store in Redis: `otp:{orderId}` → `{ otp, userId, attempts: 0 }`, TTL = 10 minutes
- Patch `otp` field into `order:{orderId}` Redis entry
- Send SMS to student's registered phone (optional — if no phone on file, skip SMS but OTP is still shown in the app)
- Response: `{ success: true }`

**`POST /internal/otp/verify`**
- Body: `{ orderId, otp }`
- Fetch `otp:{orderId}` from Redis
- If `attempts >= 3`: invalidate, regenerate, return `{ valid: false, message: "Too many wrong attempts. A new OTP has been sent." }`
- If match: return `{ valid: true }`
- If no match: increment attempts, return `{ valid: false, attemptsLeft: N }`

---

### 8. TRANSACTION HISTORY SERVICE (`/api/transactions/**`)

**Kafka Consumer:** Listen to `order.completed`
- Persist to PostgreSQL:
  ```sql
  CREATE TABLE transactions (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL,
      order_id UUID NOT NULL UNIQUE,
      shop_id VARCHAR(50),
      items_json JSONB NOT NULL,
      total_amount DECIMAL(10,2) NOT NULL,
      razorpay_payment_id VARCHAR(100),
      completed_at TIMESTAMP NOT NULL,
      created_at TIMESTAMP DEFAULT NOW()
  );
  CREATE INDEX idx_transactions_user_id ON transactions(user_id);
  CREATE INDEX idx_transactions_completed_at ON transactions(completed_at DESC);
  ```

**`GET /api/transactions`**
- Query param: `page` (default 0), `size` (default 20)
- Return paginated list for authenticated userId, sorted by `completed_at DESC`
- Response: `{ content: [...], totalPages, totalElements, currentPage }`

**`GET /api/transactions/{id}`**
- Return full transaction detail including `items_json`

---

### 9. SHOP SELECTOR STUB (`/api/shops/**`)

**`GET /api/shops/nearby`**
- Query params: `lat`, `lng` (optional, not used yet)
- Return hardcoded response:
  ```json
  {
    "success": true,
    "data": [{
      "id": "SHOP_001",
      "name": "Campus Print Zone",
      "address": "Near Gate 2, University Road",
      "avgWaitMinutes": 20,
      "isOpen": true,
      "distance": "50m",
      "rating": 4.5,
      "totalOrders": 1200
    }]
  }
  ```
- This will be replaced with real geolocation + multiple shops in Phase 4
- The feature will developed further more in phase 4, for now there will only 
  single shop option available on the website, whose details would be added and 
  available to fetch once completion of phase 3, So for now just make button on
  frontend homepage to access the feature with random shop name & details.  

---

## CROSS-CUTTING BACKEND CONCERNS

**Standard API Response Envelope — every single endpoint must use this:**
```json
{
  "success": true | false,
  "data": { ... } | null,
  "message": "Human-readable description",
  "timestamp": "ISO-8601"
}
```

**Global Exception Handler (`@ControllerAdvice`):**
- `MethodArgumentNotValidException` → `400 Bad Request` with field-level errors
- `AuthenticationException` → `401 Unauthorized`
- `AccessDeniedException` → `403 Forbidden`
- `EntityNotFoundException` → `404 Not Found`
- `DataIntegrityViolationException` → `409 Conflict`
- All others → `500 Internal Server Error` (log full stack, return sanitized message)

**Logging:** Use SLF4J + Logback. Log all incoming requests (method, path, userId from JWT), all Kafka events published/consumed, all errors.

**Kafka Topics:**
| Topic | Publisher | Consumers |
|---|---|---|
| `user.registered` | Auth | — |
| `user.verified` | Auth | — |
| `user.login` | Auth | Cart |
| `order.confirmed` | Checkout | Active Order |
| `order.status.updated` | Shop (Phase 2) | Active Order |
| `order.completed` | Active Order | Transaction History |
| `payment.failed` | Checkout | — |

All Kafka messages serialized as JSON. Use `@KafkaListener` with `@Payload` and `@Header`.

---

## DOCKER COMPOSE

`docker-compose.yml` must include:
- `postgres` — PostgreSQL 16, with init SQL scripts to create all tables
- `redis` — Redis 7 Alpine
- `zookeeper` — Confluent Zookeeper
- `kafka` — Confluent Kafka, depends on zookeeper
- `minio` — MinIO latest, with auto-create bucket on startup
- `QPrint-gateway` — built from `QPrint-gateway/Dockerfile`
- `QPrint-auth` — built from `QPrint-auth/Dockerfile`
- `QPrint-objects`
- `QPrint-cart`
- `QPrint-checkout`
- `QPrint-orders`
- `QPrint-otp`
- `QPrint-transactions`
- `QPrint-shops`

All services read from a shared `.env` file containing:
```
POSTGRES_URL=jdbc:postgresql://postgres:5432/qprint
POSTGRES_USER=qprint
POSTGRES_PASSWORD=...
REDIS_URL=redis://redis:6379
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

Each Spring Boot service exposes on a unique port and declares health check endpoint `/actuator/health`.

---

## FRONTEND — REACT (VITE) APPLICATION

### Design Philosophy
Build a **premium, modern Indian tech startup aesthetic**. Think of products like Zepto, CRED, or Razorpay — confident dark backgrounds, bold accent colors, smooth animations, and a UI that feels native and trustworthy. Avoid generic Bootstrap or Material-UI looks. Every screen should feel like it was designed by a senior product designer.

**Color Palette:**
- Background: Deep navy / charcoal (`#0A0F1E` or similar dark)
- Surface cards: Slightly lighter dark (`#111827`)
- Primary accent: Electric blue or vibrant indigo (`#6366F1` or `#3B82F6`)
- Secondary accent: Warm amber/orange (`#F59E0B`) — used for CTAs and highlights
- Success: Emerald green (`#10B981`)
- Error: Warm red (`#EF4444`)
- Text primary: Pure white
- Text secondary: Muted gray (`#9CA3AF`)

**Typography:**
- Display headings: "Syne" (Google Fonts) — geometric, modern, Indian tech feel
- Body: "Plus Jakarta Sans" — clean, highly legible
- Monospace (OTP display, order IDs): "JetBrains Mono"

**Animations (Framer Motion):**
- Page transitions: fade + slide up (300ms ease-out)
- Card hover: subtle lift with shadow expansion
- Form field focus: glowing border animation
- Loading states: custom skeleton shimmer matching dark theme
- Success states: scale + fade pop
- Toast notifications: slide in from top-right

**Routing (React Router v6):**
```
/                     → redirect to /login
/login                → LoginPage
/register             → RegisterPage
/verify-email         → EmailVerificationPage
/forgot-password      → ForgotPasswordPage
/reset-password       → ResetPasswordPage
/home                 → HomePage (protected)
/upload               → UploadPage (protected)
/cart                 → CartPage (protected)
/checkout             → CheckoutPage (protected)
/checkout/success     → CheckoutSuccessPage (protected)
/orders               → OrderStatusPage (protected)
/orders/:orderId      → OrderDetailPage (protected)
/transactions         → TransactionsPage (protected)
/transactions/:id     → TransactionDetailPage (protected)
/settings             → SettingsPage (protected)
```

**Auth Flow in Frontend:**
- Access token stored in **Zustand store in memory only** (never localStorage, never sessionStorage)
- Axios interceptor: on every request, attach `Authorization: Bearer <accessToken>` header
- Axios response interceptor: on `401`, automatically call `POST /auth/refresh` (refresh token sent via HttpOnly cookie automatically by browser), get new access token, retry original request
- If refresh also fails: clear auth state, redirect to `/login`
- `ProtectedRoute` component wraps all authenticated pages

---

### PAGE SPECIFICATIONS

#### `/login` — LoginPage
- Full-screen dark background with animated gradient mesh or subtle floating shapes in the background (CSS or Framer Motion)
- QPrint logo + tagline: *"Print smarter. Not harder."*
- Card with glassmorphism styling: frosted glass effect, subtle border glow
- Fields: Email, Password (with toggle show/hide icon)
- "Forgot Password?" link → `/forgot-password`
- "Don't have an account? Register" link → `/register`
- On submit: POST `/auth/login`
  - Success → store token in Zustand → redirect to `/home`
  - If `needsVerification: true` → redirect to `/verify-email?userId=...`
  - Error → shake animation on card + toast error message
- Show loading spinner inside button while request in flight
- Form validated with React Hook Form + Zod

#### `/register` — RegisterPage
- Same visual style as login — consistent branding
- Fields:
  - First Name
  - Last Name
  - Email
  - Password (with strength meter indicator bar showing Weak/Medium/Strong in real time as user types)
  - Confirm Password
- **Live validation on blur** for each field
- **On submit validation** (React Hook Form + Zod):
  - First/last name: 2–50 chars, letters only
  - Email: valid format
  - Password: min 8 chars, must contain uppercase, lowercase, digit, special char — show specific requirement checklist dynamically (✓ each requirement as it's met, red ✗ if not)
  - Confirm password: must match password
- On submit: POST `/auth/register`
  - Success → toast "Check your email for verification code!" → redirect to `/verify-email?userId=...&email=...`
  - Error (email exists) → inline error on email field

#### `/verify-email` — EmailVerificationPage
- Show user's email address (from query params)
- Instruction text: "We've sent a 6-digit code to your email. Enter it below."
- **6-box OTP input**: 6 individual single-character input boxes that auto-focus next box on entry, support backspace to go back, support paste of full 6-digit code
- Countdown timer showing remaining validity (15 minutes, counting down)
- "Resend Code" button (disabled until timer shows < 1 minute or user clicks and rate-limit allows) — calls `POST /auth/resend-verification`
- On submit: POST `/auth/verify-email`
  - Success → "Email verified! 🎉" success animation → redirect to `/login` with toast "Account active. Please log in."
  - Error → shake OTP boxes + error message
- If user navigates here directly without userId param → redirect to `/register`

#### `/forgot-password` — ForgotPasswordPage
- Simple card: email input
- On submit: POST `/auth/forgot-password`
- Always show success message regardless: "If that email is registered, a reset link has been sent."
- Link back to login

#### `/reset-password` — ResetPasswordPage
- Read `token` from query params
- Fields: New Password + Confirm Password
- Same password strength requirements and live checklist as register page
- On submit: POST `/auth/reset-password`
- Success → redirect to `/login` with toast "Password reset! Please log in with your new password."
- If token invalid/expired → show error with link to request new reset

#### `/home` — HomePage (Dashboard)
- **Top navigation bar:**
  - Left: QPrint logo
  - Right: Profile avatar/icon (initials of user) — clicking opens a dropdown menu:
    - "My Profile" → `/settings`
    - "Order History" → `/transactions`
    - "Logout" (with confirmation) → POST `/auth/logout` → clear store → redirect to `/login`
- **Hero section:** Personalized greeting — "Good morning, {firstName}! 👋" with current date
- **4 main action cards** (large, clickable, visually distinct with icons and animations on hover):
  1. **Upload & Print** — icon: printer/upload — links to `/upload`
  2. **My Cart** — icon: shopping cart — links to `/cart` — shows **live item count badge** (fetched from `GET /api/cart/count` on page load)
  3. **Order Status** — icon: clock/tracking — links to `/orders` — shows count of active orders
  4. **Transaction History** — icon: receipt/history — links to `/transactions`
- **Select Shop** stub widget — a card showing the currently "selected" shop (`Campus Print Zone`) with status indicator (Open/Closed), average wait time, and a "Change Shop" button (disabled with tooltip "Coming soon" in Phase 2)
- **Recent activity strip** at bottom: last 3 transactions or active orders as mini cards

#### `/upload` — UploadPage
- **Step 1 — File Upload:**
  - Large drag-and-drop zone with animated dashed border and upload icon
  - Supports: PDF, DOCX, JPG, PNG — show accepted formats
  - Max 50MB — validate client-side before upload
  - On file select: show file name, size, type icon
  - Progress bar during upload to MinIO (use axios `onUploadProgress`)

- **Step 2 — Print Preferences** (shown after file selected):
  - Copies: number input (min 1, max 100)
  - Color Mode: toggle button group — "B&W (₹1.5/pg)" vs "Color (₹8/pg)"
  - Sides: toggle — "Single Sided" vs "Double Sided (10% off)"
  - Page Range: radio — "All Pages" vs "Custom Range" (text input shown if custom, with format hint e.g. `1-5, 8, 10-12`)
  - Paper Size: dropdown — A4 / A3 / Legal
  - Binding: segmented selector — "None" / "Staple (+₹15)" / "Spiral (+₹40)"
  - Special Instructions: expandable textarea (optional)

- **Live Price Calculator:**
  - As user changes preferences, recalculate price in real time on the client using the same pricing formula
  - Show price prominently: "Estimated Cost: ₹XX.XX"
  - Breakdown below: pages × rate + binding cost
  - Note: "Final price confirmed after upload"

- **"Upload & Add to Cart" button:**
  - POST to `/api/objects/upload`
  - On success: show confirmed price from server, "Added to Cart! ✓" animation, offer "Continue Uploading" or "Go to Cart"

#### `/cart` — CartPage
- Show list of cart items:
  - File name + type icon
  - Print preferences summary (color, sides, pages, binding)
  - Price per item
  - Delete (trash icon) button per item
- If cart empty: illustrated empty state — "Your cart is empty. Upload documents to get started." with CTA to `/upload`
- Footer: **Order Summary** panel
  - Subtotal per item
  - Total
  - "Proceed to Checkout →" button
- Real-time cart: on any delete, re-fetch cart immediately

#### `/checkout` — CheckoutPage
- **Order Summary** — full list of items with prices
- **Total Billing Breakdown:**
  - Subtotal
  - Convenience fee (₹0 for now, placeholder)
  - **Total Amount** (bold, large)
- **Shop Info** — "Your order will be ready at: Campus Print Zone, Near Gate 2"
- **"Pay Now" button:** Triggers `POST /api/checkout/initiate`, then opens Razorpay JS checkout modal
- Razorpay checkout prefilled with user's name and email
- On `payment.success` callback from Razorpay JS SDK → redirect to `/checkout/success?orderId=...`
- On `payment.failed` → show error toast, allow retry

#### `/checkout/success` — CheckoutSuccessPage
- Full-screen success animation (Framer Motion: confetti or checkmark animation)
- "Order Placed! 🎉"
- Order summary card
- "Track Your Order →" button → `/orders`

#### `/orders` — OrderStatusPage
- **Poll `GET /api/orders/active` every 10 seconds** using `setInterval`
- If no active orders: empty state — "No active orders. Place an order to get started."
- For each active order, show:
  - Order ID (shortened, e.g. `#PRE-XXXX`)
  - Items list (file names)
  - Total amount paid
  - **Status Progress Bar** with 4 steps:
    - `RECEIVED` → `IN_PRODUCTION` → `READY` → `COMPLETED`
    - Current step highlighted with animated pulse indicator
    - Estimated ready time shown below
  - When status = `READY`:
    - OTP displayed in large, bold monospace font inside a highlighted card: "Show this to the shopkeeper:"
    - **`[  1  ] [  2  ] [  3  ] [  4  ] [  5  ] [  6  ]`** styled OTP boxes
    - Instructional text: "Your prints are ready! Show this OTP to collect your order."

#### `/transactions` — TransactionsPage
- Paginated list of completed orders
- Each row/card shows: Order ID, date, total amount, number of files, status badge "Completed"
- Click to expand → full detail view (or navigate to `/transactions/:id`)
- Pagination controls at bottom (previous/next + page numbers)

#### `/transactions/:id` — TransactionDetailPage
- Full order details:
  - Order metadata (ID, date, shop, payment ID)
  - Each print object: file name, preferences, pages, price
  - Total amount paid

#### `/settings` — SettingsPage
- **Profile section:**
  - Avatar (initials, with future option to upload photo)
  - First Name, Last Name (editable)
  - Email (read-only, with lock icon and tooltip "Email cannot be changed")
  - "Save Changes" button → PUT `/auth/profile`
- **Security section:**
  - "Change Password" form: Current Password, New Password (with strength meter), Confirm Password
  - Submit → PUT `/auth/change-password`
- **Account Info:**
  - Member since date
  - Account status badge

---

## ZUSTAND STATE MANAGEMENT

Define these stores:

```javascript
// authStore.js
{
  accessToken: null,
  user: null,          // { userId, email, firstName, lastName, role }
  isAuthenticated: false,
  setAuth: (token, user) => {},
  clearAuth: () => {}
}

// cartStore.js
{
  itemCount: 0,
  setItemCount: (n) => {}
}

// shopStore.js
{
  selectedShop: null,
  setShop: (shop) => {}
}
```

---

## AXIOS CONFIGURATION

```javascript
// api/axios.js
const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL, withCredentials: true });

// Request interceptor: attach access token from Zustand store
api.interceptors.request.use(config => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: handle 401 → auto-refresh
api.interceptors.response.use(
  res => res,
  async err => {
    if (err.response?.status === 401 && !err.config._retry) {
      err.config._retry = true;
      const { data } = await axios.post('/auth/refresh', {}, { withCredentials: true });
      useAuthStore.getState().setAuth(data.data.accessToken, useAuthStore.getState().user);
      err.config.headers.Authorization = `Bearer ${data.data.accessToken}`;
      return api(err.config);
    }
    return Promise.reject(err);
  }
);
```

---

## BUILD ORDER

Implement in this exact order. Show **complete code for every file** before moving to the next step. No placeholders, no `// TODO` comments — everything must be production-ready and runnable.

1. **Docker Compose + all infrastructure** (PostgreSQL init SQL, Redis, Kafka, MinIO, all service stubs)
2. **Auth Service** (all endpoints + email templates + Kafka publishing)
3. **API Gateway** (routing + JWT filter + rate limiting)
4. **React Frontend scaffolding** (Vite setup, Tailwind, Router, Zustand stores, Axios config, ProtectedRoute)
5. **Login, Register, Email Verification, Forgot/Reset Password pages** (fully styled and functional)
6. **Object Creation Service** + Upload Page
7. **Cart Service** + Cart Page
8. **Checkout Service + Razorpay** + Checkout Page + Success Page
9. **Active Order Service** + Kafka consumer + Order Status Page
10. **OTP Service** (internal)
11. **Transaction History Service** + Transactions Page
12. **Shop Selector Stub** + integrate into Home Dashboard
13. **Settings Page** (profile + change password)
14. **Home Dashboard** (fully functional with cart count, active orders count, recent activity)
15. **Final polish**: ensure all pages are visually consistent, all error states handled, all loading states shown, all empty states have illustrations, README.md complete

---

## README REQUIREMENTS

The `README.md` must include:
- Project overview and architecture diagram (ASCII or description)
- Prerequisites (Java 21, Docker, Node 18+)
- Setup instructions (clone → copy `.env.example` → `docker compose up` → frontend `npm run dev`)
- All environment variables explained
- API documentation (summary of all endpoints)
- How the Kafka event flow works
- How to run individual services without Docker
- Known limitations and Phase 2 roadmap

---

*End of QPrint Phase 1 — Student Client Build Prompt*
