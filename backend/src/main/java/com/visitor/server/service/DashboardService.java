package com.visitor.server.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> summary();

    Map<String, Object> trend(int days);
}
