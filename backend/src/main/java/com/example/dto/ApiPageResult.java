package com.example.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApiPageResult<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;

    public ApiPageResult() {}

    public ApiPageResult(List<T> list, Long total, Integer page, Integer size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
