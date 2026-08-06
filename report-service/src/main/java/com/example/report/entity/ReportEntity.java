package com.example.report.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class ReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Lob private String content;
    private LocalDateTime createdAt;
    // getters/setters
    public Long getId(){return id;} public void setId(Long i){this.id=i;}
    public String getName(){return name;} public void setName(String n){this.name=n;}
    public String getContent(){return content;} public void setContent(String c){this.content=c;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
}
