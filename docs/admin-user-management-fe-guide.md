# Huong Dan FE: Admin User Management

Tai lieu nay mo ta API cho trang Quan Li Nguoi Dung trong admin.

Base URL local mac dinh:

```text
http://localhost:8080/enghub
```

Tat ca endpoint ben duoi can Bearer token cua user co role `ADMIN`.

Header:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

## 1. Data Model

### Admin User

```ts
export interface AdminUser {
  id: number;
  email: string;
  full_name: string | null;
  phone: string | null;
  avatar_url: string | null;
  provider: string | null;
  active: boolean;
  roles: Role[];
  created_at: string | null;
}

export interface Role {
  name: string;
  description: string | null;
  permissions: Permission[];
}

export interface Permission {
  name: string;
  description: string | null;
}
```

### Page Response

```ts
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
  first: boolean;
  last: boolean;
}
```

## 2. Lay Danh Sach User

```http
GET /admin/users
GET /admin/users?keyword=huy&page=0&size=20
GET /admin/users?role=ADMIN&active=true&page=0&size=20
```

Query params:

- `keyword`: optional, search theo email, full name, phone.
- `role`: optional, nhan `ADMIN`, `TEACHER`, `STUDENT`; cung chap nhan `ROLE_ADMIN`.
- `active`: optional boolean.
- `page`: zero-based, default `0`.
- `size`: default `20`, max backend cap `100`.

Response:

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": 1,
        "email": "admin@gmail.com",
        "full_name": "Admin",
        "phone": null,
        "avatar_url": null,
        "provider": null,
        "active": true,
        "roles": [
          {
            "name": "ADMIN",
            "description": "ADMIN",
            "permissions": []
          }
        ],
        "created_at": "2026-06-01T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 1,
    "total_pages": 1,
    "first": true,
    "last": true
  }
}
```

## 3. Lay Chi Tiet User

```http
GET /admin/users/{userId}
```

Response: `AdminUser`.

## 4. Tao User

```http
POST /admin/users
```

Request:

```json
{
  "email": "teacher@example.com",
  "password": "12345678",
  "full_name": "Teacher One",
  "phone": "0900000000",
  "avatar_url": null,
  "active": true,
  "roles": ["TEACHER"]
}
```

Rules:

- `email` bat buoc, dung format email.
- `password` bat buoc, toi thieu 8 ky tu.
- `roles` optional. Neu khong gui, backend gan `STUDENT`.
- Role phai ton tai trong bang `roles`.

Response: `AdminUser`.

## 5. Cap Nhat User

```http
PATCH /admin/users/{userId}
```

Request co the gui mot phan:

```json
{
  "email": "teacher@example.com",
  "password": "newpassword123",
  "full_name": "Teacher Updated",
  "phone": "0911111111",
  "avatar_url": "https://example.com/avatar.png",
  "active": true,
  "roles": ["TEACHER", "STUDENT"]
}
```

Behavior:

- Field nao khong gui thi giu nguyen.
- Gui `password` blank thi backend bo qua.
- Gui `roles` thi backend replace toan bo roles cua user.
- Admin khong duoc tu deactivate chinh minh.

Response: `AdminUser`.

## 6. Bat/Tat Trang Thai User

```http
PATCH /admin/users/{userId}/status
```

Request:

```json
{
  "active": false
}
```

Behavior:

- Dung cho toggle active nhanh.
- Admin khong duoc tu deactivate chinh minh.

Response: `AdminUser`.

## 7. Xoa User

```http
DELETE /admin/users/{userId}
```

Response:

```json
{
  "code": 1000,
  "result": "User has been deleted"
}
```

Behavior:

- Admin khong duoc tu xoa chinh minh.
- Xoa user se xoa mapping `user_roles` theo cascade DB.

## 8. Lay Danh Sach Role

Trang admin co the dung endpoint role hien co:

```http
GET /roles
```

Response:

```json
{
  "code": 1000,
  "result": [
    {
      "name": "ADMIN",
      "description": "ADMIN",
      "permissions": []
    },
    {
      "name": "TEACHER",
      "description": "TEACHER",
      "permissions": []
    },
    {
      "name": "STUDENT",
      "description": "STUDENT",
      "permissions": []
    }
  ]
}
```

## 9. Error Codes Thuong Gap

- `1001 INVALID_KEY`: role khong ton tai, payload sai, hoac admin tu xoa/deactivate chinh minh.
- `1002 USER_EXISTED`: email da ton tai.
- `1003 INVALID_EMAIL`: email sai format.
- `1004 INVALID_PASSWORD`: password duoi 8 ky tu.
- `1005 USER_NOT_EXISTED`: user khong ton tai.
- `1007 UNAUTHORIZED`: token khong co role ADMIN.

## 10. Checklist FE

- Dung `/admin/users` thay vi `/users` cho trang admin.
- Search debounce keyword tren FE de tranh goi API lien tuc.
- Role filter gui `ADMIN`, `TEACHER`, `STUDENT`.
- Sau create/update/delete nen reload page hien tai.
- Khi delete item cuoi cua page, neu page rong thi lui ve page truoc.
- Toggle active dung `/admin/users/{userId}/status`.
