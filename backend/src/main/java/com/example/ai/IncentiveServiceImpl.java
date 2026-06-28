package com.example.ai;

import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncentiveServiceImpl implements IncentiveService {

    private final LlmClient llmClient;
    private String style = "克制、具体、鼓励行动";

    @Override
    public String generateUpgradeIncentive(String previousBatch, String currentBatch, int scoreAtUpgrade) {
        return generateOrFallback(
                "段位从" + previousBatch + "升级到" + currentBatch + "，当前等效分" + scoreAtUpgrade,
                "你已经完成从" + previousBatch + "到" + currentBatch + "的跨越。下一步不要松劲，把优势题型稳定住。"
        );
    }

    @Override
    public String generateProgressIncentive(int currentScore, int previousScore) {
        int delta = currentScore - previousScore;
        if (delta <= 0) {
            return generateStagnationEncouragement(currentScore, 14);
        }
        return generateOrFallback(
                "当前分" + currentScore + "，较上次提升" + delta + "分",
                "这次提升了" + delta + "分，说明方法开始起效。继续复盘提分来源，把它变成稳定能力。"
        );
    }

    @Override
    public String generateDreamCollegeIncentive(int currentScore, int targetScore, int gap) {
        return generateOrFallback(
                "当前分" + currentScore + "，目标分" + targetScore + "，差距" + gap + "分",
                gap <= 0 ? "你已经达到目标分线，接下来要把波动压低，稳定保持。"
                        : "距离目标还有" + gap + "分，优先处理最薄弱学科，每周只盯一个明确增量。"
        );
    }

    @Override
    public String generateStagnationEncouragement(int currentScore, int daysSinceImprovement) {
        return generateOrFallback(
                "当前分" + currentScore + "，已" + daysSinceImprovement + "天未明显提升",
                "短期停滞不等于无效。先把错题按知识点归类，找到一个最小突破口。"
        );
    }

    @Override
    public String generateDailyComment(double completionRate, int taskCount) {
        return generateOrFallback(
                "今日任务" + taskCount + "项，完成率" + completionRate + "%",
                completionRate >= 80 ? "今日完成度不错，明天保持同样节奏，并复盘一处错题。"
                        : "今日任务还没完全吃透，先补齐未完成项，不急着增加新内容。"
        );
    }

    @Override
    public void setIncentiveStyle(String style) {
        if (style != null && !style.trim().isEmpty()) {
            this.style = style.trim();
        }
    }

    private String generateOrFallback(String context, String fallback) {
        try {
            String content = llmClient.generate(LlmRequest.builder()
                    .taskType("incentive")
                    .systemPrompt("你是高中升学陪伴系统的激励文案助手。风格：" + style + "。不要承诺必然提分。")
                    .userPrompt(context + "\n生成一句80字以内的中文激励文案。")
                    .temperature(0.4)
                    .build()).getContent();
            if (content != null && content.trim().length() > 2 && !"{}".equals(content.trim())) {
                return limit(content.trim(), 120);
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
