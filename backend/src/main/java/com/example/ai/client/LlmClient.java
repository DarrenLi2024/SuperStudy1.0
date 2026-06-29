package com.example.ai.client;

import java.util.function.Consumer;

/**
 * LLM 大模型客户端接口
 * 支持同步生成和流式生成（SSE）
 */
public interface LlmClient {

    /**
     * 同步生成（请求-响应模式）
     */
    LlmResponse generate(LlmRequest request);

    /**
     * 流式生成（SSE模式），通过回调逐token返回
     * @param request 请求参数
     * @param onToken 每收到一个token的回调
     * @return 完整响应
     */
    LlmResponse generateStream(LlmRequest request, Consumer<String> onToken);
}
