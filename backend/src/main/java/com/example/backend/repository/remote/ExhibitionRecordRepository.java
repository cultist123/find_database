package com.example.backend.repository.remote;

import com.example.backend.dto.HourlyCategoryStats;
import com.example.backend.entity.remote.ExhibitionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

//SQL语句进行数据库查询逻辑
@Repository
//继承JpaRepository类，传入数据库字段参数，声明接口继续增删改查
public interface ExhibitionRecordRepository extends JpaRepository<ExhibitionRecord, Long> {

    //从data_body里面取出product字段，空值标记为"未分类"，统计去重sn字段，分组并排序
    @Query(value = "SELECT " +
           "COALESCE(data_body->>'product', '未分类') as category, " +//从json里面取值
           "COUNT(DISTINCT sn) as uniqueSnCount " +//去重统计
           "FROM ts_device_data_format " +
           "WHERE time > :startTime " +
           "AND time < :endTime " +
           "GROUP BY COALESCE(data_body->>'product', '未分类') " +//空值处理
           "ORDER BY COALESCE(data_body->>'product', '未分类')",
           nativeQuery = true)
    List<HourlyCategoryStats> getProductSnStats(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

}