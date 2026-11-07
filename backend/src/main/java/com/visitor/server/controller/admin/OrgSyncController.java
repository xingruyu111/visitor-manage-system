package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.security.SchoolScopes;
import com.visitor.server.service.OrgSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "组织同步")
@RestController
@RequestMapping("/web/admin/org-sync")
public class OrgSyncController {

    private final OrgSyncService orgSyncService;

    public OrgSyncController(OrgSyncService orgSyncService) {
        this.orgSyncService = orgSyncService;
    }

    @Operation(summary = "从数据中台同步（org/staff/student/alumni）")
    @PostMapping("/{type}")
    public Result<OrgSyncService.SyncResult> sync(@PathVariable String type,
                                                  @RequestParam(required = false) Long schoolId) {
        return Result.success(orgSyncService.sync(SchoolScopes.resolve(schoolId), type));
    }
}
