# Huong Dan FE: Practice Question AI Chat Streaming

Tai lieu nay mo ta contract BE de FE tich hop tinh nang hoi dap AI theo tung cau trong man hinh lam bai `PRACTICE`.

Base URL local mac dinh:

```text
http://localhost:8080/enghub
```

Endpoint ben duoi can Bearer token.

Header:

```http
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream
```

## 1. Muc Tieu Tinh Nang

Trong `PRACTICE` mode, learner co the hoi AI ve cau dang lam.

BE stream cau tra loi ve FE de UI hien text dan dan, khong phai doi AI tra het moi render.

Rule bao ve dap an:

- Chi hoat dong voi attempt `PRACTICE`.
- Attempt phai la `IN_PROGRESS`.
- Attempt phai thuoc user dang dang nhap.
- Question phai thuoc test va selected parts cua attempt.
- Neu learner chua tra loi cau do, AI chi duoc goi y, khong duoc tiet lo dap an dung.
- Neu learner da tra loi cau do, AI co the giai thich dap an da chon, dap an dung, transcript/passage neu co.

## 2. API Streaming Chat

```http
POST /attempts/{attemptId}/questions/{questionId}/chat/stream
```

Vi du day du:

```http
POST /enghub/attempts/30/questions/41/chat/stream
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream
```

Request:

```json
{
  "message": "Tai sao dap an B sai?",
  "conversation_id": null
}
```

Field:

- `message`: bat buoc, cau hoi cua learner.
- `conversation_id`: optional, hien tai BE chua luu history nen FE co the gui `null` hoac bo qua.

Response la `text/event-stream`, khong boc trong `ApiResponse`.

Event thanh cong:

```text
event: delta
data: {"text":"Vi trong doan hoi thoai, nguoi noi..."}

event: delta
data: {"text":" Dap an B khong khop voi thong tin..."}

event: done
data: {}
```

Event loi:

```text
event: error
data: {"message":"Gemini generation failed"}
```

## 3. FE Khong Nen Dung EventSource

`EventSource` mac dinh chi phu hop `GET` va khong set Bearer token header tot trong browser.

Tinh nang nay can:

- `POST`
- JSON body
- `Authorization: Bearer <token>`

FE nen dung `fetch()` va doc `ReadableStream`.

## 4. TypeScript Types

```ts
export interface PracticeQuestionChatRequest {
  message: string;
  conversation_id?: string | null;
}

export type PracticeQuestionChatEvent =
  | { event: 'delta'; data: { text: string } }
  | { event: 'done'; data: Record<string, never> }
  | { event: 'error'; data: { message: string } };
```

Neu FE dung camelCase, request co the map:

```ts
const payload = {
  message,
  conversation_id: conversationId ?? null,
};
```

## 5. Client Streaming Goi Y

Ham parse SSE don gian:

```ts
type SseHandler = {
  onDelta: (text: string) => void;
  onDone?: () => void;
  onError?: (message: string) => void;
};

const parseSseBlock = (block: string) => {
  const lines = block.split('\n');
  const event = lines
    .find((line) => line.startsWith('event:'))
    ?.slice('event:'.length)
    .trim();
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trim())
    .join('');

  return { event, data };
};

export async function streamPracticeQuestionChat(params: {
  baseUrl: string;
  token: string;
  attemptId: number;
  questionId: number;
  message: string;
  conversationId?: string | null;
  signal?: AbortSignal;
  handlers: SseHandler;
}) {
  const response = await fetch(
    `${params.baseUrl}/attempts/${params.attemptId}/questions/${params.questionId}/chat/stream`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${params.token}`,
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({
        message: params.message,
        conversation_id: params.conversationId ?? null,
      }),
      signal: params.signal,
    }
  );

  if (!response.ok || !response.body) {
    throw new Error(`Chat stream failed: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() ?? '';

    for (const block of blocks) {
      if (!block.trim()) continue;

      const { event, data } = parseSseBlock(block);
      if (event === 'delta') {
        params.handlers.onDelta(JSON.parse(data).text ?? '');
      } else if (event === 'done') {
        params.handlers.onDone?.();
      } else if (event === 'error') {
        params.handlers.onError?.(JSON.parse(data).message ?? 'AI chat failed');
      }
    }
  }
}
```

## 6. UI Flow Goi Y

Man hinh practice dang co cau hoi hien tai.

FE nen them panel chat theo cau:

1. User bam icon `Hoi AI` o cau dang lam.
2. Mo chat panel gan voi `questionId`.
3. User nhap cau hoi.
4. FE tao assistant message rong.
5. Goi endpoint stream.
6. Moi `delta` append vao assistant message hien tai.
7. Khi `done`, unlock input.
8. Neu user chuyen cau, FE co the giu cache theo `questionId`.

State goi y:

```ts
interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  streaming?: boolean;
}

type ChatByQuestionId = Record<number, ChatMessage[]>;
```

## 7. Cancel Stream

FE nen dung `AbortController` de huy stream khi:

- User bam stop.
- User chuyen test/attempt.
- Component unmount.

Vi du:

```ts
const controller = new AbortController();

await streamPracticeQuestionChat({
  baseUrl,
  token,
  attemptId,
  questionId,
  message,
  signal: controller.signal,
  handlers: {
    onDelta: appendText,
    onDone: finishMessage,
    onError: showError,
  },
});

controller.abort();
```

## 8. Khi Nao Hien Nut Hoi AI

FE nen hien nut trong cac truong hop:

- Attempt mode la `PRACTICE`.
- Attempt status la `IN_PROGRESS`.
- Dang co `attemptId` va `questionId`.

FE co the hien AI chat truoc khi learner chon dap an, nhung can noi ro day la "goi y".

Goi y UI:

- Neu chua tra loi: placeholder `Hoi AI de nhan goi y, AI se khong tiet lo dap an dung.`
- Neu da tra loi: placeholder `Hoi AI vi sao dap an dung/sai.`

## 9. Context BE Gui Cho AI

FE khong can gui context cau hoi. BE tu lay context tu DB.

Neu chua tra loi:

- question text EN/VI.
- answer options.
- khong co `is_correct`.
- khong co correct answer.
- khong co explanation.
- khong co transcript/passage context nhay cam.

Neu da tra loi:

- selected answer.
- correct answer.
- explanation VI neu co.
- transcript EN/VI neu la listening Part 1-4 va co audio transcript.
- passages neu la reading Part 6/7 va co passage.

Ly do: khong de FE co co hoi lay dap an dung truoc khi user tra loi.

## 10. Error Codes Thuong Gap

Voi loi validate truoc khi stream, BE co the tra response JSON binh thuong theo `ApiResponse`.

Voi loi xay ra trong stream, BE gui `event: error`.

Loi thuong gap:

- `1001 INVALID_KEY`: message rong hoac payload sai.
- `1005 USER_NOT_EXISTED`: token user khong map duoc user trong DB.
- `1010 QUESTION_NOT_EXISTED`: question khong thuoc attempt/test/selected parts.
- `1012 ATTEMPT_NOT_EXISTED`: attempt khong ton tai hoac khong thuoc user.
- `1013 ATTEMPT_INVALID_STATE`: attempt khong phai `PRACTICE` hoac khong `IN_PROGRESS`.
- `1029 GEMINI_DISABLED`: Gemini dang bi tat.
- `1030 GEMINI_API_KEY_MISSING`: thieu `GEMINI_API_KEY`.
- `1032 GEMINI_GENERATION_FAILED`: loi goi Gemini.
- `1033 GEMINI_INVALID_RESPONSE`: Gemini stream tra data khong dung format BE mong doi.

## 11. Checklist FE

- Them client method `streamPracticeQuestionChat`.
- Dung `fetch` + `ReadableStream`, khong dung `EventSource`.
- Parse SSE theo block cach nhau bang dong trong.
- Append text tu `event: delta` vao assistant message hien tai.
- Disable input khi dang streaming.
- Ho tro cancel bang `AbortController`.
- Chi hien chat AI khi attempt la `PRACTICE` va `IN_PROGRESS`.
- Neu user chua tra loi, hien wording "goi y" thay vi "giai thich dap an".
- Khong gui question context hoac correct answer tu FE.
- Sau khi user save answer, chat lan sau se co quyen giai thich dap an dung/sai.
