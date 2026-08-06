package com.example.report.controller;
import com.example.report.entity.ReportEntity;
import com.example.report.repository.ReportRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportRepository repo;
    public ReportController(ReportRepository repo){this.repo=repo;}
    @PostMapping public ReportEntity create(@RequestBody ReportEntity r){ r.setCreatedAt(LocalDateTime.now()); return repo.save(r); }
    @GetMapping public List<ReportEntity> list(){ return repo.findAll(); }
    @GetMapping("/{id}") public ReportEntity get(@PathVariable Long id){ return repo.findById(id).orElse(null); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id){ repo.deleteById(id); }
}
