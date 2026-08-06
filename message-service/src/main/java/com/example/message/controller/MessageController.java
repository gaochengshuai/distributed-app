package com.example.message.controller;
import com.example.message.entity.MessageEntity;
import com.example.message.repository.MessageRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageRepository repo;
    public MessageController(MessageRepository repo){this.repo=repo;}
    @PostMapping
    public MessageEntity create(@RequestBody MessageEntity m){ m.setCreatedAt(LocalDateTime.now()); return repo.save(m); }
    @GetMapping
    public List<MessageEntity> list(){ return repo.findAll(); }
    @GetMapping("/{id}")
    public MessageEntity get(@PathVariable Long id){ return repo.findById(id).orElse(null); }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){ repo.deleteById(id); }
}
