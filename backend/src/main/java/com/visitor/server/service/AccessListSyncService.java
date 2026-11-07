package com.visitor.server.service;

import java.util.Map;

public interface AccessListSyncService {

    Map<String, Object> pull();

    Map<String, Object> push();
}
