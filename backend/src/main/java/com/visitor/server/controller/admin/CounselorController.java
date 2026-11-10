package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.CounselorOrgSaveDTO;
import com.visitor.domain.vo.CounselorOrgVO;
import com.visitor.server.service.CounselorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "辅导员管理")
@RestController
@RequestMapping("/web/admin/counselors")
public class CounselorController {

    private final CounselorService counselorService;

    public CounselorController(CounselorService counselorService) {
        this.counselorService = counselorService;
    }

    @Operation(summary = "辅导员组织关联列表")
    @GetMapping
    public Result<PageResult<CounselorOrgVO>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(counselorService.page(keyword, page, size));
    }

    @Operation(summary = "辅导员候选人")
    @GetMapping("/candidates")
    public Result<List<Map<String, Object>>> candidates(@RequestParam(required = false) String keyword) {
        return Result.success(counselorService.candidates(keyword));
    }

    @Operation(summary = "组织覆盖人数预览")
    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam Long orgId) {
        return Result.success(counselorService.preview(orgId));
    }

    @Operation(summary = "新增辅导员组织关联")
    @PostMapping
    public Result<CounselorOrgVO> create(@Valid @RequestBody CounselorOrgSaveDTO dto) {
        return Result.success(counselorService.create(dto));
    }

    @Operation(summary = "修改负责组织")
    @PutMapping("/{id}")
    public Result<CounselorOrgVO> updateOrg(@PathVariable Long id, @RequestParam Long orgId) {
        return Result.success(counselorService.updateOrg(id, orgId));
    }

    @Operation(summary = "移除辅导员组织关联")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        counselorService.delete(id);
        return Result.success();
    }
}
