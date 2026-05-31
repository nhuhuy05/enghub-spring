# FE Test Attempt Guide

Base URL local:

```text
http://localhost:8080/enghub
```

Các endpoint làm bài cần Bearer token, trừ catalog published tests.

```http
Authorization: Bearer <jwt>
Content-Type: application/json
```

## 1. Browse Published Tests

Public endpoints, không cần token.

### List collections có published tests

```http
GET /test-collections
```

Response:

```json
{
  "code": 1000,
  "result": [
    {
      "id": 1,
      "name": "ETS 2024",
      "description": "Official practice tests",
      "created_at": "2026-05-31T10:00:00"
    }
  ]
}
```

### List tests

```http
GET /tests
GET /tests?collectionId=1
GET /test-collections/1/tests
```

Response item:

```json
{
  "id": 10,
  "collection_id": 1,
  "collection_name": "ETS 2024",
  "test_number": 1,
  "title": "ETS Test 1",
  "description": "Full TOEIC mock test",
  "total_questions": 200,
  "duration_minutes": 120,
  "created_at": "2026-05-31T10:00:00"
}
```

### Test detail

```http
GET /tests/{testId}
```

Chỉ trả test đã publish. Test chưa publish trả lỗi `1009`.

## 2. Start Attempt

```http
POST /attempts
```

Body:

```json
{
  "testId": 10,
  "mode": "MOCK",
  "part_numbers": [1, 2, 3, 4, 5, 6, 7]
}
```

Rules:

- `mode` optional, default backend là `MOCK`.
- `mode` hợp lệ: `MOCK`, `PRACTICE`.
- `part_numbers` optional. Nếu bỏ trống, backend hiểu là full part 1-7.
- `POST /attempts` luôn tạo attempt mới. Không dùng endpoint này để resume bài cũ.
- Test chưa publish không start được.

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 123,
    "testId": 10,
    "mode": "MOCK",
    "status": "IN_PROGRESS",
    "correctCount": 0,
    "listeningCorrect": 0,
    "readingCorrect": 0,
    "answeredCount": 0,
    "totalQuestions": 200,
    "totalScore": null,
    "readingScore": null,
    "listeningScore": null,
    "durationSeconds": null,
    "startedAt": "2026-05-31T19:00:00",
    "submittedAt": null,
    "expiresAt": "2026-05-31T21:00:00",
    "remainingSeconds": 7195,
    "part_numbers": [1, 2, 3, 4, 5, 6, 7]
  }
}
```

FE nên dùng `expiresAt` hoặc `remainingSeconds` làm nguồn countdown. Không tự tin vào timer client để quyết định còn được nộp hay không, vì backend sẽ tự submit khi quá giờ.

Resume bài cũ là luồng riêng: gọi `GET /attempts?status=IN_PROGRESS`, cho user chọn attempt, rồi mở `GET /attempts/{attemptId}/content`.

## 3. Attempt History

```http
GET /attempts
GET /attempts?status=IN_PROGRESS
GET /attempts?status=SUBMITTED
GET /attempts?testId=10&page=0&size=20
```

Response là Spring Page:

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": 123,
        "test_id": 10,
        "test_title": "ETS Test 1",
        "mode": "MOCK",
        "status": "IN_PROGRESS",
        "correct_count": 0,
        "listening_correct": 0,
        "reading_correct": 0,
        "answered_count": 20,
        "total_questions": 200,
        "total_score": null,
        "reading_score": null,
        "listening_score": null,
        "duration_seconds": null,
        "started_at": "2026-05-31T19:00:00",
        "submitted_at": null,
        "expires_at": "2026-05-31T21:00:00",
        "remaining_seconds": 3600,
        "part_numbers": [1, 2, 3, 4, 5, 6, 7]
      }
    ],
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

FE use cases:

- Resume banner: query `GET /attempts?status=IN_PROGRESS`.
- History screen: query `GET /attempts?status=SUBMITTED`.
- Test detail page: query `GET /attempts?testId={testId}`.

## 4. Get Attempt Content

```http
GET /attempts/{attemptId}/content
```

Response shape:

```json
{
  "code": 1000,
  "result": {
    "attempt": {
      "id": 123,
      "testId": 10,
      "mode": "MOCK",
      "status": "IN_PROGRESS",
      "answeredCount": 1,
      "totalQuestions": 2,
      "expiresAt": "2026-05-31T21:00:00",
      "remainingSeconds": 3590,
      "part_numbers": [1]
    },
    "test_id": 10,
    "title": "ETS Test 1",
    "description": "Full TOEIC mock test",
    "duration_minutes": 120,
    "parts": [
      {
        "part_number": 1,
        "title": "Photographs",
        "groups": [
          {
            "id": 501,
            "group_order": 1,
            "images": [
              {
                "id": 7001,
                "label": "p01-q001",
                "url": "https://...",
                "order_index": 0
              }
            ],
            "audio": {
              "id": 8001,
              "label": "audio_main",
              "url": "https://...",
              "start_ms": 0,
              "end_ms": 12000
            },
            "passages": [],
            "questions": [
              {
                "id": 9001,
                "question_number": 1,
                "question_text_en": "",
                "question_text_vi": null,
                "selected_answer_id": 10001,
                "answers": [
                  {
                    "id": 10001,
                    "label": "A",
                    "answer_text_en": "",
                    "answer_text_vi": null
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

FE rendering notes:

- Render by `parts -> groups -> questions`.
- Part 1 may have image and audio.
- Part 2 may have audio but no question text/options can be short.
- Part 3/4 may have audio, transcript only appears in result, not content.
- Part 6/7 may have passages with text or image URL.
- Use `selected_answer_id` to hydrate saved answers after refresh/resume.
- If `attempt.status` comes back `SUBMITTED`, backend auto-submitted due to timeout; route user to result.

## 5. Save Or Clear Answer

```http
POST /attempts/{attemptId}/answers
```

Save:

```json
{
  "questionId": 9001,
  "selectedAnswerId": 10001
}
```

Clear:

```json
{
  "questionId": 9001,
  "selectedAnswerId": null
}
```

MOCK response:

```json
{
  "code": 1000,
  "result": {
    "attemptId": 123,
    "questionId": 9001,
    "selectedAnswerId": 10001,
    "correct": null,
    "correctAnswerId": null,
    "explanationVi": null,
    "answeredAt": "2026-05-31T19:10:00"
  }
}
```

PRACTICE response reveals feedback immediately:

```json
{
  "code": 1000,
  "result": {
    "attemptId": 123,
    "questionId": 9001,
    "selectedAnswerId": 10001,
    "correct": true,
    "correctAnswerId": 10001,
    "explanationVi": "Dap an A dung vi...",
    "transcript_en": "A. The man is reading a document.",
    "transcript_vi": "A. Nguoi dan ong dang doc tai lieu.",
    "answeredAt": "2026-05-31T19:10:00"
  }
}
```

For `PRACTICE` attempts, `transcript_en` and `transcript_vi` are included after answering questions in Part 1-4 when transcript data exists. Part 5-7 answers do not include transcript fields.

FE behavior:

- Optimistic update is okay, but rollback on non-`1000`.
- If backend returns `1013 ATTEMPT_INVALID_STATE`, attempt is no longer editable, usually submitted or timed out.
- If backend returns `1010 QUESTION_NOT_EXISTED`, question may not belong to selected parts.

## 6. Submit Attempt

```http
POST /attempts/{attemptId}/submit
```

Response:

```json
{
  "code": 1000,
  "result": {
    "id": 123,
    "testId": 10,
    "mode": "MOCK",
    "status": "SUBMITTED",
    "correctCount": 150,
    "listeningCorrect": 76,
    "readingCorrect": 74,
    "answeredCount": 200,
    "totalQuestions": 200,
    "totalScore": 740,
    "readingScore": 365,
    "listeningScore": 375,
    "durationSeconds": 5110,
    "startedAt": "2026-05-31T19:00:00",
    "submittedAt": "2026-05-31T20:25:10",
    "expiresAt": "2026-05-31T21:00:00",
    "remainingSeconds": 0,
    "part_numbers": [1, 2, 3, 4, 5, 6, 7]
  }
}
```

Scoring notes:

- `correctCount`: total raw correct answers.
- `listeningCorrect`: raw correct in Part 1-4.
- `readingCorrect`: raw correct in Part 5-7.
- `listeningScore`, `readingScore`, `totalScore`: TOEIC-scaled approximation from backend.
- For selected-part attempts, backend normalizes the section score by the number of selected questions in that section.

After submit, route to:

```text
/attempts/{attemptId}/result
```

## 7. Result And Review

```http
GET /attempts/{attemptId}/result
```

Rules:

- `MOCK`: only available after submit.
- `PRACTICE`: result can reveal answered questions while in progress.
- If a mock attempt timed out, this endpoint can auto-submit and then return result.

Response shape:

```json
{
  "code": 1000,
  "result": {
    "attempt": {
      "id": 123,
      "status": "SUBMITTED",
      "correctCount": 150,
      "totalScore": 740
    },
    "parts": [
      {
        "part_number": 5,
        "title": "Incomplete Sentences",
        "groups": [
          {
            "id": 601,
            "group_order": 1,
            "images": [],
            "audio": null,
            "passages": [],
            "transcriptEn": null,
            "transcriptVi": null,
            "questions": [
              {
                "id": 9001,
                "question_number": 101,
                "question_text_en": "The meeting ...",
                "question_text_vi": "Cuoc hop ...",
                "selected_answer_id": 10002,
                "correct_answer_id": 10001,
                "correct": false,
                "explanation_vi": "Dap an A dung vi...",
                "answers": [
                  {
                    "id": 10001,
                    "label": "A",
                    "answer_text_en": "will start",
                    "answer_text_vi": "se bat dau",
                    "is_correct": true
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

FE review UI should show:

- selected answer
- correct answer
- correct/incorrect state
- explanation
- transcript for listening groups when available
- passages/images/audio as context

## 8. Timer Handling

Recommended FE timer logic:

1. On start/content/history response, store `attempt.expiresAt`.
2. Render countdown from `expiresAt - clientNow`.
3. When countdown reaches 0, call `GET /attempts/{id}` or `POST /attempts/{id}/submit`.
4. If response status is `SUBMITTED`, route to result.

Important backend behavior:

- Backend auto-submits expired `IN_PROGRESS` attempts on attempt APIs.
- User cannot save answer after backend marks attempt submitted.
- `remainingSeconds` is advisory for UI; `expiresAt` is better for stable countdown.

## 9. Common Error Codes

```text
1006 UNAUTHENTICATED: missing/invalid token
1007 UNAUTHORIZED: no permission
1009 TEST_NOT_EXISTED: test not found or not published
1010 QUESTION_NOT_EXISTED: question not found or outside selected parts
1011 ANSWER_NOT_EXISTED: selected answer not found
1012 ATTEMPT_NOT_EXISTED: attempt not found or not owned by user
1013 ATTEMPT_INVALID_STATE: attempt already submitted/abandoned or mock result before submit
1014 ANSWER_NOT_BELONG_TO_QUESTION: selected answer belongs to another question
```

## 10. Suggested FE Routes

```text
/tests
/tests/:testId
/attempts
/attempts/:attemptId
/attempts/:attemptId/result
```

Suggested screens:

- Test catalog: public list of published tests.
- Test detail: start full mock, start practice by selected parts.
- Attempt runner: fetch content, render parts/groups, save answer, countdown, submit.
- Attempt history: list in-progress and submitted attempts.
- Result review: show score and per-question explanations.

## 11. Minimal API Call Flow

Start full mock:

```text
GET  /tests
POST /attempts
GET  /attempts/{id}/content
POST /attempts/{id}/answers
POST /attempts/{id}/submit
GET  /attempts/{id}/result
```

Resume:

```text
GET /attempts?status=IN_PROGRESS
GET /attempts/{id}/content
```

Practice selected parts:

```text
POST /attempts { "testId": 10, "mode": "PRACTICE", "part_numbers": [5, 6] }
GET  /attempts/{id}/content
POST /attempts/{id}/answers
GET  /attempts/{id}/result
```
