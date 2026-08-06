package com.example.rpc.filter;

/**
 * Filter抽象基类 - 提供默认实现
 */
public abstract class AbstractProcessFilter<T> implements ProcessFilter<T> {

    private int position;
    private int transactionalGroup = 100; // 默认事务组

    // ========== Getter/Setter（Spring注入必需） ==========

    public void setPosition(int position) {
        this.position = position;
    }

    public void setTransactionalGroup(int transactionalGroup) {
        this.transactionalGroup = transactionalGroup;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public int getTransactionalGroup() {
        return transactionalGroup;
    }

    /**
     * 子类只需实现此方法
     */
    @Override
    public abstract ExecuteStatus process(T context) throws Exception;

}