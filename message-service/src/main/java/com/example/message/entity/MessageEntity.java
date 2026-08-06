package com.example.message.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String topic;
    @Column(columnDefinition = "TEXT")
    private String payload;
    private LocalDateTime createdAt;

}
