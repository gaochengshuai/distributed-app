package com.example.payment.controller;

import com.example.payment.service.WithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

/**
 * 提款控制层
 *
 * */
@RestController
@RequestMapping("/api/loan")
public class WithdrawController {

    @Autowired
    private WithdrawService withdrawService;
    @GetMapping("/draw")
    public Object list(){
//        return withdrawService
        return null;
    }
}
