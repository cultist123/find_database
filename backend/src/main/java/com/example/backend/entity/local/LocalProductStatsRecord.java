package com.example.backend.entity.local;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

//本地数据库表，记录每次刷新统计到的 product 数据
@Entity
@Table(name = "product_stats_record")
@Data
public class LocalProductStatsRecord {

    //自增主键，数据库自动生成
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //产品名称，包括"未分类"
    @Column(name = "product", nullable = false)
    private String product;

    //该 product 去重后的 SN 数量
    @Column(name = "unique_sn_count")
    private Long uniqueSnCount;

    //统计的时间范围起点
    @Column(name = "time_range_start")
    private LocalDateTime timeRangeStart;

    //统计的时间范围终点
    @Column(name = "time_range_end")
    private LocalDateTime timeRangeEnd;

    //写入本地库的时间
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}