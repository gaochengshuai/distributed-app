package com.example.rpc.filter;

/**
 * Filter执行状态
 */
public enum ExecuteStatus {

    /** 继续执行下一个Filter */
    CONTINUE,

    /** 中断执行链，直接返回 */
    TERMINATE
}
