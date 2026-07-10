package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//向前端传的趋势数据结构：哪个product，一组时间点+对应的SN数量
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTrendResponse {
    //产品名称
    private String product;
    //每个时间点的数据，格式如 [{time: "14:00", count: 50}, {time: "14:01", count: 52}]
    private List<TrendPoint> trendData;

    //一个时间点的数据：时间 + SN数量
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String time;
        private Long count;
    }
}