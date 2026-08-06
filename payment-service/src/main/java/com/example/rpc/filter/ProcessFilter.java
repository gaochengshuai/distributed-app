package com.example.rpc.filter;


/**
 * Filter接口 - 所有Filter必须实现此接口
 */
public interface ProcessFilter<T> {

    /**
     * 处理逻辑
     * @param context 上下文对象
     * @return 执行状态
     */
    ExecuteStatus process(T context) throws Exception;

    /**
     * 获取执行顺序（position）
     */
    int getPosition();

    /**
     * 获取事务组ID
     */
    int getTransactionalGroup();
}
