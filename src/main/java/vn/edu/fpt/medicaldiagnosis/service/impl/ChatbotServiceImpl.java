package vn.edu.fpt.medicaldiagnosis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.ChatRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AiInsight;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyRevenuePoint;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyRevenueSeriesResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DashboardOverviewResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.ChatHistory;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;
import vn.edu.fpt.medicaldiagnosis.repository.ChatHistoryRepository;
import vn.edu.fpt.medicaldiagnosis.service.ChatbotService;
import vn.edu.fpt.medicaldiagnosis.service.DashboardService;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ChatbotServiceImpl implements ChatbotService {
    ChatHistoryRepository chatHistoryRepository;
    ChatClient customChatClient;
    AccountRepository accountRepository;
    DashboardService dashboardService;
    ObjectMapper objectMapper;
    InvoiceService invoiceService;
    SettingService settingService;
    @Override
    public String askQuestion(ChatRequest request, String userName) {
        // Lấy Account từ userId
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(userName)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Không tìm thấy tài khoản với tên: " + userName));

        DashboardOverviewResponse dashboardOverviewResponse = dashboardService.getDashboardOverview();
        String jsonDashboard;
        try {
            jsonDashboard = objectMapper.writeValueAsString(dashboardOverviewResponse);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi chuyển dashboard thành JSON.");
        }
        String message = """
                Dưới đây là dữ liệu thống kê bệnh viện ở dạng JSON:

                %s

                Dựa vào dữ liệu trên, hãy trả lời câu hỏi sau bằng tiếng Việt ngắn gọn, dễ hiểu:

                %s
                """.formatted(jsonDashboard, request.getQuestion());

        String answer = customChatClient
                .prompt()
                .system("Bạn là trợ lý AI phân tích thống kê bệnh viện, luôn trả lời chính xác và ngắn gọn bằng tiếng Việt.")
                .user(message)
                .call()
                .content();

        chatHistoryRepository.save(ChatHistory.builder()
                .account(account)
                .question(request.getQuestion())
                .answer(answer)
                .createdAt(LocalDateTime.now())
                .build());

        return answer;
    }

    @Override
    public List<ChatHistory> getHistoryByUsername(String username) {
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Tài khoản không tồn tại"));

        List<ChatHistory> entities = chatHistoryRepository.findByAccountOrderByCreatedAtDesc(account);

        return entities.stream()
                .map(history -> ChatHistory.builder()
                        .id(history.getId())
                        .question(history.getQuestion())
                        .answer(history.getAnswer())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public AiInsight analyzeDailyRevenueEod(LocalDate date) {
        // Guard: không phân tích ngày tương lai
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        if (date.isAfter(today)) {
            return AiInsight.builder()
                    .summary("Chưa có dữ liệu cho ngày tương lai: " + date)
                    .level("OK")
                    .warnings(List.of("Ngày yêu cầu vượt quá ngày hiện tại"))
                    .root_causes(Collections.emptyList())
                    .strategies(Collections.emptyList())
                    .next_actions(Collections.emptyList())
                    .build();
        }

        // 1) Lấy series cho tháng chứa 'date' và point của ngày đó
        YearMonth ym = YearMonth.from(date);
        DailyRevenueSeriesResponse series = invoiceService.getDailySeries(ym);

        DailyRevenuePoint dayPoint = series.getData().stream()
                .filter(p -> p.getDate().equals(date))
                .findFirst()
                .orElseGet(() -> DailyRevenuePoint.builder()
                        .date(date).revenue(BigDecimal.ZERO).expected(BigDecimal.ZERO)
                        .diffPct(BigDecimal.ZERO).level("OK").direction("FLAT").reason("No data")
                        .build());

        // --- MoM-day: hôm nay vs hôm qua ---
        BigDecimal yesterdayRev = series.getData().stream()
                .filter(p -> p.getDate().equals(date.minusDays(1)))
                .map(DailyRevenuePoint::getRevenue)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        Double momDayPct = (yesterdayRev == null || yesterdayRev.signum() == 0)
                ? null
                : dayPoint.getRevenue().subtract(yesterdayRev)
                .divide(yesterdayRev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        // 2) Payload đưa cho AI
        String hospitalName = settingService.getSetting().getHospitalName();

        Map<String, Object> payload = Map.of(
                "tenant", hospitalName,
                "date", date.toString(),
                "daily", Map.of(
                        "actual", num(dayPoint.getRevenue()),
                        "expected", num(dayPoint.getExpected()),
                        "diffPctVsExpected", toDouble(dayPoint.getDiffPct()), // so baseline
                        "directionVsExpected", dayPoint.getDirection(),
                        "momDayPct", momDayPct                                // so hôm qua
                ),
                "monthToDate", Map.of(
                        "actual", num(series.getTotalToDate()),
                        "expected", num(series.getExpectedToDate()),
                        "diffPct", toDouble(series.getDiffPctToDate()),
                        "level", series.getLevelToDate()
                ),
                "last3DaysActual", last3Days(series.getData(), date).stream().map(this::num).toList(),
                "context", "Không có chiến dịch mới; theo dõi slot bác sĩ; lưu ý mùa vụ."
        );

        String system = """
        Bạn là chuyên gia tài chính bệnh viện.
        Nhiệm vụ: phân tích doanh thu của NGÀY (ưu tiên), đối chiếu baseline/ngày và so với HÔM QUA (MoM-day),
        sau đó tổng quan MTD. Chẩn đoán nguyên nhân và đề xuất 3–5 chiến lược (impact, eta_days, actions).
        Không bịa số; chỉ suy luận từ payload. Trả về JSON:
        {
          "summary": string,
          "level": "OK"|"WARN"|"CRITICAL",
          "warnings": string[],
          "root_causes": string[],
          "strategies": [
            {"title": string, "impact":"high|medium|low", "eta_days": number, "actions": string[]}
          ],
          "next_actions": string[]
        }
    """;

        // 3) Gọi model → yêu cầu JSON; nếu client hỗ trợ, bật response_format JSON
        String aiRaw = customChatClient
                .prompt()
                .system(system)
                .user(safeWriteJson(payload))
                // .responseFormat("json_object")
                .call()
                .content();

        // 4) Parse JSON → AiInsight (fallback nếu lỗi)
        try {
            String cleaned = normalizeJsonFromAi(aiRaw); // <-- dùng helper mới
            return objectMapper.readValue(cleaned, AiInsight.class);
        } catch (Exception ex) {
            log.warn("EOD AI JSON parse failed. raw={}", aiRaw, ex);
            return AiInsight.builder()
                    .summary("AI không khả dụng hoặc JSON sai định dạng.")
                    .level("WARN")
                    .warnings(List.of("AI unavailable / invalid JSON"))
                    .root_causes(Collections.emptyList())
                    .strategies(Collections.emptyList())
                    .next_actions(Collections.emptyList())
                    .build();
        }
    }


    private Number num(BigDecimal v) {
        return v == null ? null : new BigDecimal(v.toPlainString());
    }
    private Double toDouble(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }
    private List<BigDecimal> last3Days(List<DailyRevenuePoint> data, LocalDate base) {
        List<BigDecimal> out = new ArrayList<>();
        for (int i = 3; i >= 1; i--) {
            LocalDate d = base.minusDays(i);
            data.stream().filter(p -> p.getDate().equals(d)).findFirst()
                    .ifPresentOrElse(p -> out.add(p.getRevenue()), () -> out.add(BigDecimal.ZERO));
        }
        return out;
    }
    private String safeWriteJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{\"error\":\"serialize\"}"; }
    }

    private String normalizeJsonFromAi(String s) {
        if (s == null) return "{}";
        String t = s.trim();

        // 1) Nếu có code fence: ```json\n{...}\n``` hoặc ```\n{...}\n```
        if (t.startsWith("```")) {
            // Bỏ dòng đầu (``` hoặc ```json)
            int firstNewLine = t.indexOf('\n');
            if (firstNewLine > 0) {
                t = t.substring(firstNewLine + 1);
            } else {
                // không có newline — bỏ luôn 3 backticks đầu
                t = t.substring(3);
            }
            // Bỏ ``` ở cuối nếu có
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
            t = t.trim();
        }

        // 2) Cắt đúng khối JSON: từ ký tự '{' đầu tiên đến '}' cuối cùng
        int start = t.indexOf('{');
        int end   = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            t = t.substring(start, end + 1);
        }

        return t.trim();
    }
}
