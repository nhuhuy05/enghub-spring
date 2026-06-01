# Huong Dan FE: Reading Bilingual Practice

Tai lieu nay mo ta contract BE de FE tich hop tinh nang Luyen Doc Song Ngu tu TOEIC Part 7.

Tai lieu chi noi ve API, data flow, payload, validation va goi y mapping du lieu. FE tu quyet dinh giao dien.

Base URL local mac dinh:

```text
http://localhost:8080/enghub
```

Quy uoc response chung:

```json
{
  "code": 1000,
  "message": null,
  "result": {}
}
```

## 1. Muc Tieu Tinh Nang

Tinh nang Luyen Doc Song Ngu dung nguon bai doc tu Part 7.

Luon co 2 lop du lieu:

```text
Question Group Part 7 -> Question Group Passages -> Reading Lesson
```

- `question_groups`: group goc trong de thi TOEIC.
- `question_group_passages`: cac passage cua group, gom noi dung EN/VI.
- `reading_lessons`: bai luyen doc duoc tao tu Part 7 va publish cho learner.
- `reading_vocabulary_hints`: tu vung goi y cua lesson.

FE learner chi nen doc tu `/reading-lessons`.

FE admin co the doc Part 7 candidates, tao lesson, chinh passages/vocabulary, goi AI, publish.

## 2. Enum Va Trang Thai

### Reading Type

```ts
export type ReadingLessonType = 'SINGLE' | 'DOUBLE' | 'TRIPLE';
```

Y nghia:

- `SINGLE`: 1 passage.
- `DOUBLE`: 2 passages.
- `TRIPLE`: 3 passages tro len.

BE tu goi y type theo so passage khi tao lesson, nhung admin co the override.

### Status

```ts
export type ReadingLessonStatus = 'DRAFT' | 'PUBLISHED';
```

Y nghia:

- `DRAFT`: admin dang bien tap, learner khong thay.
- `PUBLISHED`: learner thay o public API.

Khi publish, BE validate:

- Lesson phai co title.
- Lesson phai co it nhat 1 passage.
- Moi passage phai co `content_en` va `content_vi`.

## 3. Data Model FE Nen Dung

### Reading Lesson List Item

Dung cho danh sach admin va learner.

```ts
export interface ReadingLessonListItem {
  id: number;
  question_group_id: number;
  test_id: number;
  test_title: string;
  group_order: number;
  title: string;
  title_vi: string | null;
  reading_type: ReadingLessonType;
  status: ReadingLessonStatus;
  difficulty: string | null;
  passage_count: number;
  vocabulary_count: number;
  updated_at: string;
}
```

### Reading Lesson Detail

```ts
export interface ReadingLessonDetail {
  id: number;
  question_group_id: number;
  test_id: number;
  test_title: string;
  group_order: number;
  title: string;
  title_vi: string | null;
  reading_type: ReadingLessonType;
  status: ReadingLessonStatus;
  difficulty: string | null;
  passages: ReadingPassage[];
  vocabulary_hints: ReadingVocabularyHint[];
  created_at: string;
  updated_at: string;
}
```

### Reading Passage

```ts
export interface ReadingPassage {
  id: number;
  question_group_id: number;
  part_number: number;
  group_order: number;
  title: string | null;
  passage_type: string | null;
  content_format: string | null;
  content_en: string | null;
  content_vi: string | null;
  vocab_hints: string | null;
  media_asset_id: number | null;
  media_label: string | null;
  media_url: string | null;
  order_index: number;
}
```

Luu y:

- `part_number` cua lesson nay luon la `7`.
- `content_format` co the la text tu do, vi du `plain_text`, `email`, `notice`; BE khong ep enum.
- `vocab_hints` la field cu tren passage, FE nen uu tien `vocabulary_hints` cap lesson.

### Vocabulary Hint

```ts
export interface ReadingVocabularyHint {
  id: number;
  passage_id: number | null;
  passage_order_index: number | null;
  word: string;
  part_of_speech: string | null;
  meaning_vi: string;
  order_index: number;
}
```

## 4. Learner API

Public GET endpoints, khong can Bearer token.

### 4.1 Lay Danh Sach Lesson Published

```http
GET /reading-lessons
GET /reading-lessons?reading_type=SINGLE
GET /reading-lessons?reading_type=DOUBLE
GET /reading-lessons?reading_type=TRIPLE
```

Response:

```json
{
  "code": 1000,
  "result": [
    {
      "id": 1,
      "question_group_id": 120,
      "test_id": 3,
      "test_title": "TOEIC Practice Test 1",
      "group_order": 8,
      "title": "Company Lunch Party",
      "title_vi": "Tiec trua cua cong ty",
      "reading_type": "SINGLE",
      "status": "PUBLISHED",
      "difficulty": null,
      "passage_count": 1,
      "vocabulary_count": 5,
      "updated_at": "2026-06-01T23:20:00"
    }
  ]
}
```

FE co the dung response nay de hien danh sach bai luyen doc.

### 4.2 Lay Chi Tiet Lesson Published

```http
GET /reading-lessons/{lessonId}
```

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 1,
    "question_group_id": 120,
    "test_id": 3,
    "test_title": "TOEIC Practice Test 1",
    "group_order": 8,
    "title": "Company Lunch Party",
    "title_vi": "Tiec trua cua cong ty",
    "reading_type": "SINGLE",
    "status": "PUBLISHED",
    "difficulty": null,
    "passages": [
      {
        "id": 10,
        "question_group_id": 120,
        "part_number": 7,
        "group_order": 8,
        "title": "Company Lunch Party (E-mail)",
        "passage_type": "E-mail",
        "content_format": "plain_text",
        "content_en": "To: All Staff\nFrom: HR Department\nSubject: Company Lunch Party\n...",
        "content_vi": "Gui: Toan the nhan vien\nTu: Phong Nhan su\nChu de: Tiec trua cua cong ty\n...",
        "vocab_hints": null,
        "media_asset_id": null,
        "media_label": null,
        "media_url": null,
        "order_index": 0
      }
    ],
    "vocabulary_hints": [
      {
        "id": 100,
        "passage_id": 10,
        "passage_order_index": 0,
        "word": "announce",
        "part_of_speech": "verb",
        "meaning_vi": "thong bao",
        "order_index": 0
      }
    ],
    "created_at": "2026-06-01T23:10:00",
    "updated_at": "2026-06-01T23:20:00"
  }
}
```

Behavior:

- Neu lesson khong ton tai hoac chua publish, BE tra `READING_LESSON_NOT_EXISTED`.
- Response chi gom lesson da publish.
- FE khong can goi API Part 7 goc cho learner.

## 5. Admin API

Tat ca admin endpoints can Bearer token co role `ADMIN` hoac `TEACHER`.

Header:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

### 5.1 Lay Part 7 Candidates

Dung de admin chon bai doc tu Part 7.

```http
GET /admin/reading-lessons/part7-candidates
GET /admin/reading-lessons/part7-candidates?test_id=3
```

Response:

```json
{
  "code": 1000,
  "result": [
    {
      "question_group_id": 120,
      "test_id": 3,
      "test_title": "TOEIC Practice Test 1",
      "group_order": 8,
      "question_numbers": [147, 148],
      "passage_count": 1,
      "suggested_reading_type": "SINGLE",
      "existing_lesson_id": null,
      "title": "Company Lunch Party (E-mail)"
    }
  ]
}
```

Field quan trong:

- `question_group_id`: id de tao lesson.
- `passage_count`: so passage hien co trong Part 7 group.
- `suggested_reading_type`: BE goi y theo passage count.
- `existing_lesson_id`: neu da co lesson tu group nay thi FE nen dieu huong qua edit thay vi tao moi.

### 5.2 Lay Danh Sach Admin Lessons

```http
GET /admin/reading-lessons
GET /admin/reading-lessons?status=DRAFT
GET /admin/reading-lessons?status=PUBLISHED&reading_type=DOUBLE
```

Response shape giong learner list, nhung co ca `DRAFT`.

### 5.3 Tao Lesson Tu Part 7 Group

```http
POST /admin/reading-lessons
```

Request:

```json
{
  "question_group_id": 120,
  "title": "Company Lunch Party",
  "title_vi": "Tiec trua cua cong ty",
  "reading_type": "SINGLE",
  "status": "DRAFT",
  "difficulty": "easy"
}
```

Field:

- `question_group_id`: bat buoc, phai thuoc Part 7.
- `title`: optional. Neu khong gui, BE lay title tu passage dau tien, group title, hoac test title.
- `title_vi`: optional. AI dich bai doc se tu dien neu dang trong hoac khi overwrite.
- `reading_type`: optional. Neu khong gui, BE infer tu so passage.
- `status`: optional, mac dinh `DRAFT`.
- `difficulty`: optional, free text.

Response: `ReadingLessonDetail`.

Validation:

- Khong duoc tao lesson trung `question_group_id`.
- Group phai la Part 7.
- Neu tao voi `status = PUBLISHED`, moi passage phai co `content_en` va `content_vi`.

### 5.4 Lay Chi Tiet Admin Lesson

```http
GET /admin/reading-lessons/{lessonId}
```

Response: `ReadingLessonDetail`, bao gom ca `DRAFT`.

### 5.5 Cap Nhat Lesson

```http
PUT /admin/reading-lessons/{lessonId}
PATCH /admin/reading-lessons/{lessonId}
```

Request co the gui mot phan hoac toan bo field:

```json
{
  "title": "Company Lunch Party",
  "title_vi": "Tiec trua cua cong ty",
  "reading_type": "SINGLE",
  "status": "DRAFT",
  "difficulty": "easy",
  "passages": [
    {
      "media_asset_id": null,
      "title": "Company Lunch Party (E-mail)",
      "passage_type": "E-mail",
      "content_format": "plain_text",
      "content_en": "To: All Staff\nFrom: HR Department\n...",
      "content_vi": "Gui: Toan the nhan vien\nTu: Phong Nhan su\n...",
      "order_index": 0
    }
  ],
  "vocabulary_hints": [
    {
      "passage_id": 10,
      "word": "announce",
      "part_of_speech": "verb",
      "meaning_vi": "thong bao",
      "order_index": 0
    }
  ]
}
```

Behavior quan trong:

- Neu gui `passages`, BE replace toan bo passages cua question group.
- Khi replace `passages`, BE cung xoa vocabulary hints cu vi `passage_id` co the thay doi.
- Neu gui `vocabulary_hints`, BE replace toan bo vocabulary hints cua lesson.
- Neu khong gui `passages` hoac `vocabulary_hints`, phan do duoc giu nguyen.
- `passage_id` trong `vocabulary_hints` optional. Neu FE vua replace passages va chua biet id moi, co the dung `passage_order_index`.

Vi du dung `passage_order_index`:

```json
{
  "vocabulary_hints": [
    {
      "passage_order_index": 0,
      "word": "confirm",
      "part_of_speech": "verb",
      "meaning_vi": "xac nhan",
      "order_index": 0
    }
  ]
}
```

Validation:

- `title` neu gui thi khong duoc blank.
- `passages` neu gui thi khong duoc empty.
- `vocabulary_hints.word` va `meaning_vi` bat buoc neu co item.
- `media_asset_id` neu gui phai la image cua cung test.
- Khi status la `PUBLISHED`, moi passage phai co `content_en` va `content_vi`.

### 5.6 Publish/Unpublish

```http
PATCH /admin/reading-lessons/{lessonId}/status
```

Request publish:

```json
{
  "status": "PUBLISHED"
}
```

Request unpublish:

```json
{
  "status": "DRAFT"
}
```

Response: `ReadingLessonDetail`.

Luu y:

- Publish co validation nhu tren.
- Unpublish khong xoa du lieu, chi an khoi public learner API.

### 5.7 AI Dich Bai Doc

```http
POST /admin/reading-lessons/{lessonId}/generate-translation
```

Request optional:

```json
{
  "overwrite_enabled": false
}
```

Behavior:

- BE gui passages hien tai sang Gemini.
- Neu passage co `media_asset_id`, BE gui anh passage do cho Gemini de dung lam nguon/context khi dich.
- Gemini tra `title_vi` cho lesson va `content_en`, `content_vi` theo tung `passage_id`.
- Neu passage chi co anh, Gemini trich xuat noi dung tieng Anh tu anh vao `content_en`, roi dich sang `content_vi`.
- Neu `overwrite_enabled = false`, BE chi dien `title_vi`, `content_en`, `content_vi` dang trong.
- Neu `overwrite_enabled = true`, BE ghi de `title_vi`, `content_en`, `content_vi` hien co.

Response: `ReadingLessonDetail`.

### 5.8 AI Sinh Tu Vung

```http
POST /admin/reading-lessons/{lessonId}/generate-vocabulary
```

Request optional:

```json
{
  "overwrite_enabled": false
}
```

Behavior:

- BE gui passages hien tai sang Gemini.
- Gemini tra 5-12 vocabulary hints.
- Neu `overwrite_enabled = false`, BE them tu moi va bo qua tu da ton tai theo lowercase word.
- Neu `overwrite_enabled = true`, BE xoa vocabulary hints cu va luu list moi.

Response: `ReadingLessonDetail`.

### 5.9 AI Dich + Sinh Tu Vung

```http
POST /admin/reading-lessons/{lessonId}/generate-ai-support
```

Request optional:

```json
{
  "overwrite_enabled": true
}
```

Behavior:

1. Goi generate translation.
2. Goi generate vocabulary.
3. Tra ve `ReadingLessonDetail` moi nhat.

### 5.10 Xoa Lesson

```http
DELETE /admin/reading-lessons/{lessonId}
```

Response:

```json
{
  "code": 1000,
  "result": "Reading lesson has been deleted"
}
```

Behavior:

- Xoa `reading_lessons`.
- `reading_vocabulary_hints` bi xoa cascade.
- Khong xoa `question_group` va `question_group_passages` goc.

## 6. Flow Admin De Xay Trang Luyen Doc

### Flow Tao Lesson Moi

1. FE lay danh sach test bang API hien co neu can.
2. FE goi `GET /admin/reading-lessons/part7-candidates?test_id={testId}`.
3. User chon mot candidate.
4. Neu `existing_lesson_id != null`, dieu huong den detail lesson do.
5. Neu chua co, goi `POST /admin/reading-lessons`.
6. FE goi `GET /admin/reading-lessons/{lessonId}` de hien editor.
7. Admin sua title, reading type, passages, vocabulary hints.
8. FE goi `PUT /admin/reading-lessons/{lessonId}` de luu.
9. Admin bam AI neu can:
   - `generate-translation`
   - `generate-vocabulary`
   - hoac `generate-ai-support`
10. Admin review.
11. FE goi `PATCH /admin/reading-lessons/{lessonId}/status` voi `PUBLISHED`.

### Flow Edit Lesson Da Co

1. FE goi `GET /admin/reading-lessons`.
2. FE chon lesson.
3. FE goi `GET /admin/reading-lessons/{lessonId}`.
4. FE submit thay doi bang `PUT`.
5. Neu dang published va FE gui noi dung khong hop le, BE tra `INVALID_KEY`.

## 7. Flow Learner

1. FE goi `GET /reading-lessons?reading_type=...` de lay list.
2. FE goi `GET /reading-lessons/{lessonId}` de lay detail.
3. FE render passages theo `order_index`.
4. FE render vocabulary theo `order_index`.
5. FE co the cho user bat/tat hien thi `content_vi` va `vocabulary_hints` o client side. BE khong can endpoint rieng cho toggle.

## 8. Mapping Snake Case Sang Camel Case

Neu codebase FE dung camelCase:

```ts
export const mapReadingLesson = (lesson: ReadingLessonDetail) => ({
  id: String(lesson.id),
  questionGroupId: lesson.question_group_id,
  testId: lesson.test_id,
  testTitle: lesson.test_title,
  groupOrder: lesson.group_order,
  title: lesson.title,
  titleVi: lesson.title_vi ?? '',
  readingType: lesson.reading_type,
  status: lesson.status,
  difficulty: lesson.difficulty,
  passages: lesson.passages
    .slice()
    .sort((a, b) => a.order_index - b.order_index)
    .map((passage) => ({
      id: String(passage.id),
      title: passage.title,
      passageType: passage.passage_type,
      contentFormat: passage.content_format,
      contentEn: passage.content_en ?? '',
      contentVi: passage.content_vi ?? '',
      mediaAssetId: passage.media_asset_id,
      mediaLabel: passage.media_label,
      mediaUrl: passage.media_url,
      orderIndex: passage.order_index,
    })),
  vocabularyHints: lesson.vocabulary_hints
    .slice()
    .sort((a, b) => a.order_index - b.order_index)
    .map((hint) => ({
      id: String(hint.id),
      passageId: hint.passage_id,
      passageOrderIndex: hint.passage_order_index,
      word: hint.word,
      partOfSpeech: hint.part_of_speech ?? '',
      meaningVi: hint.meaning_vi,
      orderIndex: hint.order_index,
    })),
});
```

## 9. Error Codes Thuong Gap

- `1001 INVALID_KEY`: payload sai, blank title, group khong phai Part 7, publish khi thieu content, vocabulary thieu word/meaning.
- `1023 QUESTION_GROUP_NOT_EXISTED`: `question_group_id` khong ton tai.
- `1024 MEDIA_ASSET_NOT_EXISTED`: `media_asset_id` khong ton tai, khong cung test, hoac khong phai image.
- `1026 PASSAGE_NOT_EXISTED`: vocabulary hint tham chieu passage id khong thuoc lesson.
- `1029 GEMINI_DISABLED`: Gemini dang tat.
- `1030 GEMINI_API_KEY_MISSING`: chua cau hinh API key.
- `1032 GEMINI_GENERATION_FAILED`: loi goi Gemini.
- `1033 GEMINI_INVALID_RESPONSE`: Gemini tra JSON khong dung shape.
- `1043 READING_LESSON_NOT_EXISTED`: lesson khong ton tai hoac learner dang truy cap lesson chua publish.
- `1044 READING_LESSON_EXISTED`: da co lesson cho `question_group_id`.

## 10. Checklist Tich Hop FE

- Tao client methods cho public:
  - `getReadingLessons(readingType?)`
  - `getReadingLesson(id)`
- Tao client methods cho admin:
  - `getPart7ReadingCandidates(testId?)`
  - `getAdminReadingLessons(filters)`
  - `createReadingLesson(payload)`
  - `getAdminReadingLesson(id)`
  - `updateReadingLesson(id, payload)`
  - `updateReadingLessonStatus(id, status)`
  - `generateReadingTranslation(id, overwriteEnabled)`
  - `generateReadingVocabulary(id, overwriteEnabled)`
  - `generateReadingAiSupport(id, overwriteEnabled)`
  - `deleteReadingLesson(id)`
- Sort `passages` va `vocabulary_hints` theo `order_index` o FE de chac chan dung thu tu.
- Khong goi public `/reading-lessons/{id}` de edit admin vi endpoint do chi tra lesson published.
- Khi replace passages, can reload detail sau khi save de lay `passage_id` moi neu FE muon link vocabulary theo passage id.
- Khi publish fail voi `INVALID_KEY`, FE nen bao admin kiem tra title, content EN/VI cua tung passage.
