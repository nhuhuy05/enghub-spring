
## 1. Tên Sheet

Tên sheet phải đúng chính xác:

```text
transcripts
```

Nếu file Excel không có sheet này thì backend vẫn import câu hỏi bình thường, chỉ bỏ qua transcript.

## Header Bắt Buộc

Dòng đầu tiên của sheet phải có đúng các cột:

```text
part
group_order
transcript_en
transcript_vi
```

## 3. Ý Nghĩa Các Cột

| Cột | Bắt buộc | Quy định |
| --- | --- | --- |
| `part` | Có | Chỉ dùng Part `1`, `2`, `3`, `4` |
| `group_order` | Có | Phải trùng với `group_order` trong sheet `questions` |
| `transcript_en` | Có nếu `transcript_vi` trống | Transcript tiếng Anh |
| `transcript_vi` | Có nếu `transcript_en` trống | Bản dịch tiếng Việt của transcript |

Mỗi dòng trong sheet `transcripts` tương ứng với một `question_group`, không phải một câu hỏi.

Rule match:

```text
questions.part + questions.group_order
= transcripts.part + transcripts.group_order
```


## Quy Tắc Nội Dung

`transcript_en` nên giữ nguyên speaker nếu có:

```text
M:
W:
Man:
Woman:
Speaker 1:
Speaker 2:
```

Có thể để nhiều câu trong một cell.



