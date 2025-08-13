package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInsight {
    private String summary;                 // tóm tắt
    private String level;                   // "OK" | "WARN" | "CRITICAL"
    private List<String> warnings;          // cảnh báo bullet
    private List<String> root_causes;       // nguyên nhân khả dĩ

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Strategy {
        private String title;
        private String impact;              // high | medium | low
        private Integer eta_days;           // số ngày triển khai
        private List<String> actions;       // các bước hành động
    }
    private List<Strategy> strategies;
    private List<String> next_actions;      // việc cần làm ngay
}
