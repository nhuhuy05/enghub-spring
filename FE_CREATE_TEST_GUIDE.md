# Hướng Dẫn FE: Feature Tạo Đề Thi

Tài liệu này mô tả luồng FE cần làm cho màn hình tạo đề thi TOEIC theo backend hiện tại.

Base URL mặc định:

```text
/enghub
```

Tất cả API admin/teacher bên dưới đều cần token hợp lệ và role:

```text
ADMIN hoặc TEACHER
```

Vì backend cho cả `ADMIN` và `TEACHER`, FE nên mở route/menu tạo đề cho cả hai role nếu admin cũng cần quản trị đề. Nếu product muốn chỉ giáo viên tạo đề thì FE có thể chặn ở UI, nhưng đó là rule UI riêng, không phải rule backend.

Response backend bọc trong `ApiResponse`:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```

FE luôn đọc data từ `response.result`.

## 1. Luồng Tổng Thể

Feature tạo đề gồm 7 bước:

1. Chọn hoặc tạo `test_collection`.
   2. Tạo `test`.
   3. Upload media theo từng file ảnh/audio.
   4. Import Excel câu hỏi và transcript.
   5. Review và chỉnh sửa từng `question_group`.
   6. Preview đề thi.
   7. Public đề thi.

Trạng thái workflow của test:

```text
draft -> media_uploaded -> imported -> reviewing -> preview_ready -> published
```

Hiện tại sau khi import thành công, backend đưa test về trạng thái:

```text
reviewing
```

Backend chỉ cho publish khi:

- Đủ 200 câu hỏi.
  - Mỗi câu có đúng 1 đáp án đúng.
  - Part 1 có ảnh.
  - Part 1-4 có audio hợp lệ.
  - Part 6-7 có passage/ảnh.
  - Tất cả question group đã được mark `reviewed`.

## 2. Màn Hình Đề Xuất

FE nên chia flow thành các màn hình/bước sau:

```text
Create Test
Upload Media
Import Excel
Review Groups
Preview
Publish
```

Trong `Review Groups`, nên có layout:

```text
Left panel: danh sách group
Right panel: chi tiết group đang chọn
```

Danh sách group hiển thị:

- Part.
  - Range câu hỏi, ví dụ `131-134`.
  - Trạng thái `needs_review` hoặc `reviewed`.
  - Missing flags nếu thiếu media/audio/passage/question/answer.

Chi tiết group cho phép sửa:

- Ảnh của group.
  - Audio, start/end, transcript.
  - Passage Part 6/7.
  - Câu hỏi.
  - Đáp án.
  - Giải thích tiếng Việt.
  - Mark reviewed.

## 3. Tạo Collection

### Tạo collection

```http
POST /enghub/admin/test-collections
Content-Type: application/json
```

Body ví dụ:

```json
{
  "name": "ETS 2024",
  "description": "Bộ đề ETS 2024"
}
```

### Lấy danh sách collection

```http
GET /enghub/admin/test-collections
```

### Lấy tests trong collection

```http
GET /enghub/admin/test-collections/{collectionId}/tests
```

## 4. Tạo Test

```http
POST /enghub/admin/tests
Content-Type: application/json
```

Body ví dụ:

```json
{
  "collection_id": 1,
  "title": "ETS Test 01",
  "description": "Full test 200 câu",
  "duration_minutes": 120
}
```

Sau khi tạo test, backend tự tạo 7 part.

Lấy chi tiết test:

```http
GET /enghub/admin/tests/{testId}
```

Response `result` có `workflow_status`, FE có thể dùng để hiển thị trạng thái wizard:

```json
{
  "id": 12,
  "collection_id": 1,
  "collection_name": "ETS 2024",
  "test_number": 1,
  "title": "ETS Test 01",
  "description": "Full test 200 câu",
  "total_questions": 200,
  "duration_minutes": 120,
  "is_published": false,
  "workflow_status": "reviewing",
  "created_at": "2026-05-29T19:30:00"
}
```

## 5. Upload Media

Endpoint upload 1 file:

```http
POST /enghub/admin/tests/{testId}/media
Content-Type: multipart/form-data
```

Form data:

```text
file: File
label: string
type: image | audio
```

Có thể gửi `mediaType` thay cho `type` nếu FE đang dùng field đó.

### Cloudinary folder

FE không cần gửi `bucket`, `folder` hoặc `partNumber` khi upload media.

Backend tự lưu Cloudinary theo `type`:

```text
type=image -> enghub/{env}/tests/{testId}/images/{label}
type=audio -> enghub/{env}/tests/{testId}/audios/{label}
```

Ví dụ:

```text
label = 131-134, type = image
-> enghub/dev/tests/12/images/131-134

label = p03-q032-034, type = audio
-> enghub/dev/tests/12/audios/p03-q032-034
```

FE chỉ cần gửi đúng:

```text
file
label
type hoặc mediaType
```

### Rule label quan trọng

FE nên mặc định lấy `label` bằng tên file bỏ phần extension.

Ví dụ:

```text
1.png              -> 1
1.mp3              -> 1
p01-q001.png       -> p01-q001
p03-q032-034.mp3   -> p03-q032-034
131-134.png        -> 131-134
176-180(1).png     -> 176-180(1)
176-180(2).png     -> 176-180(2)
audio_main.mp3     -> audio_main
```

Backend sẽ dùng `label` để auto match vào question group khi import Excel.

### Pattern label được hỗ trợ

Canonical:

```text
p01-q001
p03-q032-034
p07-q176-180-01
```

Range đơn giản:

```text
1
2
3
131-134
147-148
176-180
```

Lưu ý: label số đơn như `1`, `2`, `3` chỉ nên dùng khi nó chính là `q_number` thật, ví dụ ảnh Part 1 cho câu 1, 2, 3. Không dùng `group_order` làm media label cho Part 3/4, vì Part 3 group 1 thực chất là range câu `32-34`, nên label đúng nên là `32-34` hoặc `p03-q032-034`.

Range có nhiều ảnh:

```text
176-180(1)
176-180(2)
181-185(1)
181-185(2)
181-185(3)
```

Part 6/7 có thể thêm title passage vào filename bằng suffix sau `_`:

```text
{start}-{end}({page})_{title}
```

Ví dụ:

```text
191-195(1)_invitation-webpage
191-195(2)_email
176-180(1)_webpage
176-180(2)_order-form
```

Backend vẫn match group theo phần `start-end(page)`, còn phần sau `_` được dùng làm `question_group_passages.title`.

Rule title:

```text
lowercase
không dấu
nối từ bằng dấu -
không dùng space
không dùng _ trong title
```

Audio chung tạm thời:

```text
audio_main
```

### Cách upload theo file user đưa

Part 6:

```text
131-134.png
133-137.png
```

FE upload từng file:

```text
file = 131-134.png, label = 131-134, type = image
file = 133-137.png, label = 133-137, type = image
```

Part 7:

```text
147-148.png
149-150.png
176-180(1).png
176-180(2).png
181-185(1).png
181-185(2).png
181-185(3).png
```

FE upload từng file, label tương ứng là tên file bỏ `.png`.

### Lấy danh sách media đã upload

FE dùng endpoint này để load lại media sau refresh, hoặc khi vào `Review Groups` cần chọn media thủ công.

```http
GET /enghub/admin/tests/{testId}/media
```

Response `result` là list:

```json
[
  {
    "id": 88,
    "test_id": 12,
    "label": "131-134",
    "media_type": "image",
    "cloudinary_public_id": "enghub/dev/tests/12/images/131-134",
    "url": "https://res.cloudinary.com/...",
    "duration_ms": null,
    "original_filename": "131-134.png",
    "created_at": "2026-05-29T19:30:00"
  },
  {
    "id": 89,
    "test_id": 12,
    "label": "p03-q032-034",
    "media_type": "audio",
    "cloudinary_public_id": "enghub/dev/tests/12/audios/p03-q032-034",
    "url": "https://res.cloudinary.com/...",
    "duration_ms": 27000,
    "original_filename": "p03-q032-034.mp3",
    "created_at": "2026-05-29T19:31:00"
  }
]
```

### Update media file

```http
PUT /enghub/admin/tests/{testId}/media/{mediaAssetId}
Content-Type: multipart/form-data
```

Form data:

```text
file: File
```

### Xóa media

```http
DELETE /enghub/admin/tests/{testId}/media/{mediaAssetId}
```

## 6. Import Excel

```http
POST /enghub/admin/tests/{testId}/import?replace=false
Content-Type: multipart/form-data
```

Form data:

```text
file: Excel file
```

Tham số `replace`:

| Giá trị | Ý nghĩa | Khi nào dùng |
| --- | --- | --- |
| `false` | Import thêm/cập nhật theo dữ liệu hiện có, không chủ động xóa toàn bộ data cũ trước khi import | Dùng mặc định khi test mới tạo hoặc chỉ import lần đầu |
| `true` | Import lại từ đầu, backend xóa dữ liệu câu hỏi/group/media mapping đã sinh từ lần import trước rồi tạo lại | Dùng khi file Excel sai nhiều, muốn làm lại sạch |

Ví dụ import lại từ đầu:

```http
POST /enghub/admin/tests/{testId}/import?replace=true
```

Sau khi import thành công, backend sẽ:

- Tạo/cập nhật `question_groups`.
  - Tạo/cập nhật `questions`.
  - Tạo/cập nhật `answers`.
  - Match media đã upload vào group dựa trên `media_assets.label`.
  - Match transcript vào audio group nếu có sheet `transcripts`.
  - Đưa test về workflow status `reviewing`.

### Sheet bắt buộc: `questions`

Sheet `questions` là sheet chính để tạo cấu trúc đề. Mỗi dòng tương ứng với 1 câu hỏi.

Header khuyến nghị theo đúng thứ tự:

```text
part
group_order
q_number
question_text
question_text_vi
option_a
option_a_vi
option_b
option_b_vi
option_c
option_c_vi
option_d
option_d_vi
correct
explanation
```

### Ý nghĩa các cột sheet `questions`

| Cột | Bắt buộc | Kiểu dữ liệu | Áp dụng | Backend dùng để làm gì | Lưu ý |
| --- | --- | --- | --- | --- | --- |
| `part` | Có | Number | Part 1-7 | Xác định câu hỏi thuộc part nào | Chỉ nhận giá trị `1` đến `7` |
| `group_order` | Có | Number | Tất cả part | Gom các câu vào cùng một `question_group` trong part đó | Các câu cùng `part` và cùng `group_order` sẽ nằm chung 1 group |
| `q_number` | Có | Number | Tất cả part | Lưu số thứ tự câu hỏi thật trong đề | Nên chạy từ `1` đến `200`, không trùng |
| `question_text` | Có theo header, nội dung có thể trống | Text | Tất cả part | Lưu câu hỏi tiếng Anh vào `questions.question_text_en` | Part 1-4 có thể để trống nếu đề gốc không hiện text câu hỏi |
| `question_text_vi` | Không | Text | Tất cả part | Lưu bản dịch câu hỏi vào `questions.question_text_vi` | FE nên cho giáo viên sửa lại ở bước review |
| `option_a` | Có theo header, nội dung có thể trống | Text | Tất cả part | Tạo answer label `A`, lưu vào `answers.answer_text_en` | Part 1/2 có thể để trống vì đáp án được đọc trong audio và có thể được AI fill sau khi gen transcript |
| `option_a_vi` | Không | Text | Tất cả part | Lưu bản dịch đáp án A vào `answers.answer_text_vi` | Có thể để trống và bổ sung sau |
| `option_b` | Có theo header, nội dung có thể trống | Text | Tất cả part | Tạo answer label `B`, lưu vào `answers.answer_text_en` | Part 1/2 có thể để trống |
| `option_b_vi` | Không | Text | Tất cả part | Lưu bản dịch đáp án B vào `answers.answer_text_vi` |  |
| `option_c` | Có theo header, nội dung có thể trống | Text | Tất cả part | Tạo answer label `C`, lưu vào `answers.answer_text_en` | Part 1/2 có thể để trống |
| `option_c_vi` | Không | Text | Tất cả part | Lưu bản dịch đáp án C vào `answers.answer_text_vi` |  |
| `option_d` | Có theo header, nội dung có thể trống | Text | Part 1, 3, 4, 5, 6, 7 | Tạo answer label `D`, lưu vào `answers.answer_text_en` | Part 2 có thể để trống nếu chỉ có A/B/C |
| `option_d_vi` | Không | Text | Part 1, 3, 4, 5, 6, 7 | Lưu bản dịch đáp án D vào `answers.answer_text_vi` | Nếu `option_d` trống thì `option_d_vi` cũng nên trống |
| `correct` | Có | Text | Tất cả part | Đánh dấu đáp án đúng bằng `answers.is_correct` | Chỉ nhập `A`, `B`, `C` hoặc `D`. Part 2 không nên nhập `D` nếu không có đáp án D |
| `explanation` | Không | Text | Tất cả part | Lưu giải thích tiếng Việt vào `questions.explanation_vi` | Hiện tại chỉ cần giải thích tiếng Việt, không có `explanation_en` |

### Cách hiểu `group_order`

`group_order` là thứ tự nhóm câu trong từng part, không phải số câu hỏi.

Ví dụ Part 6:

| part | group_order | q_number | Ý nghĩa |
| --- | --- | --- | --- |
| 6 | 1 | 131 | Câu 131 thuộc group đầu tiên của Part 6 |
| 6 | 1 | 132 | Câu 132 vẫn thuộc group đầu tiên |
| 6 | 1 | 133 | Câu 133 vẫn thuộc group đầu tiên |
| 6 | 1 | 134 | Câu 134 vẫn thuộc group đầu tiên |
| 6 | 2 | 135 | Câu 135 thuộc group thứ hai của Part 6 |

Như vậy group Part 6 đầu tiên sẽ có range:

```text
131-134
```

Backend sẽ dùng range này để match media label như:

```text
131-134
p06-q131-134
```

Ví dụ Part 7 có một passage nhiều ảnh:

| part | group_order | q_number | Media label có thể match |
| --- | --- | --- | --- |
| 7 | 8 | 176 | `176-180(1)_webpage`, `176-180(2)_email` |
| 7 | 8 | 177 | `176-180(1)_webpage`, `176-180(2)_email` |
| 7 | 8 | 178 | `176-180(1)_webpage`, `176-180(2)_email` |
| 7 | 8 | 179 | `176-180(1)_webpage`, `176-180(2)_email` |
| 7 | 8 | 180 | `176-180(1)_webpage`, `176-180(2)_email` |

Các file `176-180(1)_webpage.png` và `176-180(2)_email.png` sẽ được lưu thành nhiều passage image trong cùng group, sắp xếp theo hậu tố `(1)`, `(2)`. Title tự sinh lần lượt là `webpage` và `email`, giáo viên có thể sửa lại ở bước Review Groups.

### Ví dụ dòng dữ liệu sheet `questions`

Part 5, một câu độc lập:

| part | group_order | q_number | question_text | question_text_vi | option_a | option_a_vi | option_b | option_b_vi | option_c | option_c_vi | option_d | option_d_vi | correct | explanation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 5 | 1 | 101 | The report ___ by Friday. | Báo cáo ___ trước thứ Sáu. | complete | hoàn thành | completed | đã hoàn thành | must complete | phải hoàn thành | must be completed | phải được hoàn thành | D | Câu cần bị động vì chủ ngữ là báo cáo. |

Part 6, nhiều câu chung một passage:

| part | group_order | q_number | question_text | correct |
| --- | --- | --- | --- | --- |
| 6 | 1 | 131 | Question 131 | A |
| 6 | 1 | 132 | Question 132 | C |
| 6 | 1 | 133 | Question 133 | B |
| 6 | 1 | 134 | Question 134 | D |

Part 2, nếu chưa có nội dung đáp án in trong đề thì có thể để trống A/B/C/D và chỉ nhập đáp án đúng:

| part | group_order | q_number | option_a | option_b | option_c | option_d | correct |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2 | 3 | 9 |  |  |  |  | A |

Lưu ý:

- `explanation` hiện tại là tiếng Việt, backend lưu vào `explanation_vi`.
  - Part 1-4 vẫn có thể để trống `question_text` nếu đề gốc không hiện câu hỏi.
  - Part 1/2 có thể để trống `option_a/b/c/d`; sau khi gen transcript, backend có thể fill `answer_text_en/vi` từ audio.
  - Part 2 chỉ có 3 đáp án A/B/C thì FE/import file vẫn nên để cột D trống.
  - FE nên validate trước khi upload/import: `q_number` không trùng, `part` nằm trong 1-7, `correct` nằm trong A-D, và đáp án đúng không được trỏ tới option đang trống.
  - Sau import, giáo viên vẫn phải qua bước `Review Groups` để check/sửa từng group trước khi publish.

### Sheet optional: `transcripts`

Dùng để nhập transcript cho Part 1-4 theo group. Mỗi dòng tương ứng với 1 `question_group`, không phải 1 câu hỏi.

Header:

```text
part
group_order
transcript_en
transcript_vi
```

### Ý nghĩa các cột sheet `transcripts`

| Cột | Bắt buộc | Kiểu dữ liệu | Áp dụng | Backend dùng để làm gì | Lưu ý |
| --- | --- | --- | --- | --- | --- |
| `part` | Có | Number | Part 1-4 | Xác định transcript thuộc part nào | Chỉ dùng cho listening parts |
| `group_order` | Có | Number | Part 1-4 | Match transcript vào đúng `question_group` | Phải trùng với `group_order` trong sheet `questions` |
| `transcript_en` | Không | Text dài | Part 1-4 | Lưu vào `question_group_audios.transcript_en` | Có thể nhập nhiều dòng trong cùng một cell |
| `transcript_vi` | Không | Text dài | Part 1-4 | Lưu vào `question_group_audios.transcript_vi` | Đây là bản dịch transcript |

Ví dụ sheet `transcripts`:

| part | group_order | transcript_en | transcript_vi |
| --- | --- | --- | --- |
| 3 | 1 | M: Good morning, how can I help you? W: I need to change my reservation. | Nam: Chào buổi sáng, tôi có thể giúp gì cho cô? Nữ: Tôi cần đổi đặt chỗ của mình. |
| 4 | 2 | Good afternoon. This announcement is for all passengers... | Chào buổi chiều. Thông báo này dành cho tất cả hành khách... |

Rule match:

```text
questions.part + questions.group_order
= transcripts.part + transcripts.group_order
```

Nếu không có sheet `transcripts`, FE vẫn có thể cho giáo viên nhập/sửa transcript thủ công ở bước Review Groups bằng API:

```http
PATCH /enghub/admin/question-groups/{groupId}/transcript
```

Hiện tại FE chưa cần làm `transcript_lines`. Bảng đó để dành cho tính năng chép chính tả sau này.

## 7. Review Question Groups

### Lấy danh sách groups của test

```http
GET /enghub/admin/tests/{testId}/question-groups
```

Response `result` là list:

```json
[
  {
    "id": 101,
    "part_number": 6,
    "group_order": 1,
    "question_numbers": "131-134",
    "review_status": "needs_review",
    "missing_flags": []
  }
]
```

`missing_flags` có thể dùng để hiện warning. Ví dụ:

```text
missing_image
missing_audio
missing_passage
missing_question
missing_answer
```

`missing_flags` hiện là `string[]`, chưa có detail theo field/question. FE có thể dùng group id hiện tại để nhảy về đúng group, còn trong group thì highlight theo flag:

| Flag | Ý nghĩa gợi ý |
| --- | --- |
| `missing_questions` | Group chưa có câu hỏi |
| `missing_image` | Part 1 thiếu ảnh |
| `missing_audio` | Part 1-4 thiếu audio |
| `missing_passage` | Part 6/7 thiếu passage |

### Lấy chi tiết 1 group

```http
GET /enghub/admin/question-groups/{groupId}
```

Response `result`:

```json
{
  "id": 101,
  "part_number": 6,
  "group_order": 1,
  "review_status": "needs_review",
  "images": [],
  "audio": null,
  "passages": [
    {
      "id": 501,
      "media_asset_id": 88,
      "label": "131-134",
      "url": "/enghub/uploads/...",
      "title": null,
      "passage_type": "image",
      "content_format": "image",
      "content_en": null,
      "content_vi": null,
      "vocab_hints": null,
      "order_index": 0
    }
  ],
  "questions": [
    {
      "id": 9001,
      "question_number": 131,
      "question_text_en": "What is suggested about the company?",
      "question_text_vi": "Điều gì được gợi ý về công ty?",
      "explanation_vi": "Đáp án đúng vì...",
      "answers": [
        {
          "id": 1,
          "label": "A",
          "answer_text_en": "It is expanding.",
          "answer_text_vi": "Nó đang mở rộng.",
          "is_correct": true
        }
      ]
    }
  ]
}
```

## 8. Sửa Media Của Group

### Sửa images

Dùng cho Part 1 hoặc group có ảnh minh họa.

```http
PATCH /enghub/admin/question-groups/{groupId}/images
Content-Type: application/json
```

Body:

```json
{
  "images": [
    {
      "media_asset_id": 10,
      "order_index": 0
    }
  ]
}
```

API này thay thế toàn bộ images của group.

Nếu gửi:

```json
{
  "images": []
}
```

Backend sẽ hiểu là xóa hết images của group.

### Sửa audio

Dùng cho Part 1-4.

```http
PATCH /enghub/admin/question-groups/{groupId}/audio
Content-Type: application/json
```

Body:

```json
{
  "media_asset_id": 20,
  "start_ms": 123000,
  "end_ms": 150000,
  "transcript_en": "Where is the conversation taking place?",
  "transcript_vi": "Cuộc hội thoại đang diễn ra ở đâu?"
}
```

Nếu mỗi group có 1 file audio riêng, FE có thể để:

```json
{
  "media_asset_id": 20,
  "start_ms": null,
  "end_ms": null,
  "transcript_en": "...",
  "transcript_vi": "..."
}
```

Nếu dùng 1 audio lớn cho nhiều group, FE cần gửi `start_ms` và `end_ms`.

### Sửa transcript riêng

Nếu chỉ sửa transcript, không đổi file audio:

```http
PATCH /enghub/admin/question-groups/{groupId}/transcript
Content-Type: application/json
```

Body:

```json
{
  "transcript_en": "Speaker A: ...",
  "transcript_vi": "Người nói A: ..."
}
```

### Generate AI support

Các API này dùng ở màn `Review Groups`. Backend gọi Gemini, lưu kết quả vào DB, đánh dấu group về `needs_review`, rồi trả về `QuestionGroupDetailResponse` mới nhất.

Hiện tại backend **không còn API generate toàn bộ test**. FE chỉ cần hỗ trợ:

- gen lẻ 3 phần
  - gen cả 1 group

#### Gen lẻ từng phần

Gen lẻ luôn ghi đè field tương ứng. FE nên confirm trước khi gọi vì nội dung giáo viên đã sửa có thể bị thay thế.

Generate transcript audio:

```http
POST /enghub/admin/question-groups/{groupId}/generate-transcript
Content-Type: application/json
```

Không cần body. Backend yêu cầu group đã có `audio.media_asset_id`.

Kết quả lưu vào:

| Field | Nơi lưu |
| --- | --- |
| `transcript_en` | `question_group_audios.transcript_en` |
| `transcript_vi` | `question_group_audios.transcript_vi` |
| `answer_text_en` | `answers.answer_text_en`, chỉ Part 1/2 |
| `answer_text_vi` | `answers.answer_text_vi`, chỉ Part 1/2 |

Với Part 1/2, audio có chứa nội dung đáp án. Khi gọi API này:

- Part 1: backend yêu cầu Gemini tách đáp án `A/B/C/D`.
  - Part 2: backend yêu cầu Gemini tách đáp án `A/B/C`.
  - Backend chỉ cập nhật text đáp án, không đổi `is_correct`.
  - Response là `QuestionGroupDetailResponse` mới nhất, FE nên dùng response này để replace state group hiện tại.

Generate bản dịch câu hỏi/đáp án:

```http
POST /enghub/admin/question-groups/{groupId}/generate-question-translation
Content-Type: application/json
```

Không cần body.

Kết quả lưu vào:

| Field | Nơi lưu |
| --- | --- |
| `question_text_vi` | `questions.question_text_vi` |
| `answer_text_vi` | `answers.answer_text_vi` |

Generate giải thích tiếng Việt:

```http
POST /enghub/admin/question-groups/{groupId}/generate-explanations
Content-Type: application/json
```

Không cần body.

Kết quả lưu vào:

| Field | Nơi lưu |
| --- | --- |
| `explanation_vi` | `questions.explanation_vi` |

Backend tự gửi kèm visual media đã match vào group khi gọi Gemini:

| Media | Nguồn |
| --- | --- |
| Part 1 image | `question_group_images` |
| Part 3/4 graphic | `question_group_images` |
| Part 6/7 passage image | `question_group_passages.media_asset` |

FE không cần gửi image/audio URL trong request generate explanation.

`generate-explanations` yêu cầu đủ context:

| Part | Context cần có |
| --- | --- |
| 1 | transcript + image + answer text + correct answer |
| 2 | transcript + answer text + correct answer |
| 3/4 | transcript + correct answer, thêm image nếu group có graphic |
| 5 | question text + answers + correct answer |
| 6/7 | passage text hoặc passage image + answers + correct answer |

Nếu thiếu context, backend trả `AI_MISSING_REQUIRED_CONTEXT`. Ví dụ muốn gen explanation cho Part 1/2 thì nên gen transcript trước để backend có transcript và answer text.

#### Gen cả group

```http
POST /enghub/admin/question-groups/{groupId}/generate-ai-support
Content-Type: application/json
```

Body:

```json
{
  "transcript": true,
  "question_translation": true,
  "explanation": true,
  "overwrite": false
}
```

Quy tắc:

| Field | Ý nghĩa |
| --- | --- |
| `transcript` | Có gen transcript không |
| `question_translation` | Có gen dịch câu hỏi/đáp án không |
| `explanation` | Có gen giải thích không |
| `overwrite` | `false` chỉ fill field trống, `true` ghi đè |

Backend luôn chạy theo thứ tự:

```text
transcript -> question_translation -> explanation
```

Với Part 1/2, bước `transcript` sẽ fill `answer_text_en/vi` trước, sau đó bước `question_translation` và `explanation` dùng dữ liệu này.

Response là `QuestionGroupDetailResponse`.

AI chỉ tạo bản nháp. Giáo viên vẫn phải review/sửa thủ công và mark reviewed trước khi preview/publish.

## 9. Sửa Passage Part 6/7

```http
PATCH /enghub/admin/question-groups/{groupId}/passages
Content-Type: application/json
```

Body với passage dạng ảnh:

```json
{
  "passages": [
    {
      "media_asset_id": 88,
      "title": null,
      "passage_type": "image",
      "content_format": "image",
      "content_en": null,
      "content_vi": null,
      "vocab_hints": null,
      "order_index": 0
    },
    {
      "media_asset_id": 89,
      "title": null,
      "passage_type": "image",
      "content_format": "image",
      "content_en": null,
      "content_vi": null,
      "vocab_hints": null,
      "order_index": 1
    }
  ]
}
```

Body với passage dạng text:

```json
{
  "passages": [
    {
      "media_asset_id": null,
      "title": "Notice",
      "passage_type": "text",
      "content_format": "text",
      "content_en": "The office will be closed on Friday...",
      "content_vi": "Văn phòng sẽ đóng cửa vào thứ Sáu...",
      "vocab_hints": "closed: đóng cửa",
      "order_index": 0
    }
  ]
}
```

API này thay thế toàn bộ passages của group.

Nếu gửi:

```json
{
  "passages": []
}
```

Backend sẽ hiểu là xóa hết passages của group.

## 10. Sửa Câu Hỏi Và Đáp Án

### Sửa câu hỏi

```http
PATCH /enghub/admin/questions/{questionId}
Content-Type: application/json
```

Body:

```json
{
  "question_text_en": "What is suggested about the company?",
  "question_text_vi": "Điều gì được gợi ý về công ty?",
  "explanation_vi": "Đáp án đúng vì câu trong bài đọc nói rằng..."
}
```

Backend trả về chi tiết group mới nhất.

### Sửa đáp án

```http
PATCH /enghub/admin/answers/{answerId}
Content-Type: application/json
```

Body:

```json
{
  "answer_text_en": "It is expanding.",
  "answer_text_vi": "Nó đang mở rộng.",
  "is_correct": true
}
```

Nếu `is_correct = true`, backend tự set các đáp án khác trong cùng câu hỏi thành false.

Backend trả về `QuestionGroupDetailResponse` mới nhất, giống API sửa câu hỏi. FE có thể lấy response này để replace state của group hiện tại, không bắt buộc gọi lại `GET /question-groups/{groupId}` ngay sau đó.

## 11. Mark Reviewed

Sau khi giáo viên check xong group:

```http
PATCH /enghub/admin/question-groups/{groupId}/review-status
Content-Type: application/json
```

Body:

```json
{
  "review_status": "reviewed"
}
```

Nếu cần mở lại để sửa:

```json
{
  "review_status": "needs_review"
}
```

FE nên chỉ cho bấm `Mark reviewed` khi group không còn missing flag nghiêm trọng.

## 12. Preview

Có 2 API preview khác nhau.

### Preview validation

Dùng để biết đề có publish được không.

```http
GET /enghub/admin/tests/{testId}/preview
```

Response `result`:

```json
{
  "test_id": 12,
  "question_count": 200,
  "invalid_correct_answer_count": 0,
  "part1_missing_image_count": 0,
  "listening_missing_audio_range_count": 0,
  "reading_missing_passage_count": 0,
  "publishable": true,
  "errors": []
}
```

Ví dụ khi chưa hợp lệ:

```json
{
  "test_id": 12,
  "question_count": 198,
  "invalid_correct_answer_count": 2,
  "part1_missing_image_count": 1,
  "listening_missing_audio_range_count": 3,
  "reading_missing_passage_count": 1,
  "publishable": false,
  "errors": [
    "Test must have exactly 200 questions, current count is 198",
    "2 questions do not have exactly one correct answer",
    "Part 1 still has 1 groups without an image",
    "Part 1-4 still has 3 groups without a valid audio range",
    "Part 6-7 still has 1 groups without passage content",
    "5 question groups have not been reviewed"
  ]
}
```

Response gồm checklist. FE nên dùng API này để:

- Hiện danh sách lỗi cần sửa.
  - Disable nút publish nếu chưa hợp lệ.
  - Điều hướng người dùng về group cần sửa.

### Preview content

Dùng để render bản xem trước đề thi giống user sẽ làm.

```http
GET /enghub/admin/tests/{testId}/preview-content
```

Response `result`:

```json
{
  "test_id": 1,
  "title": "ETS Test 01",
  "description": "Full test 200 câu",
  "duration_minutes": 120,
  "parts": [
    {
      "part_number": 1,
      "title": "Part 1",
      "groups": []
    }
  ]
}
```

Mỗi `group` trong `parts[].groups` có cùng shape với `QuestionGroupDetailResponse`.

FE preview nên render:

- Audio player cho Part 1-4.
  - Ảnh Part 1 nếu có.
  - Transcript chỉ hiện trong chế độ giáo viên, không cần hiện cho học viên nếu đang preview như bài thi thật.
  - Passage/ảnh Part 6-7.
  - Câu hỏi, đáp án, bản dịch, giải thích.

Nếu giáo viên thấy sai:

```text
Preview -> quay lại Review Groups -> sửa group -> Mark reviewed lại -> Preview lại
```

## 13. Publish

```http
PATCH /enghub/admin/tests/{testId}/publish
```

Backend sẽ validate lại. Nếu fail, FE hiện lỗi từ response và giữ người dùng ở preview/review.

Khi thành công:

```text
tests.is_published = true
tests.workflow_status = published
```

### Unpublish

Dùng khi đề đã public nhưng cần ẩn khỏi phía học viên để sửa lại hoặc xử lý lỗi.

```http
PATCH /enghub/admin/tests/{testId}/unpublish
```

Response `result`:

```json
{
  "success": true,
  "is_published": false,
  "errors": []
}
```

Khi thành công:

```text
tests.is_published = false
tests.workflow_status = reviewing
```

Unpublish không xóa câu hỏi, media, attempts cũ hoặc dữ liệu review. Nó chỉ ẩn test khỏi trạng thái published để giáo viên/admin có thể chỉnh sửa rồi preview/publish lại.

## 14. Gợi Ý UX Cho FE

### Upload media

Nên có bảng media đã upload:

```text
Filename | Label | Type | Status | Preview | Actions
```

Trong lúc upload nhiều file:

- Upload từng file.
  - Hiện progress từng file.
  - Nếu file fail, cho retry file đó.
  - Cho sửa label trước khi import Excel.

### Import Excel

Sau khi import thành công, FE nên chuyển sang `Review Groups`.

Nếu import fail:

- Hiện message lỗi.
  - Hiện tên sheet/cột/row nếu backend trả về.
  - Không publish.

### Review Groups

Nên filter theo:

```text
All
Needs review
Reviewed
Missing media
Missing audio
Missing passage
```

Nên có nút:

```text
Previous group
Next group
Save
Mark reviewed
Back to needs review
```

Khi user sửa bất kỳ field nào trong group đã reviewed, FE nên cân nhắc gọi:

```http
PATCH /enghub/admin/question-groups/{groupId}/review-status
```

với:

```json
{
  "review_status": "needs_review"
}
```

để bắt giáo viên check lại.

### Preview

Nút publish chỉ nên enable khi:

- API `/preview` báo hợp lệ.
  - Không còn group `needs_review`.

## 15. Quy Tắc Render Theo Part

Part 1:

- Mỗi group thường là 1 câu.
  - Cần ảnh trong `images`.
  - Cần audio trong `audio`.
  - Câu hỏi có thể rỗng hoặc có text tùy data.
  - Khi người học đang làm bài, FE chỉ render các lựa chọn dạng label `(A) (B) (C) (D)`, không hiện nội dung đáp án.
  - Khi giáo viên review hoặc người học xem kết quả sau nộp bài, FE có thể hiện `answer_text_en` và `answer_text_vi` nếu backend đã có dữ liệu.

Part 2:

- Mỗi group thường là 1 câu.
  - Cần audio.
  - Thường không có ảnh/passage.
  - Có thể chỉ có A/B/C.
  - Khi người học đang làm bài, FE chỉ render `(A) (B) (C)`.
  - Khi review/kết quả sau nộp bài, FE có thể hiện nội dung đáp án và bản dịch nếu đã được nhập hoặc gen từ transcript.

Part 3/4:

- Mỗi group gồm nhiều câu.
  - Cần audio.
  - Có thể có image/graphic nếu đề có biểu đồ, lịch, bảng.
  - Transcript nằm trong `audio.transcript_en` và `audio.transcript_vi`.

Part 5:

- Không cần media.
  - Render câu hỏi và đáp án.

Part 6/7:

- Mỗi group gồm nhiều câu.
  - Cần passage.
  - Passage có thể là ảnh hoặc text.
  - Nếu một passage có nhiều ảnh, render theo `order_index`.

## 16. Lỗi Thường Gặp FE Cần Bắt

Label media sai:

```text
176-180 (1)  sai nếu có space
176-180(1)   đúng
```

Dùng nhầm extension trong label:

```text
131-134.png  sai label
131-134      đúng label
```

Import Excel trước khi upload media:

```text
Có thể import được câu hỏi, nhưng group sẽ bị missing media.
```

Chưa reviewed hết group:

```text
Preview có thể xem được, nhưng publish sẽ fail.
```

Sửa đáp án đúng:

```text
Chỉ cần set đáp án mới is_correct=true, backend tự unset đáp án cũ.
```

## 17. Checklist FE Hoàn Thành

FE được xem là hoàn thành feature khi có đủ các phần:

- Tạo/chọn collection.
  - Tạo test.
  - Upload image/audio lẻ từng file.
  - Tự lấy label từ filename bỏ extension.
  - Import Excel sheet `questions`.
  - Import optional sheet `transcripts`.
  - Xem danh sách question groups.
  - Sửa image/audio/transcript/passage/question/answer theo group.
  - Mark reviewed/needs_review.
  - Render preview content.
  - Gọi preview validation.
  - Publish khi hợp lệ.
  - Hiện lỗi rõ ràng khi backend validate fail.
