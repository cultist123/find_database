package com.example.backend.service;

import com.example.backend.dto.CategoryStatsResponse;
import com.example.backend.dto.HourlyCategoryStats;
import com.example.backend.dto.ProductTrendResponse;
import com.example.backend.entity.local.LocalProductStatsRecord;
import com.example.backend.repository.local.LocalProductStatsRecordRepository;
import com.example.backend.repository.remote.ExhibitionRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ExhibitionStatsService {
    //依赖注入 - 远程数据库查询（只有定时刷新时才用）
    @Autowired
    private ExhibitionRecordRepository exhibitionRecordRepository;
    //依赖注入 - 本地数据库读写（前端请求从这里拿数据）
    @Autowired
    private LocalProductStatsRecordRepository localProductStatsRecordRepository;
    //依赖注入
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    //
    private volatile long lastRefreshTime = 0;
    private volatile boolean isRefreshing = false;

    private static final long CACHE_TTL_MS = 30_000; // 缓存有效期30秒，防止并发刷新

    @PostConstruct
    public void init() {
        log.info("服务启动，预热：从远程拉数据存到本地...");
        refreshStats();
    }

    //从本地数据库读取最新快照，转换成前端需要的格式
    //不再依赖内存缓存，也不阻塞等远程查询
    public CategoryStatsResponse getCategoryStats() {
        List<LocalProductStatsRecord> snapshot = localProductStatsRecordRepository.findLatestSnapshot();
        if (snapshot.isEmpty()) {
            //本地库还没数据（服务刚启动），异步触发一次远程刷新
            CompletableFuture.runAsync(() -> refreshStats());
            return null;
        }

        //把本地库的数据拼成前端格式
        Map<String, Long> productData = new LinkedHashMap<>();
        long totalSnCount = 0;
        for (LocalProductStatsRecord record : snapshot) {
            productData.put(record.getProduct(), record.getUniqueSnCount());
            totalSnCount += record.getUniqueSnCount();
        }

        return new CategoryStatsResponse(
                snapshot.get(0).getRecordedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                productData,
                totalSnCount
        );
    }

    //定时任务：从远程数据库拉数据，存到本地数据库，推送给前端
    //这个方法只负责"抓数据存本地"，不负责响应前端请求
    @Scheduled(cron = "0 * * * * *")//每分钟执行一次
    public void refreshStats() {
        //存在并发或者上次刷新不到30s就不刷新了
        long now = System.currentTimeMillis();
        if (isRefreshing || (now - lastRefreshTime) < CACHE_TTL_MS) {
            log.debug("跳过刷新：正在刷新或缓存未过期");
            return;
        }
        //简易并发锁，先锁门后刷新拉数据
        isRefreshing = true;
        try {
            doRefresh();
        } finally {
            //开门，记录时间
            isRefreshing = false;
            lastRefreshTime = System.currentTimeMillis();
        }
    }

    //每天凌晨3点清理超过7天的历史数据，防止本地库无限膨胀
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldRecords() {
        int deleted = localProductStatsRecordRepository.deleteOldRecords();
        log.info("已清理{}条超过7天的历史记录", deleted);
    }

    //查远程，加工，存本地，推前端
    //ELT方法,读取加载推送
    //读写分离，和缓存本地化的思想
    private synchronized void doRefresh() {
        log.info("从远程数据库拉取product统计数据...");
        //依据开始和结束时间去数据库里找对应的表
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime now = LocalDateTime.now();
        List<HourlyCategoryStats> stats = exhibitionRecordRepository.getProductSnStats(oneHourAgo, now);

        //保持顺序，做成map键值对
        Map<String, Long> productData = new LinkedHashMap<>();
        for (HourlyCategoryStats stat : stats) {
            productData.put(stat.getCategory(), stat.getUniqueSnCount());
        }

        //取出值，转为流式，拆箱为long，求和
        long totalSnCount = productData.values().stream().mapToLong(Long::longValue).sum();

        //存到本地数据库
        saveToLocalDatabase(productData, oneHourAgo, now);

        //拼好数据后通过websocket推送到前端
        CategoryStatsResponse response = new CategoryStatsResponse(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),//时间戳
                productData,//各product统计
                totalSnCount//sn总数
        );
        messagingTemplate.convertAndSend("/topic/stats", response);//转换成json格式并作为相应通过websocket订阅地址发送给前端
        log.info("远程数据已存入本地库并推送到前端，共{}个product", productData.size());
    }

    //从本地数据库读取最近一小时统计，格式和之前一样
    public Map<String, Object> getRecentHourStats() {
        List<LocalProductStatsRecord> snapshot = localProductStatsRecordRepository.findLatestSnapshot();
        if (snapshot.isEmpty()) {
            CompletableFuture.runAsync(() -> refreshStats());
            return null;
        }

        LocalProductStatsRecord firstRecord = snapshot.get(0);
        Map<String, Long> productData = new LinkedHashMap<>();
        long totalSnCount = 0;
        for (LocalProductStatsRecord record : snapshot) {
            productData.put(record.getProduct(), record.getUniqueSnCount());
            totalSnCount += record.getUniqueSnCount();
        }

        LocalDateTime now = LocalDateTime.now();
        String timeRange = firstRecord.getTimeRangeStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " - " +
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.put("timeRange", timeRange);
        response.put("categorySnCount", productData);
        response.put("totalSnCount", totalSnCount);
        return response;
    }

    //手动刷新（强制从远程拉数据存到本地）
    public CategoryStatsResponse forceRefresh() {
        synchronized (this) {
            log.info("手动强制刷新：从远程拉数据...");
            doRefresh();
        }
        return getCategoryStats();
    }

    //把统计结果批量写入本地数据库
    private void saveToLocalDatabase(Map<String, Long> productData, LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) {
        LocalDateTime recordedAt = LocalDateTime.now();
        List<LocalProductStatsRecord> records = new ArrayList<>();

        for (Map.Entry<String, Long> entry : productData.entrySet()) {
            LocalProductStatsRecord record = new LocalProductStatsRecord();
            record.setProduct(entry.getKey());
            record.setUniqueSnCount(entry.getValue());
            record.setTimeRangeStart(timeRangeStart);
            record.setTimeRangeEnd(timeRangeEnd);
            record.setRecordedAt(recordedAt);
            records.add(record);
        }

        localProductStatsRecordRepository.saveAll(records);
        log.info("已写入本地数据库，共{}条product记录", records.size());
    }

    //根据 product 名称和时间范围查询趋势数据，按分钟间隔取点
    public ProductTrendResponse getProductTrend(String product, int hours) {
        //计算查询的起始时间，默认查最近7天
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<LocalProductStatsRecord> records =
                localProductStatsRecordRepository.findTrendByProductAndTimeRange(product, startTime);

        List<ProductTrendResponse.TrendPoint> trendData = new ArrayList<>();
        LocalDateTime lastRecordedAt = null;

        for (LocalProductStatsRecord record : records) {
            // 每15分钟取一个点，兼顾数据密度和可读性
            if (lastRecordedAt == null ||
                    java.time.Duration.between(lastRecordedAt, record.getRecordedAt()).toMinutes() >= 15) {
                trendData.add(new ProductTrendResponse.TrendPoint(
                        record.getRecordedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        record.getUniqueSnCount()
                ));
                lastRecordedAt = record.getRecordedAt();
            }
        }

        return new ProductTrendResponse(product, trendData);
    }
}