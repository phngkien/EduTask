package com.edutask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> generateChecklist(String taskName, String description) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Gemini API Key trống. Đang sử dụng chế độ tạo Checklist giả lập.");
            return generateMockChecklist(taskName);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            String prompt = "Bạn là trợ lý học tập thông minh. Hãy chia nhỏ công việc sau thành từ 4 đến 7 bước thực hiện cụ thể. " +
                    "Yêu cầu: Trả về kết quả CHỈ dưới dạng một JSON array chứa các chuỗi văn bản tiếng Việt thuần. " +
                    "Không được kèm theo bất kỳ thẻ markdown, không bọc trong ```json hay ```, không có văn bản giải thích nào khác ngoài chuỗi JSON đó. " +
                    "Ví dụ kết quả mong muốn: [\"Chuẩn bị tài liệu tham khảo\", \"Viết đề cương chi tiết\", \"Hoàn thành bản thảo\"] " +
                    "Thông tin công việc cần phân rã:\n" +
                    "- Tên công việc: " + taskName + "\n" +
                    "- Mô tả: " + (description != null ? description : "Không có mô tả");

            // Build request body
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", List.of(part));

            Map<String, Object> content = new HashMap<>();
            content.put("contents", List.of(parts));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(content, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse response
                Map body = response.getBody();
                List candidates = (List) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map contentObj = (Map) candidate.get("content");
                    if (contentObj != null) {
                        List partsList = (List) contentObj.get("parts");
                        if (partsList != null && !partsList.isEmpty()) {
                            Map partObj = (Map) partsList.get(0);
                            String text = (String) partObj.get("text");
                            if (text != null) {
                                return parseJsonArray(text);
                            }
                        }
                    }
                }
            }

            log.warn("Không nhận được phản hồi hợp lệ từ Gemini API. Fallback sang tạo checklist giả lập.");
            return generateMockChecklist(taskName);

        } catch (Exception e) {
            log.error("Lỗi khi kết nối với Gemini API: {}. Đang fallback sang tạo checklist giả lập.", e.getMessage());
            return generateMockChecklist(taskName);
        }
    }

    private List<String> parseJsonArray(String text) {
        try {
            String cleaned = text.trim();
            // Clean markdown block
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("(?s)^```(?:json)?\\s*", "")
                                 .replaceAll("(?s)\\s*```$", "");
            }
            cleaned = cleaned.trim();
            
            String[] list = objectMapper.readValue(cleaned, String[].class);
            return Arrays.asList(list);
        } catch (Exception e) {
            log.error("Không thể phân tích kết quả JSON từ Gemini: {}. Nội dung gốc: {}", e.getMessage(), text);
            // Fallback manually if it's text lines instead of JSON
            List<String> list = new ArrayList<>();
            String[] lines = text.split("\n");
            for (String line : lines) {
                String cleanLine = line.replaceAll("^[\\-\\d\\.\\*\\s]+", "").trim();
                if (!cleanLine.isEmpty() && cleanLine.length() < 100) {
                    list.add(cleanLine);
                }
            }
            return list.isEmpty() ? generateMockChecklist("") : list;
        }
    }

    private List<String> generateMockChecklist(String taskName) {
        String name = taskName != null ? taskName.toLowerCase() : "";
        List<String> list = new ArrayList<>();

        if (name.contains("viết") || name.contains("báo cáo") || name.contains("slide") || name.contains("thuyết trình") || name.contains("write") || name.contains("report")) {
            list.add("Thu thập tài liệu tham khảo và phác thảo dàn ý sơ bộ");
            list.add("Viết nội dung chương 1 & chương 2 (hoặc phần mở đầu)");
            list.add("Hoàn thành toàn bộ nội dung chi tiết và kết luận");
            list.add("Thiết kế slide trình chiếu PowerPoint chuyên nghiệp");
            list.add("Kiểm tra định dạng, sửa lỗi chính tả và lưu file PDF cuối cùng");
        } else if (name.contains("code") || name.contains("lập trình") || name.contains("thiết kế") || name.contains("design") || name.contains("chức năng") || name.contains("api")) {
            list.add("Phân tích luồng nghiệp vụ và thiết kế cơ sở dữ liệu (ERD)");
            list.add("Khởi tạo cấu trúc dự án và kết nối Database");
            list.add("Xây dựng APIs Backend xử lý logic nghiệp vụ");
            list.add("Thiết kế giao diện UI Frontend và kết nối dữ liệu");
            list.add("Kiểm thử lỗi (Unit Test/Manual Test) và sửa các bug phát sinh");
        } else if (name.contains("học") || name.contains("ôn tập") || name.contains("thi") || name.contains("đọc") || name.contains("read") || name.contains("study")) {
            list.add("Đọc lại các bài giảng trên lớp và tổng hợp kiến thức trọng tâm");
            list.add("Giải các bài tập mẫu và đề thi của các năm trước");
            list.add("Gặp nhóm thảo luận để giải đáp các phần chưa hiểu");
            list.add("Lập mindmap (sơ đồ tư duy) tóm tắt các công thức/khái niệm");
            list.add("Tự làm bài test thử dưới áp lực thời gian");
        } else {
            list.add("Nghiên cứu tài liệu và làm rõ mục tiêu của công việc");
            list.add("Lập kế hoạch phân bổ thời gian thực hiện chi tiết");
            list.add("Tiến hành triển khai nội dung cốt lõi");
            list.add("Rà soát, kiểm tra chất lượng kết quả cùng với nhóm");
            list.add("Hoàn thiện sản phẩm cuối cùng và nộp báo cáo");
        }
        return list;
    }

    public List<Map<String, Object>> getAiAssignmentSuggestions(
            String taskName, String taskDesc, String priority, String category,
            List<Map<String, Object>> candidates) {
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Gemini API Key trống. Không thể thực hiện phân công bằng AI.");
            return null;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            String candidatesJson = objectMapper.writeValueAsString(candidates);

            String prompt = "Bạn là một AI chuyên phân chia công việc trong nhóm học tập thông minh. " +
                    "Nhiệm vụ của bạn là đánh giá độ phù hợp của các thành viên trong nhóm đối với một công việc cụ thể và đưa ra điểm số cùng lý do đề xuất.\n\n" +
                    "Thông tin công việc:\n" +
                    "- Tên công việc: " + taskName + "\n" +
                    "- Mô tả: " + (taskDesc != null ? taskDesc : "Không có mô tả") + "\n" +
                    "- Mức độ ưu tiên: " + (priority != null ? priority : "MEDIUM") + "\n" +
                    "- Danh mục: " + (category != null ? category : "Không có") + "\n\n" +
                    "Danh sách thành viên cần đánh giá:\n" +
                    candidatesJson + "\n\n" +
                    "Yêu cầu đánh giá từng thành viên theo thang điểm 100:\n" +
                    "1. Điểm kỹ năng (skillScore, tối đa 40 điểm): Đánh giá sự trùng khớp giữa kỹ năng thành viên và yêu cầu công việc.\n" +
                    "2. Điểm tải việc (workloadScore, tối đa 30 điểm): Thành viên có ít công việc đang chạy hơn (so với số công việc tối đa) sẽ có điểm cao hơn.\n" +
                    "3. Điểm ưu tiên (priorityScore, tối đa 20 điểm): Sự ưu tiên và khẩn cấp của công việc.\n" +
                    "4. Điểm sẵn sàng (availabilityScore, tối đa 10 điểm): Điểm mặc định hoặc tự động đánh giá mức độ sẵn sàng.\n" +
                    "5. Tổng điểm (totalScore) = skillScore + workloadScore + priorityScore + availabilityScore.\n" +
                    "6. Lý do đề xuất (reason): Trích dẫn cụ thể ngắn gọn bằng tiếng Việt (1-2 câu) giải thích lý do điểm số đó (ví dụ: \"Có kỹ năng Java phù hợp với API, hiện tại ít task...\").\n\n" +
                    "Yêu cầu trả về kết quả CHỈ dưới dạng một JSON array chứa các đối tượng có cấu trúc như ví dụ sau, không kèm bất kỳ ký tự nào khác ngoài JSON, không có ```json hay ```:\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"userId\": 1,\n" +
                    "    \"totalScore\": 85.5,\n" +
                    "    \"skillScore\": 35.0,\n" +
                    "    \"workloadScore\": 25.5,\n" +
                    "    \"priorityScore\": 15.0,\n" +
                    "    \"availabilityScore\": 10.0,\n" +
                    "    \"reason\": \"Nguyễn Văn A có kỹ năng phù hợp...\"\n" +
                    "  }\n" +
                    "]";

            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", List.of(part));

            Map<String, Object> content = new HashMap<>();
            content.put("contents", List.of(parts));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(content, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List candidatesList = (List) body.get("candidates");
                if (candidatesList != null && !candidatesList.isEmpty()) {
                    Map candidateObj = (Map) candidatesList.get(0);
                    Map contentObj = (Map) candidateObj.get("content");
                    if (contentObj != null) {
                        List partsList = (List) contentObj.get("parts");
                        if (partsList != null && !partsList.isEmpty()) {
                            Map partObj = (Map) partsList.get(0);
                            String text = (String) partObj.get("text");
                            if (text != null) {
                                return parseAssignmentJson(text);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối với Gemini API để phân công: {}", e.getMessage());
        }
        return null;
    }

    private List<Map<String, Object>> parseAssignmentJson(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("(?s)^```(?:json)?\\s*", "")
                                 .replaceAll("(?s)\\s*```$", "");
            }
            cleaned = cleaned.trim();
            return objectMapper.readValue(cleaned, List.class);
        } catch (Exception e) {
            log.error("Không thể phân tích kết quả JSON phân công từ Gemini: {}. Nội dung gốc: {}", e.getMessage(), text);
            return null;
        }
    }
}
