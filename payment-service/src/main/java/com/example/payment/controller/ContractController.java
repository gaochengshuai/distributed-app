package com.example.payment.controller;

import com.example.payment.entity.ClsContract;
import com.example.payment.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同管理控制器
 * 提供合同的增删改查等REST API接口
 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    /**
     * 创建新合同
     * POST /api/contracts
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String contrNo = (String) request.get("contrNo");
            String custId = (String) request.get("custId");
            String productId = (String) request.get("productId");
            BigDecimal signAmt = new BigDecimal(request.get("signAmt").toString());

            // 参数校验
            if (contrNo == null || contrNo.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "合同号不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            if (custId == null || custId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "客户ID不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            if (signAmt == null || signAmt.compareTo(BigDecimal.ZERO) <= 0) {
                result.put("success", false);
                result.put("message", "签署金额必须大于0");
                return ResponseEntity.badRequest().body(result);
            }

            ClsContract contract = contractService.createContract(contrNo, custId, productId, signAmt);
            
            result.put("success", true);
            result.put("message", "合同创建成功");
            result.put("data", contract);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 根据合同号查询合同
     * GET /api/contracts/{contrNo}
     */
    @GetMapping("/{contrNo}")
    public ResponseEntity<Map<String, Object>> getContract(@PathVariable String contrNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ClsContract contract = contractService.getContractByNo(contrNo);
            
            if (contract == null) {
                result.put("success", false);
                result.put("message", "合同不存在");
                return ResponseEntity.notFound().build();
            }
            
            result.put("success", true);
            result.put("data", contract);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 根据客户ID查询所有合同
     * GET /api/contracts/customer/{custId}
     */
    @GetMapping("/customer/{custId}")
    public ResponseEntity<Map<String, Object>> getContractsByCustomer(@PathVariable String custId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ClsContract> contracts = contractService.getContractsByCustId(custId);
            
            result.put("success", true);
            result.put("data", contracts);
            result.put("count", contracts.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询客户合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 更新合同状态
     * PUT /api/contracts/{contrNo}/status
     */
    @PutMapping("/{contrNo}/status")
    public ResponseEntity<Map<String, Object>> updateContractStatus(
            @PathVariable String contrNo, 
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String newStatus = request.get("status");
            
            if (newStatus == null || newStatus.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "状态不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            ClsContract contract = contractService.updateContractStatus(contrNo, newStatus);
            
            result.put("success", true);
            result.put("message", "合同状态更新成功");
            result.put("data", contract);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新合同状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 激活合同
     * PUT /api/contracts/{contrNo}/activate
     */
    @PutMapping("/{contrNo}/activate")
    public ResponseEntity<Map<String, Object>> activateContract(@PathVariable String contrNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ClsContract contract = contractService.activateContract(contrNo);
            
            result.put("success", true);
            result.put("message", "合同已激活");
            result.put("data", contract);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "激活合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 关闭合同
     * PUT /api/contracts/{contrNo}/close
     */
    @PutMapping("/{contrNo}/close")
    public ResponseEntity<Map<String, Object>> closeContract(@PathVariable String contrNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ClsContract contract = contractService.closeContract(contrNo);
            
            result.put("success", true);
            result.put("message", "合同已关闭");
            result.put("data", contract);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "关闭合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 删除合同
     * DELETE /api/contracts/{contrNo}
     */
    @DeleteMapping("/{contrNo}")
    public ResponseEntity<Map<String, Object>> deleteContract(@PathVariable String contrNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            contractService.deleteContract(contrNo);
            
            result.put("success", true);
            result.put("message", "合同删除成功");
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取所有合同
     * GET /api/contracts
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllContracts() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ClsContract> contracts = contractService.getAllContracts();
            
            result.put("success", true);
            result.put("data", contracts);
            result.put("count", contracts.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询所有合同失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 检查合同是否存在
     * GET /api/contracts/{contrNo}/exists
     */
    @GetMapping("/{contrNo}/exists")
    public ResponseEntity<Map<String, Object>> checkContractExists(@PathVariable String contrNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean exists = contractService.existsContract(contrNo);
            
            result.put("success", true);
            result.put("exists", exists);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查合同存在性失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
