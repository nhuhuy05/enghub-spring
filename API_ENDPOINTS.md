# EngHub API Endpoints

Base URL (local):

`http://localhost:8080/enghub`

All responses are wrapped by:

```json
{
  "code": 1000,
  "message": "optional",
  "result": {}
}
```

## Authentication (for FE)

### FE flow de xuat

1. User login bang email/password qua `POST /auth/token`.
2. Lay `token` tu `result` va luu (uu tien memory hoac secure storage).
3. Goi API protected voi header `Authorization: Bearer <token>`.
4. Khi app reload hoac truoc action quan trong, co the validate token bang `POST /auth/introspect`.
5. Neu API tra unauthorized/forbidden, FE clear session va dieu huong ve man login.

### 1) Login

- Method: `POST`
- Path: `/auth/token`
- Auth: Public
- Content-Type: `application/json`

Request body:

```json
{
  "email": "admin@gmail.com",
  "password": "admin"
}
```

Response (example):

```json
{
  "code": 1000,
  "message": "success",
  "result": {
    "token": "<jwt-token>",
    "authenticated": true
  }
}
```

### 2) Introspect token

- Method: `POST`
- Path: `/auth/introspect`
- Auth: Public
- Content-Type: `application/json`

Request body:

```json
{
  "token": "<jwt-token>"
}
```

Response (example):

```json
{
  "code": 1000,
  "message": "success",
  "result": {
    "valid": true
  }
}
```

### 3) Logout

- Method: `POST`
- Path: `/auth/logout`
- Auth: Bearer JWT
- Content-Type: `application/json`

Headers:

```http
Authorization: Bearer <jwt-token>
```

Request body:

```json
{
  "token": "<jwt-token>"
}
```

## Users (for FE)

### User object FE thuong dung

```json
{
  "id": 1,
  "email": "student1@gmail.com",
  "fullName": "Student 1",
  "phone": "0900000000",
  "avatarUrl": "https://example.com/avatar.png",
  "roles": [
    {
      "name": "STUDENT",
      "description": "STUDENT",
      "permissions": []
    }
  ]
}
```

### 4) Create user (register)

- Method: `POST`
- Path: `/users`
- Auth: Public
- Content-Type: `application/json`

Request body:

```json
{
  "email": "student1@gmail.com",
  "password": "123456",
  "fullName": "Student 1",
  "phone": "0900000000",
  "avatarUrl": "https://example.com/avatar.png"
}
```

### 5) Get all users (admin)

- Method: `GET`
- Path: `/users`
- Auth: Bearer JWT
- Authorization: `hasRole('ADMIN')`

Headers:

```http
Authorization: Bearer <jwt-token>
```

### 6) Get user by ID (admin)

- Method: `GET`
- Path: `/users/{userId}`
- Auth: Bearer JWT
- Authorization: `hasRole('ADMIN')`

Headers:

```http
Authorization: Bearer <jwt-token>
```

### 7) Get my info

- Method: `GET`
- Path: `/users/myInfo`
- Auth: Bearer JWT

Headers:

```http
Authorization: Bearer <jwt-token>
```

### 8) Update user

- Method: `PUT`
- Path: `/users/{userId}`
- Auth: Bearer JWT
- Authorization: `ADMIN` hoac chinh user do (owner)
- Content-Type: `application/json`

Headers:

```http
Authorization: Bearer <jwt-token>
```

Request body:

```json
{
  "password": "new-password",
  "fullName": "New Name",
  "phone": "0911111111",
  "avatarUrl": "https://example.com/new-avatar.png",
  "roles": ["STUDENT", "TEACHER"]
}
```

### 9) Delete user

- Method: `DELETE`
- Path: `/users/{userId}`
- Auth: Bearer JWT

Headers:

```http
Authorization: Bearer <jwt-token>
```

## Roles

### 10) Create role

- Method: `POST`
- Path: `/roles`
- Auth: Bearer JWT

Request body:

```json
{
  "name": "CONTENT_REVIEWER",
  "description": "Can review content",
  "permissions": ["question.read", "question.update"]
}
```

### 11) Get all roles

- Method: `GET`
- Path: `/roles`
- Auth: Bearer JWT

### 12) Delete role

- Method: `DELETE`
- Path: `/roles/{role}`
- Auth: Bearer JWT
- Note: `{role}` is role name (business key), for example `STUDENT`.

## Permissions

### 13) Create permission

- Method: `POST`
- Path: `/permissions`
- Auth: Bearer JWT

Request body:

```json
{
  "name": "question.create",
  "description": "Create TOEIC questions"
}
```

### 14) Get all permissions

- Method: `GET`
- Path: `/permissions`
- Auth: Bearer JWT

### 15) Delete permission

- Method: `DELETE`
- Path: `/permissions/{permission}`
- Auth: Bearer JWT
- Note: `{permission}` is permission name (business key), for example `question.create`.

## Security Summary

Public endpoints (`POST` only):

- `/users`
- `/auth/token`
- `/auth/introspect`

All other endpoints require Bearer JWT.
