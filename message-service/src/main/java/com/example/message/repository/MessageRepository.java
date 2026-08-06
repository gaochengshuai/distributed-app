package com.example.message.repository;
import com.example.message.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MessageRepository extends JpaRepository<MessageEntity,Long> {}
