package com.example.system.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique=true)
    private String username;
    private String password;
    private String displayName;
    private LocalDateTime createdAt;
    // getters/setters
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getUsername(){return username;} public void setUsername(String u){this.username=u;}
    public String getPassword(){return password;} public void setPassword(String p){this.password=p;}
    public String getDisplayName(){return displayName;} public void setDisplayName(String d){this.displayName=d;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
}
