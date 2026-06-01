# Huong Dan FE: Listening Dictation

Tai lieu nay mo ta contract BE de FE tich hop tinh nang Nghe Chep.

Base URL local mac dinh:

```text
http://localhost:8080/enghub
```

Tat ca endpoint ben duoi can Bearer token, tru khi security config duoc mo public rieng.

## 1. Data Model Chinh

Nghe Chep dung cau truc:

```text
Test -> Part 1-4 -> Question Group -> Transcript Lines -> Sentence
```

Luu y:

- `question_group` la don vi audio chinh.
- Part 1/2 thuong moi group la 1 cau hoi.
- Part 3/4 moi group thuong gom nhieu cau hoi.
- FE khong nen gan transcript line truc tiep vao `question_id`; nen gan theo `question_group_id`.

## 2. Student API Lay Session Nghe Chep

```http
GET /listening/tests/{testId}/parts/{partNumber}/dictation
```

Dieu kien:

- `testId` phai la test da publish.
- `partNumber` chi nhan `1`, `2`, `3`, `4`.
- BE uu tien lay `question_group_transcript_lines`.
- Neu group chua co transcript lines, BE fallback bang cach tach cau tu `question_group_audios.transcript_en/transcript_vi`.
- Group khong co audio/transcript se bi bo qua trong response.

### Response Mau

```json
{
  "code": 1000,
  "result": {
    "test_id": 1,
    "part_id": "part-1",
    "part_number": 1,
    "title": "TEST 1 2026",
    "part_name": "Part 1",
    "instruction": "Part 1: listen to each sentence and practice dictation.",
    "groups": [
      {
        "id": 10,
        "title": "Cau 1",
        "group_order": 1,
        "question_numbers": [1],
        "sentences": [
          {
            "id": "10-0",
            "speaker": "W",
            "text": "The woman is carrying a tray of food.",
            "translation": "Nguoi phu nu dang bung mot khay thuc an.",
            "audio_url": "https://example.com/audio.mp3",
            "start_ms": 1200,
            "end_ms": 4300,
            "order_index": 0,
            "completed": false,
            "hint_levels": [30, 50, 100]
          }
        ]
      }
    ]
  }
}
```

### TypeScript Goi Y

```ts
export interface ListeningDictationSession {
  test_id: number;
  part_id: string;
  part_number: number;
  title: string;
  part_name: string;
  instruction: string;
  groups: ListeningDictationGroup[];
}

export interface ListeningDictationGroup {
  id: number;
  title: string;
  group_order: number;
  question_numbers: number[];
  sentences: ListeningDictationSentence[];
}

export interface ListeningDictationSentence {
  id: string;
  speaker: string | null;
  text: string;
  translation: string | null;
  audio_url: string;
  start_ms: number | null;
  end_ms: number | null;
  order_index: number;
  completed: boolean;
  hint_levels: Array<30 | 50 | 100>;
}
```

Neu FE dang dung type camelCase hien tai, map nhu sau:

```ts
const mapSession = (data: ListeningDictationSession): ListeningSession => ({
  testId: String(data.test_id),
  partId: data.part_id,
  title: data.title,
  partName: data.part_name,
  instruction: data.instruction,
  audioUrl: data.groups[0]?.sentences[0]?.audio_url ?? '',
  duration: 0,
  vocabulary: [],
  groups: data.groups.map((group) => ({
    id: String(group.id),
    title: group.title,
    sentences: group.sentences.map((sentence) => ({
      id: sentence.id,
      text: sentence.text,
      translation: sentence.translation ?? '',
      audioUrl: sentence.audio_url,
      startMs: sentence.start_ms,
      endMs: sentence.end_ms,
      completed: sentence.completed,
      hintLevels: sentence.hint_levels,
    })),
  })),
});
```

## 3. Phat Audio Theo Sentence

Moi sentence tra ve:

- `audio_url`: file audio group.
- `start_ms`, `end_ms`: doan can phat.

FE nen:

1. Load `audio_url`.
2. Khi bam play sentence, set `audio.currentTime = start_ms / 1000`.
3. Neu `end_ms` co gia tri, dung timer hoac `timeupdate` de pause khi `currentTime >= end_ms / 1000`.
4. Neu `start_ms/end_ms` null, phat ca file/group audio.

## 4. Logic An Tu Tren FE

BE khong cham bai nghe chep. FE tu xu ly:

- `30`: an khoang 30% tu trong cau.
- `50`: an khoang 50% tu trong cau.
- `100`: an toan bo cau, user chep full sentence.

Khuyen nghi:

- Normalize answer ve lowercase.
- Bo dau cau khi so sanh.
- Giu apostrophe trong cac tu nhu `don't`, `I'm`.
- Voi `30/50`, moi hidden word la mot input.
- Voi `100`, dung textarea va so sanh theo thu tu word.

## 5. Admin API: Chon Test -> Part -> Group

Lay group theo test va part:

```http
GET /admin/tests/{testId}/parts/{partNumber}/question-groups
```

Response moi item:

```json
{
  "id": 10,
  "part_number": 1,
  "group_order": 1,
  "question_numbers": [1],
  "review_status": "needs_review",
  "missing_flags": [],
  "has_audio": true,
  "audio_url": "https://example.com/audio.mp3",
  "transcript_line_count": 4,
  "has_transcript_lines": true
}
```

FE admin nen dung endpoint nay de render sidebar theo part.

## 6. Admin API: Lay Chi Tiet Group

```http
GET /admin/question-groups/{groupId}
```

Phan quan trong trong response:

```json
{
  "result": {
    "id": 10,
    "part_number": 1,
    "group_order": 1,
    "audio": {
      "id": 20,
      "media_asset_id": 30,
      "label": "Audio 1",
      "url": "https://example.com/audio.mp3",
      "start_ms": 0,
      "end_ms": 10000,
      "transcript_en": "The woman is carrying a tray of food.",
      "transcript_vi": "Nguoi phu nu dang bung mot khay thuc an.",
      "transcript_lines": [
        {
          "id": 100,
          "speaker": "W",
          "text_en": "The woman is carrying a tray of food.",
          "text_vi": "Nguoi phu nu dang bung mot khay thuc an.",
          "start_ms": 1200,
          "end_ms": 4300,
          "order_index": 0
        }
      ]
    }
  }
}
```

## 7. Admin API: Cap Nhat Transcript Lines

```http
PUT /admin/question-groups/{groupId}/transcript-lines
Content-Type: application/json
```

Request:

```json
{
  "lines": [
    {
      "id": 100,
      "speaker": "W",
      "text_en": "The woman is carrying a tray of food.",
      "text_vi": "Nguoi phu nu dang bung mot khay thuc an.",
      "start_ms": 1200,
      "end_ms": 4300,
      "order_index": 0
    },
    {
      "speaker": "M",
      "text_en": "The man is standing near the counter.",
      "text_vi": "Nguoi dan ong dang dung gan quay.",
      "start_ms": 4400,
      "end_ms": 7000,
      "order_index": 1
    }
  ]
}
```

Behavior:

- BE replace toan bo transcript lines cu cua group audio.
- `id` trong request chi de FE giu context; BE khong update tung row theo id.
- Sau khi update, group se thanh `review_status = needs_review`.

Validation:

- Chi Part `1-4`.
- Group phai co audio.
- `lines` khong duoc null.
- `text_en` bat buoc.
- `order_index >= 0`.
- Khong duoc trung `order_index`.
- `start_ms >= 0`, `end_ms >= 0`.
- Neu co ca `start_ms` va `end_ms` thi `end_ms > start_ms`.
- `speaker` toi da 100 ky tu.

## 8. UI Admin Goi Y

Man hinh admin nen co:

- Select test.
- Tabs/segmented control Part 1-4.
- Sidebar group:
  - title
  - question numbers
  - audio status
  - transcript line count
- Main panel:
  - audio player
  - table/list transcript lines
  - fields: speaker, text_en, text_vi, start_ms, end_ms, order_index
  - add line, duplicate line, delete line, reorder line
  - save button goi `PUT /transcript-lines`

## 9. Loi Thuong Gap

- `1009 TEST_NOT_EXISTED`: test khong ton tai hoac chua publish voi student API.
- `1001 INVALID_KEY`: part ngoai 1-4, duplicate order, time range sai, thieu text_en.
- `1034 AI_AUDIO_NOT_EXISTED`: admin update transcript lines nhung group chua co audio.

## 10. Checklist FE

- Thay mock `listeningService.getSession` bang API `/listening/tests/{testId}/parts/{partNumber}/dictation`.
- Map snake_case sang camelCase neu UI dang dung camelCase.
- Audio player ho tro `start_ms/end_ms`.
- Sidebar hien group va sentences theo response moi.
- Admin page dung `GET /admin/tests/{testId}/parts/{partNumber}/question-groups`.
- Admin editor dung `GET /admin/question-groups/{groupId}` va `PUT /admin/question-groups/{groupId}/transcript-lines`.
