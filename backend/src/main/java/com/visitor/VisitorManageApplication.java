package com.visitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智慧校园访客管理系统 - 启动类。
 *
 * <p>接口统一前缀 /api（见 application.yaml 的 context-path）。</p>
 */
@SpringBootApplication
public class VisitorManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisitorManageApplication.class, args);
    }
}
