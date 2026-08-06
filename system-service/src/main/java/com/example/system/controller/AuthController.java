package com.example.system.controller;
import com.example.system.entity.UserEntity;
import com.example.system.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.security.Key;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    @Value("${security.jwt.secret}")
    private String secret;
    @Value("${security.jwt.expireSeconds}")
    private long expireSeconds;

    public AuthController(UserRepository userRepo){ this.userRepo = userRepo; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body){
        String username = body.get("username");
        String password = body.get("password");
        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)){
            return ResponseEntity.status(401).body(Map.of("error","invalid credentials"));
        }
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String jws = Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + expireSeconds*1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return ResponseEntity.ok(Map.of("token", jws));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body){
        String username = body.get("username");
        String password = body.get("password");
        if (userRepo.findByUsername(username).isPresent()) return ResponseEntity.badRequest().body(Map.of("error","exists"));
        UserEntity u = new UserEntity();
        u.setUsername(username); u.setPassword(password); u.setDisplayName(body.getOrDefault("displayName",username)); u.setCreatedAt(LocalDateTime.now());
        userRepo.save(u);
        return ResponseEntity.ok(u);
    }
}
