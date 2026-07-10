package com.example.backend.repository.local;

import com.example.backend.entity.local.LocalProductStatsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

//本地数据库的增删改查接口，继承 JpaRepository 自动获得 save、findAll 等方法
@Repository
public interface LocalProductStatsRecordRepository extends JpaRepository<LocalProductStatsRecord, Long> {

    //查最新一批快照数据：取 recorded_at 最大的那批记录（同一次刷新写入的所有 product）
    //因为每分钟写入时所有 product 的 recorded_at 是同一个时间，
    //取 MAX(recorded_at) 就能拿到最近一次刷新的完整数据
    //对应索引：idx_product_stats_record_recorded_at(recorded_at DESC)
    @Query(value = "SELECT * FROM product_stats_record " +
           "WHERE recorded_at = (SELECT MAX(recorded_at) FROM product_stats_record) " +
           "ORDER BY product ASC",
           nativeQuery = true)
    List<LocalProductStatsRecord> findLatestSnapshot();

    //删除超过7天的历史记录，防止本地数据库无限膨胀
    @Modifying
    @Query(value = "DELETE FROM product_stats_record WHERE recorded_at < NOW() - INTERVAL '7 days'",
           nativeQuery = true)
    int deleteOldRecords();

    //按时间范围查询某个 product 的历史数据
    //对应索引：idx_product_stats_record_product_recorded_at(product, recorded_at DESC)
    @Query(value = "SELECT * FROM product_stats_record " +
           "WHERE product = :product " +
           "AND recorded_at >= :startTime " +
           "ORDER BY recorded_at ASC",
           nativeQuery = true)
    List<LocalProductStatsRecord> findTrendByProductAndTimeRange(
            @Param("product") String product,
            @Param("startTime") java.time.LocalDateTime startTime);
}