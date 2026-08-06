package com.example.rpc;

import com.example.rpc.filter.ExecuteStatus;
import com.example.rpc.filter.ProcessFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.lang.reflect.Constructor;

/**
 * 通用RPC处理器 - Filter链执行引擎
 */
public class AbstractProcessor {

    // ========== 配置属性（对应XML中的property） ==========

    /** 上下文类类型（用于反射创建Context实例） */
    private Class<?> contextClassType;

    /** 请求类类型（用于JSON/XML反序列化） */
    private Class<?> requestClassType;

    /** 响应类类型（用于JSON/XML序列化） */
    private Class<?> responseClassType;

    /** 默认Filter链（从父Bean继承） */
    private List<ProcessFilter> processFilters = new ArrayList<>();

    /** 追加Filter链（子Bean配置） */
    private List<ProcessFilter> addProcessFilters = new ArrayList<>();

    // ========== Setter方法（Spring注入必需） ==========

    public void setContextClassType(String className) throws ClassNotFoundException {
        this.contextClassType = Class.forName(className);
    }

    public void setRequestClassType(String className) throws ClassNotFoundException {
        this.requestClassType = Class.forName(className);
    }

    public void setResponseClassType(String className) throws ClassNotFoundException {
        this.responseClassType = Class.forName(className);
    }

    public void setProcessFilters(List<ProcessFilter> filters) {
        this.processFilters = filters;
    }

    public void setAddProcessFilters(List<ProcessFilter> filters) {
        this.addProcessFilters = filters;
    }

    // ========== 初始化逻辑（Spring容器启动时调用） ==========

    /**
     * 合并Filter链并按position排序
     */
    private List<ProcessFilter> mergedFilters;

    private PlatformTransactionManager transactionManager;

    /**
     * Spring容器初始化后自动调用
     */
    public void afterPropertiesSet() {
        // 1. 合并父Bean和子Bean的Filter
        List<ProcessFilter> allFilters = new ArrayList<>();
        allFilters.addAll(processFilters);
        allFilters.addAll(addProcessFilters);

        // 2. 按position排序
        allFilters.sort(Comparator.comparingInt(ProcessFilter::getPosition));

        // 3. 按transactionalGroup分组
        this.mergedFilters = allFilters;
    }

    // ========== 核心执行逻辑 ==========

    /**
     * HTTP入口方法（由Controller调用）
     */
    public void execute(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // 1. 解析请求体为ReqInfo对象
        String requestBody = readRequestBody(request);
        Object reqInfo = deserialize(requestBody, requestClassType);

        // 2. 创建上下文对象
        Constructor<?> constructor = contextClassType.getConstructor();
        Object context = constructor.newInstance();

        // 3. 设置请求信息到上下文
        invokeSetter(context, "setReqInfo", reqInfo);

        // 4. 创建响应对象
        Object respInfo = responseClassType.getDeclaredConstructor().newInstance();
        invokeSetter(context, "setRespInfo", respInfo);

        // 5. 执行Filter链
        try {
            ExecuteStatus status = executeFilterChain(context);

            if (status == ExecuteStatus.TERMINATE) {
                // Filter链中断，直接返回
                writeResponse(response, respInfo);
                return;
            }

        } catch (Exception e) {
            // 异常处理
            handleException(e, respInfo);
        }

        // 6. 序列化响应
        writeResponse(response, respInfo);
    }

    /**
     * 执行Filter链（核心逻辑）
     */
    private ExecuteStatus executeFilterChain(Object context) throws Exception {

        // 按transactionalGroup分组执行
        Map<Integer, List<ProcessFilter>> groupMap = groupByTransactionGroup(mergedFilters);

        for (Map.Entry<Integer, List<ProcessFilter>> entry : groupMap.entrySet()) {
            int transactionalGroup = entry.getKey();
            List<ProcessFilter> filters = entry.getValue();

            // 开启新事务（如果transactionalGroup不同）
            TransactionStatus txStatus = null;
            boolean isNewTransaction = isDifferentTransactionGroup(transactionalGroup);

            try {
                if (isNewTransaction) {
                    txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
                }

                // 执行当前组的所有Filter
                for (ProcessFilter filter : filters) {
                    ExecuteStatus status = filter.process(context);

                    if (status == ExecuteStatus.TERMINATE) {
                        // 中断执行
                        if (txStatus != null) {
                            transactionManager.rollback(txStatus);
                        }
                        return ExecuteStatus.TERMINATE;
                    }
                }

                // 提交事务
                if (txStatus != null) {
                    transactionManager.commit(txStatus);
                }

            } catch (Exception e) {
                // 回滚事务
                if (txStatus != null) {
                    transactionManager.rollback(txStatus);
                }
                throw e;
            }
        }

        return ExecuteStatus.CONTINUE;
    }

    // ========== 辅助方法 ==========

    private Map<Integer, List<ProcessFilter>> groupByTransactionGroup(List<ProcessFilter> filters) {
        Map<Integer, List<ProcessFilter>> map = new TreeMap<>();
        for (ProcessFilter filter : filters) {
            int group = filter.getTransactionalGroup();
            map.computeIfAbsent(group, k -> new ArrayList<>()).add(filter);
        }
        return map;
    }

    private boolean isDifferentTransactionGroup(int currentGroup) {
        // 简化实现：每个group都开新事务
        return true;
    }

    private Object deserialize(String json, Class<?> clazz) throws JsonProcessingException {
        // 使用Jackson/Gson等库反序列化
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, clazz);
    }

    private void writeResponse(HttpServletResponse response, Object respInfo)
            throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(respInfo);
        response.getWriter().write(json);
    }

    private void invokeSetter(Object obj, String methodName, Object param)
            throws Exception {
        obj.getClass().getMethod(methodName, param.getClass())
                .invoke(obj, param);
    }

    private String readRequestBody(HttpServletRequest request)
            throws Exception {
        java.io.BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void handleException(Exception e, Object respInfo) throws Exception {
        // 设置错误响应
        invokeSetter(respInfo, "setRespCode", "E_999");
        invokeSetter(respInfo, "setRespDesc", e.getMessage());
    }

//    /**
//     * 开启新事务
//     */
//    private TransactionStatus getTransaction() {
//        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
//        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
//        return transactionManager.getTransaction(def);
//    }
//
//    /**
//     * 提交事务
//     */
//    private void commit(TransactionStatus status) {
//        if (status != null && !status.isCompleted()) {
//            transactionManager.commit(status);
//        }
//    }
//
//    /**
//     * 回滚事务
//     */
//    private void rollback(TransactionStatus status) {
//        if (status != null && !status.isCompleted()) {
//            transactionManager.rollback(status);
//        }
//    }
}
