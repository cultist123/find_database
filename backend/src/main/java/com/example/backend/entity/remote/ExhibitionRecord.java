package com.example.backend.entity.remote;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

//把数据库的表映射成 Java 代码里的"类"，方便用代码操作数据库。
@Entity
@Table(name = "ts_device_data_format")
@Data
public class ExhibitionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time")
    private LocalDateTime time;

    @Column(name = "a_time")
    private LocalDateTime aTime;

    @Column(name = "c_time")
    private LocalDateTime cTime;

    @Column(name = "sn")
    private String sn;

    @Column(name = "gate")
    private String gate;

    @Column(name = "topic")
    private String topic;

    @Column(name = "data_body", columnDefinition = "json")
    private String dataBody;

}