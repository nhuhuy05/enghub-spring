# API Tạo Đề TOEIC

Tài liệu này mô tả các API FE cần để làm màn tạo đề ở giai đoạn hiện tại: tạo collection, tạo test, upload media, import Excel, gắn audio range, preview và publish.

Phần tạo/chỉnh sửa passage chưa cần làm ở màn này.

## Quy Ước Chung

Base path:

```text
/enghub
```

Tất cả API cần Bearer token:

```http
Authorization: Bearer <access_token>
```

Role được phép dùng:

```text
ADMIN hoặc TEACHER
```

Response luôn bọc:

```json
{
  "code": 1000,
  "message": null,
  "result": {}
}
```

## Flow FE

```text
1. Tạo/chọn collection
2. Tạo test
3. Upload media
4. Import Excel sheet questions
5. Gắn timestamp audio Part 1-4
6. Preview
7. Publish
```

## 1. Collections

### Tạo collection

```http
POST /enghub/admin/test-collections
Content-Type: application/json
```

Request:

```json
{
  "name": "ETS 2023",
  "description": "Bộ 10 đề ETS TOEIC 2023"
}
```

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 1,
    "name": "ETS 2023",
    "description": "Bộ 10 đề ETS TOEIC 2023",
    "created_at": "2026-05-21T10:00:00"
  }
}
```

### Lấy danh sách collections

```http
GET /enghub/admin/test-collections
```

### Lấy tests trong collection

```http
GET /enghub/admin/test-collections/{collectionId}/tests
```

Response:

```json
{
  "code": 1000,
  "result": [
    {
      "id": 10,
      "collection_id": 1,
      "collection_name": "ETS 2023",
      "test_number": 1,
      "title": "Test 1",
      "description": "Đề số 1 trong bộ ETS 2023",
      "total_questions": 200,
      "duration_minutes": 120,
      "is_published": false,
      "created_at": "2026-05-21T10:05:00"
    }
  ]
}
```

## 2. Tạo Test

```http
POST /enghub/admin/tests
Content-Type: application/json
```

Request cho test thuộc collection:

```json
{
  "collection_id": 1,
  "test_number": 1,
  "title": "Test 1",
  "description": "Đề số 1 trong bộ ETS 2023",
  "duration_minutes": 120
}
```

Request cho test lẻ:

```json
{
  "collection_id": null,
  "test_number": null,
  "title": "Mini Test",
  "description": "Đề luyện tập riêng",
  "duration_minutes": 120
}
```

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 10,
    "collection_id": 1,
    "collection_name": "ETS 2023",
    "test_number": 1,
    "title": "Test 1",
    "description": "Đề số 1 trong bộ ETS 2023",
    "total_questions": 200,
    "duration_minutes": 120,
    "is_published": false,
    "created_at": "2026-05-21T10:05:00"
  }
}
```

Backend tự tạo 7 TOEIC parts sau khi tạo test.

Rule:

- Có `collection_id` thì phải có `test_number`.
- Không có `collection_id` thì `test_number` phải null.
- Trong cùng collection, `test_number` không được trùng.
- `title` chỉ dùng để hiển thị, thứ tự dựa vào `test_number`.

### Get test detail

```http
GET /enghub/admin/tests/{testId}
```

FE can call this endpoint when opening a test detail/upload screen. Calling `GET /enghub/admin/tests/{testId}/media` is not supported.

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 10,
    "collection_id": 1,
    "collection_name": "ETS 2023",
    "test_number": 1,
    "title": "Test 1",
    "description": "Đề số 1 trong bộ ETS 2023",
    "total_questions": 200,
    "duration_minutes": 120,
    "is_published": false,
    "created_at": "2026-05-21T10:05:00"
  }
}
```

## 3. Upload Media

Media upload lên Cloudinary và lưu vào `media_assets`.

Cloudinary public id backend tự tạo:

```text
enghub/tests/{testId}/{mediaType}/{label}
```

Label nên dùng:

| Media                                                        | Label |
|--------------------------------------------------------------|---|
| Ảnh Part 1                                                   | `group_order`, ví dụ `1`, `2`, `3` |
| Ảnh Graphic/Passage/Email/Notice/Anouncement... Part 3/4/6/7 | `group_order`, ví dụ `68` |
| Audio chính Part 1-4                                         | `audio_main` |

### Upload media

```http
POST /enghub/admin/tests/{testId}/media
Content-Type: multipart/form-data
```

Form fields:

| Field | Required | Mô tả |
|---|---|---|
| `file` | yes | File ảnh hoặc audio |
| `label` | yes | Label để match media |
| `type` | yes | `image` hoặc `audio` |

Backend cũng nhận `mediaType` thay cho `type`.

Ví dụ:

```text
file = ets2023_test1.mp3
label = audio_main
type = audio
```

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 20,
    "test_id": 10,
    "label": "audio_main",
    "media_type": "audio",
    "cloudinary_public_id": "enghub/tests/10/audio/audio_main",
    "url": "https://res.cloudinary.com/...",
    "duration_ms": 2712000,
    "original_filename": "ets2023_test1.mp3",
    "created_at": "2026-05-21T10:10:00"
  }
}
```

### Replace media

```http
PUT /enghub/admin/tests/{testId}/media/{mediaAssetId}
Content-Type: multipart/form-data
```

```text
file = new_file.jpg
```

### Delete media

```http
DELETE /enghub/admin/tests/{testId}/media/{mediaAssetId}
```

Response:

```json
{
  "code": 1000,
  "result": "Media asset has been deleted"
}
```

## 4. Import Excel

Sheet bắt buộc:

```text
questions
```

Header bắt buộc:

| Cột | Bắt buộc | Mô tả |
|---|---|---|
| `part` | yes | 1-7 |
| `group_order` | yes | Số câu đầu tiên của group |
| `q_number` | yes | 1-200, không trùng |
| `question_text` | no | Nội dung câu hỏi |
| `option_a` | yes | Đáp án A |
| `option_b` | yes | Đáp án B |
| `option_c` | yes | Đáp án C |
| `option_d` | Part 1,3,4,5,6,7 | Part 2 để trống |
| `correct` | yes | Part 2: A/B/C, part khác: A/B/C/D |
| `explanation` | no | Giải thích |

Rule `group_order`:

| Part | Rule |
|---|---|
| 1, 2, 5 | `group_order = q_number` |
| 3, 4 | `group_order = câu đầu tiên của conversation/talk` |
| 6, 7 | `group_order = câu đầu tiên của passage` |

Import:

```http
POST /enghub/admin/tests/{testId}/import
Content-Type: multipart/form-data
```

```text
file = ets2023_test1.xlsx
```

Response thành công:

```json
{
  "code": 1000,
  "result": {
    "success": true,
    "summary": {
      "total_rows": 200,
      "valid_rows": 200,
      "error_count": 0
    },
    "errors": []
  }
}
```

Response có lỗi validation:

```json
{
  "code": 1000,
  "result": {
    "success": false,
    "summary": {
      "total_rows": 200,
      "valid_rows": 198,
      "error_count": 2
    },
    "errors": [
      {
        "row": 45,
        "field": "correct",
        "message": "correct phải viết hoa"
      }
    ]
  }
}
```

Quan trọng:

- Backend validate toàn bộ file trước.
- Có lỗi thì không ghi DB.
- Test đã có questions thì import thường bị chặn.
- Part 1 yêu cầu đã upload ảnh theo `label = group_order`.
- Part 1-4 yêu cầu đã upload audio `label = audio_main`.

Import lại khi chưa có user attempt:

```http
POST /enghub/admin/tests/{testId}/import?replace=true
Content-Type: multipart/form-data
```

## 5. Audio Ranges

Dùng cho Part 1-4. Sau import, backend đã tạo sẵn audio range với `start_ms = 0`, `end_ms = null`.

```http
PATCH /enghub/admin/tests/{testId}/audio-ranges
Content-Type: application/json
```

Request:

```json
[
  {
    "part_number": 1,
    "group_order": 1,
    "start_ms": 8000,
    "end_ms": 22000
  },
  {
    "part_number": 3,
    "group_order": 32,
    "start_ms": 125000,
    "end_ms": 163000
  }
]
```

Response:

```json
{
  "code": 1000,
  "result": [
    {
      "id": 100,
      "question_group_id": 50,
      "part_number": 1,
      "group_order": 1,
      "media_asset_id": 20,
      "start_ms": 8000,
      "end_ms": 22000
    }
  ]
}
```

Rule:

- `start_ms >= 0`.
- Nếu có `end_ms`, `end_ms > start_ms`.
- Group phải tồn tại.

## 6. Preview

```http
GET /enghub/admin/tests/{testId}/preview
```

Response:

```json
{
  "code": 1000,
  "result": {
    "test_id": 10,
    "question_count": 200,
    "invalid_correct_answer_count": 0,
    "part1_missing_image_count": 0,
    "listening_missing_audio_range_count": 0,
    "reading_missing_passage_count": 0,
    "publishable": true,
    "errors": []
  }
}
```

FE nên disable nút Publish nếu `publishable = false`.

Lưu ý hiện tại: backend vẫn trả `reading_missing_passage_count` vì schema đã có passage, nhưng field này chỉ mang tính tham khảo cho màn tạo đề. Nó không chặn `publishable` trong workflow hiện tại.

## 7. Publish

```http
PATCH /enghub/admin/tests/{testId}/publish
```

Thành công:

```json
{
  "code": 1000,
  "result": {
    "success": true,
    "is_published": true,
    "errors": []
  }
}
```

Thất bại:

```json
{
  "code": 1000,
  "result": {
    "success": false,
    "is_published": false,
    "errors": [
      "Part 1 still has 3 groups without an image"
    ]
  }
}
```

FE nên gọi Preview trước khi Publish.

## 8. Error Codes

| Code | Ý nghĩa |
|---:|---|
| 1006 | Chưa đăng nhập/token không hợp lệ |
| 1007 | Không đủ quyền |
| 1009 | Test không tồn tại |
| 1015 | Collection không tồn tại |
| 1016 | Collection đã tồn tại |
| 1017 | `test_number` đã tồn tại trong collection |
| 1018 | `collection_id` và `test_number` phải đi cùng nhau |
| 1019 | Media cùng `(test_id, label, media_type)` đã tồn tại |
| 1020 | Sai media type hoặc MIME type |
| 1021 | Upload/xoá Cloudinary lỗi |
| 1022 | Test đã import questions |
| 1023 | Không tìm thấy question group |
| 1024 | Không tìm thấy media asset |
| 1025 | Test đã có attempt, không được import replace |

| 1027 | Uploaded file is too large |
| 1028 | Media asset is being used by question group, passage, or audio range |

## 9. Gợi Ý State FE

```text
collection_selected
test_created
media_uploaded
questions_imported
audio_ranges_done
preview_passed
published
```

UI tối thiểu:

- Step Collection/Test: chọn collection, nhập `test_number`, title.
- Step Media: upload ảnh/audio, quản lý label.
- Step Import: upload Excel, hiển thị lỗi theo `row`, `field`, `message`.
- Step Audio Range: waveform + danh sách group Part 1-4.
- Step Preview: hiển thị checklist từ response.
- Step Publish: bật nút khi `publishable = true`.
