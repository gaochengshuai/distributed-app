package com.example.payment.controller;
import com.example.payment.entity.Payment;
import com.example.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    Logger logger = LoggerFactory.getLogger(PaymentController.class);
    Map<String,String> result = new HashMap<>();
    private final PaymentRepository repo;
    private final RabbitTemplate rabbit;
    public PaymentController(PaymentRepository repo, RabbitTemplate rabbit){this.repo=repo;this.rabbit=rabbit;}

    @PostMapping
    public Object create(@RequestBody Payment p){

        p.setCreatedAt(LocalDateTime.now());
        p.setStatus("CREATED");
        Payment s = repo.save(p);
        rabbit.convertAndSend("payments.exchange","payments.created",s.getId());
        return s;
    }

    @GetMapping
    public List<Payment> list(){ return repo.findAllByOrderByCreatedAtDesc(); }

    @GetMapping("/{id}")
    public Payment get(@PathVariable Long id){ return repo.findById(id).orElse(null); }

    @PutMapping("/{id}")
    public Payment update(@PathVariable Long id, @RequestBody Payment p){
        return repo.findById(id).map(ex->{ ex.setAmount(p.getAmount()); ex.setOrderNo(p.getOrderNo()); ex.setStatus(p.getStatus()); return repo.save(ex); }).orElse(null);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Long id){ repo.deleteById(id); }
}
