# TN ARTISAN — Tamil Nadu

TN ARTISAN is a Spring Boot modular monolith for a Tamil Nadu craft marketplace. It brings together artisan verification, a searchable catalogue, buyer checkout, order tracking and a transparent artisan earnings ledger. The landing page follows the supplied marketplace references with a warmer Tamil Nadu palette and place-based craft storytelling.

The storefront is an independent concept marketplace for Tamil Nadu crafts. Product names, demo artisans, prices, availability and dimensions are illustrative sample data. It is not a government service and does not claim a partnership with any NGO or artisan group.

## Stack and architecture

- Java 17, Spring Boot 3.3, Spring MVC, Spring Security, Spring Data JPA / Hibernate
- JSP + JSTL, Bootstrap 5, CSS and JavaScript
- WAR packaging; Jakarta Servlet compatible with Tomcat 10.1
- MySQL 8 for Compose or persistent H2 for local development
- Flyway migrations, JWT bearer tokens for REST access, session login for JSP
- OpenAPI UI at /swagger-ui/index.html

The browser and REST API use the same MVC application and domain services, with JPA repositories backed by MySQL or H2. This is a modular monolith, not separate microservices.

## Implemented features

- Public catalogue, search, category filters, product details, maker profiles and craft passport pages.
- Customer and artisan account registration with BCrypt passwords and server-side role access checks.
- Artisan profile queue with admin approval/rejection. Only approved artisans may publish listings.
- Basket, stock validation at checkout, historical per-item prices, multi-artisan basket orders, and order status history.
- Production workflow from placement through acceptance, production, quality check, packing, shipment and delivery.
- Order item based earnings ledger with gross, 10% commission and artisan share recorded per item.
- REST catalogue, JWT login token endpoint, authenticated /api/me, Swagger/OpenAPI.
- Razorpay Standard Checkout creates provider orders server-side, verifies the callback signature, fetches the provider payment and fulfils only a captured payment. Cash on delivery remains a separate, unpaid state.
- Flyway migrations, MySQL Compose configuration and H2 local configuration.

## Tamil Nadu product content

The /products marketplace contains 50 independently curated concept listings across Thanjavur painting, metalwork, lamps, pottery, Kalamkari, palm-leaf weaving, wood carving and stone carving, plus sample maker listings. Every concept listing has an estimated price and dimensions, image attribution, quantity control and a working basket. Confirm specifications and price with an artisan before fulfilment.

The project owner supplied the artisan and product photographs used in the stories and selected product listings. Additional craft reference photographs come from Wikimedia Commons under the credited Creative Commons licences; each product page links to its photo source and licence. The TN ARTISAN mark is an original SVG.

## Customer flows

- Sign in with an existing account, or start registration when an email is unknown.
- Choose Razorpay online checkout or cash on delivery after entering a complete shipping address. Every order keeps an address snapshot, item and payment references, and event history.
- View products, payment details, address, real recorded status events, courier reference and a locality-level OpenStreetMap. The tracking page polls saved backend events every 20 seconds.
- Leave or update a rating after delivery. Flyway V2 stores reviews and records the payment method on the order.
## Run on Windows

Requirements: Java 17 and Maven 3.9 or newer.

Run locally with: mvn spring-boot:run

Open http://localhost:8080. H2 data persists under the local data folder.

For MySQL, run docker compose up --build. Docker was not installed in the development environment, so the Compose path is included but has not been executed here. Set non-default database credentials and JWT secret before sharing a deployment.

## Payment setup

The application does not include payment secrets. For a no-charge provider sandbox, create a Razorpay account, use its **Test Mode** API key pair, and set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` in the server environment. The Razorpay popup can offer UPI apps such as Google Pay when enabled for that merchant account. Configure `RAZORPAY_WEBHOOK_SECRET` and a public HTTPS webhook URL at `/webhooks/razorpay` for asynchronous payment updates. Test API keys do not transfer money; replace them with approved Live Mode credentials only when the merchant account, domain, and deployment are ready. Standard Checkout secrets remain server-side.

Razorpay's published pricing currently shows no setup or annual maintenance charge, but transaction/platform fees and GST apply to successful payments; Google Pay itself does not add a separate Google API fee. See [Razorpay pricing](https://razorpay.com/pricing/) and [Google Pay for India web setup](https://developers.google.com/pay/india/api/web/getting-started). Google Pay production web integrations also require merchant onboarding/approval.

To package a WAR for external Tomcat 10.1, run: mvn clean package

Deploy target/artisanconnect.war. In production set DB_URL, DB_USER, DB_PASSWORD and a random JWT_SECRET, disable the H2 console, use HTTPS, and configure secure cookies and operational secrets.

## Demo accounts

Development seed accounts (change these before any shared deployment):

| Role | Email | Password |
|---|---|---|
| Admin | admin@artisanconnect.local | Admin@123 |
| Approved artisan | meenakshi@artisanconnect.local | Artisan@123 |
| Approved artisan | arun@artisanconnect.local | Artisan@123 |

Create customer accounts from the registration page. New artisan accounts stay pending until the admin approves them.

## API

- GET /api/products
- GET /api/products/{id}
- POST /api/auth/token with JSON containing email and password
- GET /api/me with Authorization: Bearer accessToken
- Swagger UI: /swagger-ui/index.html

## Database model

users 1-to-1 artisan_profiles; users 1-to-many products; products many-to-1 categories; users 1-to-many orders; orders 1-to-many order_items; orders 1-to-many order_status_history; order_items 1-to-1 earnings_ledger.

Flyway creates the schema from src/main/resources/db/migration/V1__marketplace_core.sql and applies later versioned changes for reviews, dimensions, payment tracking and saved customer contact details. Financial columns use DECIMAL(12,2) and Java BigDecimal; purchased unit price and earning snapshots remain on order items when catalogue prices later change.

## Verification

Run mvn test and mvn package.

`mvn clean verify` passes. A local H2 smoke run validated the V1–V5 migrations, customer account creation/sign-in, COD checkout with a saved delivery address, order detail/tracking events, role access restrictions, artisan approval, and publication of an approved listing to the marketplace. The application is running locally at `http://localhost:8080`.

## Current scope and limitations

Tracking reflects database events entered by the relevant artisan or administrator. The map identifies the destination locality; automatic GPS/courier-network updates need a carrier API integration and are not fabricated. Refunds, return workflows, email/SMS notifications, password reset, coordinator assignments, actual Tamil/Hindi translations and voice listing, cross-device persistent carts, CSV/PDF exports, Redis/Kafka and service extraction remain follow-up phases. Profile approval is internal marketplace moderation, not government identity verification. Live payment acceptance still requires merchant onboarding, approved Live keys and payment methods; the local development environment has no merchant credentials, so real charges have not been run here.


### Public hosting

Use the Dockerfile to run the Spring Boot app. Set `SPRING_PROFILES_ACTIVE=hosted`, `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` (at least 16 characters), and `JWT_SECRET` (at least 32 random characters) in the hosting dashboard. Keep those values outside the repository. For Render, set `DATABASE_URL` to the Neon connection URI from the Neon dashboard or the ignored local `.env.local` file. The hosted profile accepts Neon's standard `postgresql://` URI and builds its JDBC connection securely at startup. It also accepts `DB_URL`, `DB_USER`, and `DB_PASSWORD` for other JDBC-compatible databases. Flyway chooses the matching H2 or PostgreSQL migrations automatically. The H2 fallback and seller image uploads use local files; Render Free's filesystem is temporary, so configure Neon before relying on hosted account or order data.

Local development accounts listed above are for a fresh local database.
