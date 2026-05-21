# EngHub - Workflow Tạo Đề Chi Tiết

## Tổng Quan

```text
Bước 1: Tạo hoặc chọn collection
Bước 2: Tạo test trong collection
Bước 3: Upload media kèm label
Bước 4: Tạo 7 parts
Bước 5: Import Excel từ sheet questions
Bước 6: Tạo passage cho Part 6/7
Bước 7: Gắn timestamp audio trên waveform
Bước 8: Preview
Bước 9: Publish
```

---

## Bước 1 - Tạo Hoặc Chọn Collection

Collection dùng để gom nhiều test cùng bộ, ví dụ `ETS 2023` có `Test 1` đến `Test 10`.

```http
POST /api/admin/test-collections
```

```json
{
  "name": "ETS 2023",
  "description": "Bộ 10 đề ETS TOEIC 2023"
}
```

Nếu collection đã có, admin chỉ cần chọn collection đó khi tạo test.

---

## Bước 2 - Tạo Test Trong Collection

```http
POST /api/admin/tests
```

```json
{
  "collection_id": 1,
  "test_number": 1,
  "title": "Test 1",
  "description": "Đề số 1 trong bộ ETS 2023",
  "duration_minutes": 120
}
```

Ghi chú:

- Trong cùng một collection, `test_number` không được trùng.
- Đề lẻ không thuộc collection thì `collection_id = null` và `test_number = null`.
- `title` chỉ dùng để hiển thị; thứ tự trong collection dựa vào `test_number`.
- Server tự tạo 7 TOEIC parts sau khi tạo test.

---

## Bước 3 - Upload Media Kèm Label

Upload media trước khi import Excel để import service tự match vào đúng group.

| Media | Label | Vị trí lưu |
|---|---|---|
| Ảnh Part 1 group 1 | `1` | `question_groups.media_asset_id` |
| Ảnh Part 1 group 2 | `2` | `question_groups.media_asset_id` |
| Ảnh graphic Part 3 group 68 | `68` | `question_groups.media_asset_id` |
| Audio file chính | `audio_main` | `question_group_audio_ranges.media_asset_id` |
| Ảnh passage Part 6/7 | `passage-154` | `passages.media_asset_id` |

Cloudinary public id:

```text
{folderRoot}/tests/{testId}/{mediaType}/{label}
```

Ví dụ:

```text
enghub/tests/1/audio/audio_main
enghub/tests/1/image/1
enghub/tests/1/image/68
enghub/tests/1/image/passage-154
```

Upload:

```http
POST /api/admin/tests/{test_id}/media
Content-Type: multipart/form-data
```

```text
file = @photo_part1_1.jpg
label = 1
type = image
```

Replace media:

```http
PUT /api/admin/tests/{test_id}/media/{media_asset_id}
Content-Type: multipart/form-data
```

```text
file = @new_photo.jpg
```

Delete media:

```http
DELETE /api/admin/tests/{test_id}/media/{media_asset_id}
```

---

## Bước 4 - Tạo 7 Parts

Bình thường không cần gọi vì server tự tạo khi tạo test. Nếu cần chạy lại:

```http
POST /api/admin/tests/{test_id}/parts/init
```

---

## Bước 5 - Import Excel

Giai đoạn này dùng sheet `questions`.

| Cột | Bắt buộc | Mô tả |
|---|---|---|
| `part` | có | 1-7 |
| `group_order` | có | Số câu đầu tiên của group |
| `q_number` | có | 1-200, không trùng |
| `question_text` | tùy part | Nội dung câu hỏi |
| `option_a` | có | |
| `option_b` | có | |
| `option_c` | có | |
| `option_d` | Part 1,3,4,5,6,7 | Part 2 không có D |
| `correct` | có | Part 2: A/B/C, part khác: A/B/C/D |
| `explanation` | không | Giải thích đáp án |

Quy tắc `group_order`:

| Part | Rule |
|---|---|
| 1, 2, 5 | `group_order = q_number` |
| 3, 4 | `group_order = số câu đầu tiên của conversation/talk` |
| 6, 7 | `group_order = số câu đầu tiên của passage` |

Import:

```http
POST /api/admin/tests/{test_id}/import
Content-Type: multipart/form-data
```

```text
file = @ets2023_test1.xlsx
```

Import lại khi chưa có user làm bài:

```http
POST /api/admin/tests/{test_id}/import?replace=true
Content-Type: multipart/form-data
```

Nguyên tắc:

- Validate toàn bộ file trước.
- Có lỗi thì không ghi DB.
- Nếu test đã có questions, import thường sẽ bị chặn.
- `replace=true` sẽ xoá nội dung import cũ rồi import lại.
- `replace=true` bị chặn nếu test đã có attempt.

Import service tạo:

```text
question_groups
questions
answers
question_group_audio_ranges cho Part 1-4 dùng audio_main
```

---

## Bước 6 - Tạo Passage Cho Part 6/7

Sau khi import questions, các `question_group` của Part 6/7 đã có. Admin tạo passage và gắn vào group theo `(test_id, part_number, group_order)`.

```http
POST /api/admin/passages
```

```json
{
  "test_id": 1,
  "part_number": 7,
  "group_order": 154,
  "title": "Email from HR",
  "passage_type": "email",
  "content_format": "text",
  "content_en": "Dear all, this email is to inform...",
  "content_vi": "Kính gửi mọi người...",
  "vocab_hints": "inform=thông báo;policy=chính sách",
  "media_asset_id": 15,
  "order_index": 0
}
```

Gắn hoặc đổi ảnh passage:

```http
PATCH /api/admin/passages/{passage_id}/media
```

```json
{
  "media_asset_id": 15
}
```

---

## Bước 7 - Gắn Timestamp Audio

Chỉ cần làm với Part 1-4.

```http
PATCH /api/admin/tests/{test_id}/audio-ranges
```

```json
[
  { "group_order": 1, "part_number": 1, "start_ms": 8000, "end_ms": 22000 },
  { "group_order": 32, "part_number": 3, "start_ms": 125000, "end_ms": 163000 }
]
```

Cloudinary trim URL dùng `media_assets.cloudinary_public_id`:

```text
https://res.cloudinary.com/{cloud_name}/video/upload/so_125.0,eo_163.0/{cloudinary_public_id}.mp3
```

---

## Bước 8 - Preview

```http
GET /api/admin/tests/{test_id}/preview
```

Preview kiểm tra:

- Đủ 200 câu.
- Mỗi câu có đúng 1 đáp án correct.
- Part 1 đủ ảnh.
- Part 1-4 đủ audio range hợp lệ.
- Part 6/7 đủ passage.

---

## Bước 9 - Publish

```http
PATCH /api/admin/tests/{test_id}/publish
```

Server chỉ publish khi preview không còn lỗi:

```text
tests.is_published = true
```

Sau khi publish, nếu đã có user làm bài thì không cho sửa nội dung câu hỏi, answers, passages, media mapping. Chỉ cho sửa metadata như `title`, `description`.

---

## Tóm Tắt

```text
Tạo/chọn collection
-> Tạo test với collection_id + test_number
-> Upload media theo public_id enghub/tests/{testId}/{mediaType}/{label}
-> Import Excel sheet questions
-> Tạo passage Part 6/7
-> Gắn timestamp audio Part 1-4
-> Preview
-> Publish
```
