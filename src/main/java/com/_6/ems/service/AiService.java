package com._6.ems.service;

import com._6.ems.dto.response.AiReviewResponse;
import com._6.ems.dto.response.GuideTaskResponse;
import com._6.ems.entity.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {
    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public AiService(ChatClient.Builder builder, ChatModel chatModel) {
        chatClient = builder.build();
        this.chatModel = chatModel;
    }

    public AiReviewResponse getAiReviewTask(MultipartFile file, Task task) {
        try {
            String contentType = file.getContentType();
            MimeType mimeType = contentType != null
                    ? MimeTypeUtils.parseMimeType(contentType)
                    : MimeTypeUtils.APPLICATION_OCTET_STREAM;

            Media media = Media.builder()
                    .mimeType(mimeType)
                    .data(file.getResource())
                    .build();

            ChatOptions chatOptions = ChatOptions.builder()
                    .temperature(0.3D)
                    .build();

            String taskContent = """
                # Thông tin nhiệm vụ
                Tiêu đề: %s
                Mô tả: %s
                """.formatted(task.getTitle(), task.getDescription());

            String aiRawResponse = chatClient.prompt()
                    .options(chatOptions)
                    .system("""
                    Bạn là chuyên gia đánh giá chất lượng công việc của nhân viên.
                    
                    Nhiệm vụ của bạn:
                    1. Phân tích kỹ nội dung file được nộp (có thể là tài liệu, hình ảnh, code, v.v.)
                    2. So sánh với yêu cầu của Task (tiêu đề và mô tả)
                    3. Đánh giá theo các tiêu chí:
                       - Độ hoàn thiện (có đáp ứng đầy đủ yêu cầu không?)
                       - Chất lượng nội dung (chính xác, rõ ràng, chuyên nghiệp)
                       - Tính khả thi và thực tiễn
                       - Format và trình bày
                    
                    Trả về kết quả ĐÚNG định dạng JSON sau (không có markdown, không có text thừa):
                    {
                      "review": "Nhận xét chi tiết về bài nộp, nêu rõ ưu điểm và nhược điểm",
                      "score": 85,
                      "strengths": ["Điểm mạnh 1", "Điểm mạnh 2"],
                      "improvements": ["Gợi ý cải thiện 1", "Gợi ý cải thiện 2"]
                    }
                    
                    Điểm số từ 0-100:
                    - 90-100: Xuất sắc, vượt mong đợi
                    - 80-89: Tốt, đáp ứng đầy đủ yêu cầu
                    - 70-79: Khá, đáp ứng cơ bản
                    - 60-69: Trung bình, còn thiếu sót
                    - 0-59: Cần cải thiện nhiều
                    
                    CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT NÀO KHÁC.
                    Và trả về bằng tiếng anh.
                    """)
                    .user(promptUserSpec -> promptUserSpec
                            .media(media)
                            .text(taskContent))
                    .call()
                    .content();

            if (aiRawResponse == null || aiRawResponse.trim().isEmpty()) {
                throw new RuntimeException("AI không trả về response");
            }

            String cleanedResponse = aiRawResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(cleanedResponse);

            if (node == null || !node.has("review") || !node.has("score")) {
                throw new RuntimeException("AI response thiếu field bắt buộc (review hoặc score)");
            }

            JsonNode reviewNode = node.get("review");
            String review = (reviewNode != null && !reviewNode.isNull())
                    ? reviewNode.asText()
                    : "Không có nhận xét";

            if (review.trim().isEmpty()) {
                review = "AI không cung cấp nhận xét chi tiết";
            }

            JsonNode scoreNode = node.get("score");
            int score = 0;
            if (scoreNode != null && !scoreNode.isNull()) {
                score = scoreNode.asInt();
                if (score < 0 || score > 100) {
                    throw new RuntimeException("Điểm số không hợp lệ: " + score + " (phải từ 0-100)");
                }
            } else {
                throw new RuntimeException("AI không trả về điểm số");
            }

            List<String> strengths = new ArrayList<>();
            if (node.has("strengths") && node.get("strengths").isArray()) {
                node.get("strengths").forEach(s -> {
                    if (s != null && !s.isNull() && !s.asText().trim().isEmpty()) {
                        strengths.add(s.asText());
                    }
                });
            }

            List<String> improvements = new ArrayList<>();
            if (node.has("improvements") && node.get("improvements").isArray()) {
                node.get("improvements").forEach(i -> {
                    if (i != null && !i.isNull() && !i.asText().trim().isEmpty()) {
                        improvements.add(i.asText());
                    }
                });
            }

            return AiReviewResponse.builder()
                    .review(review)
                    .score(score)
                    .strengths(strengths)
                    .improvements(improvements)
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi parse JSON từ AI" , e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi không xác định khi gọi AI: " + e.getMessage(), e);
        }
    }


    public GuideTaskResponse getAiGuideForTask(Task task) {
        try {
            ChatOptions chatOptions = ChatOptions.builder()
                    .temperature(0.4D)
                    .build();

            String taskContent = """
                    # Task Information
                    Title: %s
                    Description: %s
                    """.formatted(task.getTitle(), task.getDescription());

            String aiRawResponse = chatClient.prompt()
                    .options(chatOptions)
                    .system("""
                            You are an expert who gives clear, step-by-step guidance for completing tasks.
                            
                            Your mission:
                            1. Read the task title and description carefully.
                            2. Understand the goal, requirements, and expected outcomes.
                            3. Provide a practical execution plan for the user.
                            4. Break down the instructions into clear steps, with details and reasoning.
                            5. Identify any prerequisite knowledge, tools, risks, or important notes.
                            
                            Response format MUST be valid JSON (no markdown, no extra text):
                            {
                              "guide": "A detailed explanation and step-by-step instructions.",
                              "steps": ["Step 1", "Step 2", "Step 3"],
                              "notes": ["Important note 1", "Important note 2"]
                            }
                            
                            Only return JSON. Respond in English.
                            """)
                    .user(taskContent)
                    .call()
                    .content();

            if (aiRawResponse == null || aiRawResponse.trim().isEmpty()) {
                throw new RuntimeException("AI returned empty response");
            }

            // Clean markdown
            String cleaned = aiRawResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(cleaned);

            String guide = node.has("guide") ? node.get("guide").asText() : "No guide provided.";
            List<String> steps = new ArrayList<>();
            List<String> notes = new ArrayList<>();

            if (node.has("steps") && node.get("steps").isArray()) {
                node.get("steps").forEach(s -> steps.add(s.asText()));
            }

            if (node.has("notes") && node.get("notes").isArray()) {
                node.get("notes").forEach(n -> notes.add(n.asText()));
            }

            return GuideTaskResponse.builder()
                    .guide(guide)
                    .steps(steps)
                    .notes(notes)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error generating AI guide for task: " + e.getMessage(), e);
        }
    }
}
