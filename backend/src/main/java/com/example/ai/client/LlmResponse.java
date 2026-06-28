package com.example.ai.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;
    private String provider;
    private String model;
    private boolean cached;
    private boolean fallback;
}
