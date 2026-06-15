# EngHub Spring Boot

EngHub is a Spring Boot backend for an English learning platform focused on TOEIC practice, bilingual reading, listening dictation, vocabulary review, and AI-assisted content preparation. The application exposes REST APIs secured by JWT, stores data in PostgreSQL, manages schema evolution with Flyway, uploads media to Cloudinary, and integrates with Gemini for transcript, translation, explanation, vocabulary, and practice-chat workflows.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Runtime Configuration](#runtime-configuration)
- [Local Development](#local-development)
- [Database Migrations](#database-migrations)
- [Authentication and Authorization](#authentication-and-authorization)
- [API Overview](#api-overview)
- [AI Integration](#ai-integration)
- [Media Storage](#media-storage)
- [Response and Error Format](#response-and-error-format)
- [Testing](#testing)
- [Frontend Integration Guides](#frontend-integration-guides)
- [Security Notes](#security-notes)

## Features

- JWT authentication with logout token invalidation.
- Role and permission based authorization for admin and student workflows.
- TOEIC test collection management with seven default parts.
- Excel import for TOEIC questions, answers, media mapping, transcripts, and review workflow.
- Published test catalog for learners.
- Mock and practice attempts with selected parts, answer saving, auto-submit on mock timeout, and TOEIC-style scoring.
- Practice-mode AI question chat with Server-Sent Events streaming.
- Cloudinary-backed image and audio asset management.
- Listening dictation sessions built from transcript lines or transcript fallback splitting.
- Part 7 bilingual reading lessons with admin publishing workflow.
- Vocabulary topics, dictionary lookup, AI translation, learning progress, and spaced review.
- Gemini AI support for transcripts, question translations, explanations, reading translation, reading vocabulary, and practice chat.
- OpenAPI/Swagger UI and Spring Boot Actuator.

## Tech Stack

- Java 21
- Spring Boot 3.4.0
- Spring Web MVC
- Spring Security + OAuth2 Resource Server
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JWT with Nimbus JOSE JWT
- Lombok
- MapStruct
- Cloudinary Java SDK
- Apache POI for Excel import
- Gemini API via Spring `RestClient`
- Springdoc OpenAPI
- JUnit 5, AssertJ, Mockito, Spring Security Test

## Project Structure

```text
src/main/java/com/nhuhuy05/enghub
+-- ai             # Gemini prompts, file upload, response parsing, and AI orchestration
+-- auth           # Login, token introspection, logout, invalidated tokens
+-- common         # Shared enums, exceptions, response wrappers
+-- config         # Security, CORS, OpenAPI, Cloudinary, Gemini, dotenv, bootstrapping
+-- grammar        # Grammar entities and repositories
+-- listening      # Listening dictation, group audio, transcript lines
+-- media          # Cloudinary-backed media assets
+-- notification   # Notification persistence model
+-- progress       # User progress and daily streak persistence model
+-- reading        # Part 7 reading lessons, passages, vocabulary hints
+-- test           # TOEIC tests, parts, groups, questions, answers, attempts
+-- user           # Users, roles, permissions, admin user management
+-- vocabulary     # Vocabulary topics, import, lookup, enrichment, review progress
```

Additional directories:

```text
docs/                         # Frontend integration guides
src/main/resources/db/migration # Flyway SQL migrations
src/test/java                 # Unit and Spring context tests
```

## Runtime Configuration

The application reads environment variables directly and also supports a local `.env` file through `DotenvEnvironmentPostProcessor`.

Core settings:

| Variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | HTTP server port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_NAME` | `enghub` | PostgreSQL database name |
| `DB_USER` | `postgres` | PostgreSQL username |
| `DB_PASS` | `123456` | PostgreSQL password |
| `JWT_SIGNER_KEY` | none | HS512 signing key. Required. |
| `MAX_FILE_SIZE` | `200MB` | Multipart file size limit |
| `MAX_REQUEST_SIZE` | `200MB` | Multipart request size limit |
| `MAX_SWALLOW_SIZE` | `200MB` | Tomcat swallow size limit |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | Flyway baseline flag |
| `CLOUDINARY_CLOUD_NAME` | none | Cloudinary cloud name. Required for media upload. |
| `CLOUDINARY_API_KEY` | none | Cloudinary API key. Required for media upload. |
| `CLOUDINARY_API_SECRET` | none | Cloudinary API secret. Required for media upload. |
| `CLOUDINARY_FOLDER_ROOT` | `enghub` | Cloudinary root folder |
| `CLOUDINARY_FOLDER_ENV` | `dev` | Cloudinary environment folder |
| `GEMINI_API_KEY` | none | Gemini API key. Required when Gemini is enabled. |
| `GEMINI_MODEL` | `gemini-3.1-flash-lite` | Gemini model name |
| `GEMINI_ENABLED` | `true` | Enables or disables Gemini workflows |
| `GEMINI_DELETE_FILE_AFTER_USE` | `true` | Deletes uploaded Gemini files after generation |

Application defaults from `application.properties`:

```properties
server.servlet.context-path=/enghub
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.enabled=true
allowed.origins=http://localhost:5173
springdoc.swagger-ui.path=/swagger-ui.html
```

## Local Development

### Prerequisites

- JDK 21
- PostgreSQL
- Maven Wrapper from this repository
- Optional: Cloudinary account for media upload
- Optional: Gemini API key for AI features

### 1. Create the database

```sql
CREATE DATABASE enghub;
```

### 2. Configure environment variables

PowerShell example:

```powershell
$env:PORT="8080"
$env:DB_HOST="localhost"
$env:DB_NAME="enghub"
$env:DB_USER="postgres"
$env:DB_PASS="123456"
$env:JWT_SIGNER_KEY="replace-with-a-long-random-secret-for-hs512"
$env:CLOUDINARY_CLOUD_NAME="your-cloud-name"
$env:CLOUDINARY_API_KEY="your-api-key"
$env:CLOUDINARY_API_SECRET="your-api-secret"
$env:GEMINI_API_KEY="your-gemini-api-key"
```

For local development, you can also copy `.env.example` to `.env` and fill in the values.

### 3. Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Local base URL:

```text
http://localhost:8080/enghub
```

Swagger UI:

```text
http://localhost:8080/enghub/swagger-ui.html
```

Actuator base path:

```text
http://localhost:8080/enghub/actuator
```

## Database Migrations

Schema migrations are stored in:

```text
src/main/resources/db/migration
```

The application uses Flyway and validates the schema on startup. Hibernate DDL generation is disabled for mutation by using:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Do not change production schema manually. Add a new `V{n}__description.sql` migration instead.

## Authentication and Authorization

Authentication is JWT based.

- Tokens are signed with HS512 using `JWT_SIGNER_KEY`.
- JWT `sub` is the user email.
- JWT `scope` contains `ROLE_*` entries plus permission names.
- Logout stores token `jti` in `invalidated_tokens` and the resource server rejects invalidated tokens.
- Method-level authorization is enabled with `@PreAuthorize` and related Spring Security annotations.

Public endpoints configured in `SecurityConfig`:

| Method | Endpoint |
| --- | --- |
| `POST` | `/users` |
| `POST` | `/auth/token` |
| `POST` | `/auth/introspect` |
| `GET` | `/test-collections` |
| `GET` | `/test-collections/*/tests` |
| `GET` | `/tests` |
| `GET` | `/tests/*` |
| `GET` | `/reading-lessons` |
| `GET` | `/reading-lessons/*` |
| any | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` |

All other endpoints require a Bearer token unless explicitly changed in security configuration.

Default bootstrap user:

```text
email: admin@gmail.com
password: admin
role: ADMIN
```

The user is created only when it does not already exist.

## API Overview

This is a high-level map of the main REST resources. Use Swagger UI for complete request and response schemas.

### Auth

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/token` | Authenticate and issue JWT |
| `POST` | `/auth/introspect` | Validate token |
| `POST` | `/auth/logout` | Invalidate token |

### Users, Roles, Permissions

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/users` | Register user |
| `GET` | `/users/myInfo` | Current user profile |
| `GET/PUT/DELETE` | `/users/{userId}` | User self-management endpoints |
| `GET/POST/PATCH/DELETE` | `/admin/users` | Admin user management |
| `GET/POST/DELETE` | `/roles` | Role management |
| `GET/POST/DELETE` | `/permissions` | Permission management |

### Test Catalog and Attempts

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/test-collections` | Public test collections |
| `GET` | `/test-collections/{collectionId}/tests` | Public tests by collection |
| `GET` | `/tests` | Public published tests |
| `GET` | `/tests/{testId}` | Public test detail |
| `POST` | `/attempts` | Start mock or practice attempt |
| `GET` | `/attempts` | List current user's attempts |
| `GET` | `/attempts/{attemptId}` | Attempt status |
| `GET` | `/attempts/{attemptId}/content` | Attempt content |
| `POST` | `/attempts/{attemptId}/answers` | Save or clear answer |
| `POST` | `/attempts/{attemptId}/submit` | Submit attempt |
| `GET` | `/attempts/{attemptId}/result` | Attempt result |
| `POST` | `/attempts/{attemptId}/questions/{questionId}/chat/stream` | Practice AI chat streaming |

### Admin Test Management

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/admin/test-collections` | Create collection |
| `GET` | `/admin/test-collections` | List collections |
| `POST` | `/admin/tests` | Create test |
| `GET` | `/admin/tests/{testId}` | Admin test detail |
| `POST` | `/admin/tests/{testId}/parts/init` | Initialize TOEIC parts 1-7 |
| `POST/PUT/DELETE` | `/admin/tests/{testId}/media` | Manage test media assets |
| `POST` | `/admin/tests/{testId}/import` | Import questions from Excel |
| `GET` | `/admin/tests/{testId}/preview` | Test preview summary |
| `GET` | `/admin/tests/{testId}/preview-content` | Test preview content |
| `GET` | `/admin/tests/{testId}/question-groups` | Question groups |
| `PATCH` | `/admin/tests/{testId}/publish` | Publish test |
| `PATCH` | `/admin/tests/{testId}/unpublish` | Unpublish test |

### Question Group Review and AI Support

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/admin/question-groups/{groupId}` | Group detail |
| `PATCH` | `/admin/question-groups/{groupId}/review-status` | Update review status |
| `PATCH` | `/admin/question-groups/{groupId}/images` | Update group images |
| `PATCH` | `/admin/question-groups/{groupId}/audio` | Update group audio |
| `PATCH` | `/admin/question-groups/{groupId}/transcript` | Update transcript |
| `PUT` | `/admin/question-groups/{groupId}/transcript-lines` | Replace transcript lines |
| `POST` | `/admin/question-groups/{groupId}/generate-transcript` | Gemini transcript |
| `POST` | `/admin/question-groups/{groupId}/generate-question-translation` | Gemini question translation |
| `POST` | `/admin/question-groups/{groupId}/generate-explanations` | Gemini explanations |
| `POST` | `/admin/question-groups/{groupId}/generate-ai-support` | Combined AI support |

### Listening, Reading, Vocabulary

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/listening/tests/{testId}/parts/{partNumber}/dictation` | Listening dictation session |
| `GET` | `/reading-lessons` | Public published reading lessons |
| `GET` | `/reading-lessons/{lessonId}` | Public reading lesson detail |
| `GET/POST/PUT/PATCH/DELETE` | `/admin/reading-lessons` | Admin reading lesson workflow |
| `POST` | `/admin/reading-lessons/{lessonId}/generate-ai-support` | Reading translation + vocabulary AI |
| `GET` | `/vocabulary/topics` | Learner vocabulary topics |
| `GET` | `/vocabulary/topics/{topicId}/words` | Topic words |
| `GET` | `/vocabulary/progress` | User vocabulary progress |
| `GET` | `/vocabulary/due` | Due words for review |
| `POST` | `/vocabulary/{vocabularyId}/learn` | Start learning word |
| `POST` | `/vocabulary/{vocabularyId}/review` | Review word |
| `GET/POST/PUT/DELETE` | `/admin/vocabulary` | Admin vocabulary and topic management |

## AI Integration

Gemini integration is split into small services:

```text
GeminiClientService   # generateContent and streamGenerateContent orchestration
GeminiFileService     # Gemini Files API upload/download/delete
GeminiPromptFactory   # Prompt construction
GeminiResponseParser  # JSON, transcript, and SSE parsing
QuestionGroupAiService # Admin AI support for TOEIC question groups
QuestionChatService   # Practice-mode learner AI chat context and SSE bridge
```

AI use cases:

- Audio transcript generation for TOEIC listening groups.
- Translation of questions and answer options.
- Vietnamese answer explanations.
- Reading passage extraction and translation.
- Reading vocabulary hint generation.
- Dictionary lookup translation and example translation.
- Practice question chat streamed to the frontend with SSE.

Disable AI globally with:

```properties
gemini.enabled=false
```

## Media Storage

Media files are uploaded to Cloudinary and tracked in the `media_assets` table. Media can be linked to:

- test-level image/audio assets
- question group images
- question group audio ranges
- reading passages

The application prevents deleting media that is currently referenced by question groups, audio ranges, or passages.

## Response and Error Format

Most REST responses are wrapped in `ApiResponse<T>`:

```json
{
  "code": 1000,
  "message": null,
  "result": {}
}
```

Streaming endpoints such as practice question chat use `text/event-stream` and are not wrapped in `ApiResponse`.

Common error codes:

| Code | Name | Meaning |
| --- | --- | --- |
| `1001` | `INVALID_KEY` | Invalid request data |
| `1002` | `USER_EXISTED` | Email already exists |
| `1005` | `USER_NOT_EXISTED` | User not found |
| `1006` | `UNAUTHENTICATED` | Missing or invalid authentication |
| `1007` | `UNAUTHORIZED` | Missing required role/permission |
| `1009` | `TEST_NOT_EXISTED` | Test not found or not visible |
| `1012` | `ATTEMPT_NOT_EXISTED` | Attempt not found |
| `1013` | `ATTEMPT_INVALID_STATE` | Attempt state does not allow operation |
| `1024` | `MEDIA_ASSET_NOT_EXISTED` | Media asset not found |
| `1029` | `GEMINI_DISABLED` | Gemini integration disabled |
| `1030` | `GEMINI_API_KEY_MISSING` | Missing Gemini API key |
| `1032` | `GEMINI_GENERATION_FAILED` | Gemini generation failed |
| `1033` | `GEMINI_INVALID_RESPONSE` | Gemini response shape is invalid |

## Testing

Run the full test suite:

```powershell
.\mvnw.cmd test
```

Current test coverage includes:

- Spring application context
- Listening dictation service
- Listening transcript splitting
- Test attempt lifecycle and scoring
- Question group review
- Practice question chat validation and context protection

Note: the Spring context test uses the configured PostgreSQL datasource. Ensure your local database and required environment variables are available before running the full suite.

## Frontend Integration Guides

Detailed FE contracts are available in `docs/`:

- [Admin user management](docs/admin-user-management-fe-guide.md)
- [Listening dictation](docs/listening-dictation-fe-guide.md)
- [Practice question AI chat streaming](docs/practice-question-chat-fe-guide.md)
- [Reading bilingual practice](docs/reading-bilingual-fe-guide.md)

## Security Notes

- Replace the default admin password immediately after first startup.
- Never commit real `.env` values or API keys.
- Use a long random `JWT_SIGNER_KEY` suitable for HS512.
- Keep Cloudinary and Gemini credentials in environment variables or a secrets manager.
- Restrict `allowed.origins` in deployed environments.
- Review public endpoints before production deployment.
- Do not expose correct answers to the frontend before a practice learner has answered; the practice AI chat service enforces this at backend context level.
