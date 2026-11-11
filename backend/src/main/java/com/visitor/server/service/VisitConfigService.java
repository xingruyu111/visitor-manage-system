package com.visitor.server.service;

import java.util.List;

public interface VisitConfigService {

    List<Integer> alumniMonths();

    void updateAlumniMonths(List<Integer> months);
}
