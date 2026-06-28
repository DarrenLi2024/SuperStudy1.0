package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 知识点掌握情况
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subject;
    private String name;
    private Integer masteryLevel;
    private String status;
}
