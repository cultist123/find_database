package com.example.backend.controller;

import com.example.backend.dto.CategoryStatsResponse;
import com.example.backend.dto.ProductTrendResponse;
import com.example.backend.service.ExhibitionStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

//前端接口就是前端能调用的api地址，每个接口对应一个功能
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private ExhibitionStatsService exhibitionStatsService;

    //获取product的sn统计
    //前端寻路通过get获取数据
    @GetMapping("/category")
    //定义一个公共函数用的是ResponseEntity的CategoryStatsResponse返回类型
    public ResponseEntity<CategoryStatsResponse> getCategoryStats() {
        //getCategoryStats()的意思是获取exhibitionStatsService的getCategoryStats方法后处理的返回值
        //，经过 ResponseEntity
        //类的ok函数加个200状态码处理后，返回给CategoryStatsResponse参数
        return ResponseEntity.ok(exhibitionStatsService.getCategoryStats());
    }

    //手动触发刷新
    @PostMapping("/refresh")
    public ResponseEntity<CategoryStatsResponse> refreshStats() {
        return ResponseEntity.ok(exhibitionStatsService.forceRefresh());
    }

    //获取近一个小时统计
    @GetMapping("/recent-hour")
    public ResponseEntity<Map<String, Object>> getRecentHourStats() {
        Map<String, Object> response = exhibitionStatsService.getRecentHourStats();
        return ResponseEntity.ok(response);
    }

    //健康检查
    @GetMapping("/health")
    //返回一个ResponseEntity<Map<String, String>>类型的数据，通过health（）方法处理后
    public ResponseEntity<Map<String, String>> health() {
        //创建一个新的名字是response的哈希表，key和value类型都是string
        Map<String, String> response = new HashMap<>();
        //放入key和value
        response.put("status", "UP");
        response.put("message", "服务正常运行");
        //将响应体打上响应头并且返回
        return ResponseEntity.ok(response);
    }

    //获取某个product的趋势数据，支持按时间范围查询
    @GetMapping("/trend")
    public ResponseEntity<ProductTrendResponse> getProductTrend(@RequestParam String product,
                                                                 @RequestParam(defaultValue = "168") int hours) {
        ProductTrendResponse trend = exhibitionStatsService.getProductTrend(product, hours);
        return ResponseEntity.ok(trend);
    }
}