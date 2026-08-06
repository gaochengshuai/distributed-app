package com.example.report.repository;
import com.example.report.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReportRepository extends JpaRepository<ReportEntity,Long> {}
