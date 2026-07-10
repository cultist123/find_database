package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

//向前端传的数据结构就就是一个时间，一个sn统计
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsResponse {
    private String timestamp;
    private Map<String, Long> data;
    private Long totalRecords;
}