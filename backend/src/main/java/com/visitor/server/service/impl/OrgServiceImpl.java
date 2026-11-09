package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.Staff;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.service.OrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OrgServiceImpl implements OrgService {

    private final OrgMapper orgMapper;
    private final StaffMapper staffMapper;

    public OrgServiceImpl(OrgMapper orgMapper, StaffMapper staffMapper) {
        this.orgMapper = orgMapper;
        this.staffMapper = staffMapper;
    }

    @Override
    public List<Map<String, Object>> tree() {
        List<Org> all = orgMapper.selectList(new LambdaQueryWrapper<Org>()
                .eq(Org::getStatus, 1)
                .orderByAsc(Org::getSortOrder)
                .orderByAsc(Org::getId));
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Org org : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", org.getId());
            node.put("parentId", org.getParentId());
            node.put("name", org.getName());
            node.put("headTeacherId", org.getHeadTeacherId());
            node.put("externalId", org.getExternalId());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(org.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Org org : all) {
            Map<String, Object> node = nodes.get(org.getId());
            Map<String, Object> parent = org.getParentId() == null ? null : nodes.get(org.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                ((List<Map<String, Object>>) parent.get("children")).add(node);
            }
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Org create(Long parentId, String name, Integer sortOrder) {
        if (name == null || name.isBlank()) {
            throw new BizException(400, "组织名称不能为空");
        }
        Org org = new Org();
        org.setName(name.trim());
        org.setParentId(parentId);
        org.setSortOrder(sortOrder == null ? 0 : sortOrder);
        org.setStatus(1);
        applyParent(org, parentId);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        orgMapper.insert(org);
        return org;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Org update(Long id, Long parentId, String name, Integer sortOrder) {
        Org org = require(id);
        if (parentId != null && Objects.equals(parentId, id)) {
            throw new BizException(400, "不能选择自己作为上级");
        }
        if (parentId != null && isDescendant(id, parentId)) {
            throw new BizException(400, "不能移动到自己的下级");
        }
        if (name != null && !name.isBlank()) {
            org.setName(name.trim());
        }
        if (sortOrder != null) {
            org.setSortOrder(sortOrder);
        }
        org.setParentId(parentId);
        applyParent(org, parentId);
        org.setUpdatedAt(LocalDateTime.now());
        orgMapper.updateById(org);
        refreshAncestors(org);
        return org;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        Long children = orgMapper.selectCount(new LambdaQueryWrapper<Org>().eq(Org::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(400, "请先删除下级节点");
        }
        Long staff = staffMapper.selectCount(new LambdaQueryWrapper<Staff>().eq(Staff::getOrgId, id));
        if (staff != null && staff > 0) {
            throw new BizException(400, "该节点下还有人员，不能删除");
        }
        orgMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, Object> importRows(java.util.List<com.visitor.domain.dto.OrgImportRow> rows) {
        java.util.List<Org> all = orgMapper.selectList(null);
        java.util.Map<Long, Org> byId = new java.util.HashMap<>();
        for (Org org : all) {
            byId.put(org.getId(), org);
        }
        java.util.Map<Long, String> paths = new java.util.HashMap<>();
        java.util.Map<String, Org> byPath = new java.util.HashMap<>();
        for (Org org : all) {
            byPath.put(computePath(org, byId, paths), org);
        }

        int created = 0;
        int skipped = 0;
        java.util.List<java.util.Map<String, Object>> errors = new java.util.ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            com.visitor.domain.dto.OrgImportRow row = rows.get(i);
            int excelRow = i + 2;
            try {
                String name = row.getName() == null ? "" : row.getName().trim();
                if (name.isEmpty()) {
                    throw new BizException(400, "组织名称不能为空");
                }
                String parentPath = row.getParentPath() == null ? "" : row.getParentPath().trim()
                        .replace('\\', '/');
                while (parentPath.startsWith("/")) {
                    parentPath = parentPath.substring(1);
                }
                while (parentPath.endsWith("/")) {
                    parentPath = parentPath.substring(0, parentPath.length() - 1);
                }
                Org parent = null;
                if (!parentPath.isEmpty()) {
                    parent = byPath.get(parentPath);
                    if (parent == null) {
                        throw new BizException(400, "上级组织不存在：" + parentPath);
                    }
                }
                String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
                if (byPath.containsKey(path)) {
                    skipped++;
                    continue;
                }
                Long sameName = orgMapper.selectCount(new LambdaQueryWrapper<Org>()
                        .eq(parent == null, Org::getParentId, null)
                        .eq(parent != null, Org::getParentId, parent == null ? null : parent.getId())
                        .eq(Org::getName, name));
                if (sameName != null && sameName > 0) {
                    skipped++;
                    continue;
                }
                Org org = create(parent == null ? null : parent.getId(), name, 0);
                byPath.put(path, org);
                created++;
            } catch (Exception e) {
                java.util.Map<String, Object> error = new java.util.LinkedHashMap<>();
                error.put("row", excelRow);
                error.put("name", row.getName());
                error.put("message", e instanceof BizException ? e.getMessage() : "导入失败");
                errors.add(error);
            }
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return result;
    }

    private String computePath(Org org, java.util.Map<Long, Org> byId, java.util.Map<Long, String> paths) {
        String cached = paths.get(org.getId());
        if (cached != null) {
            return cached;
        }
        String path;
        if (org.getParentId() == null) {
            path = org.getName();
        } else {
            Org parent = byId.get(org.getParentId());
            path = (parent == null ? "" : computePath(parent, byId, paths) + "/") + org.getName();
        }
        paths.put(org.getId(), path);
        return path;
    }

    private Org require(Long id) {
        Org org = orgMapper.selectById(id);
        if (org == null) {
            throw new BizException(404, "组织节点不存在");
        }
        return org;
    }

    private void applyParent(Org org, Long parentId) {
        if (parentId == null) {
            org.setLevel(1);
            org.setAncestors("");
            return;
        }
        Org parent = require(parentId);
        org.setLevel((parent.getLevel() == null ? 1 : parent.getLevel()) + 1);
        String ancestors = parent.getAncestors();
        org.setAncestors((ancestors == null || ancestors.isBlank() ? "" : ancestors + ",") + parent.getId());
    }

    private boolean isDescendant(Long id, Long targetId) {
        Org target = orgMapper.selectById(targetId);
        if (target == null || target.getAncestors() == null) {
            return false;
        }
        String path = target.getAncestors();
        for (String part : path.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            if (Long.parseLong(part) == id) {
                return true;
            }
        }
        return false;
    }

    private void refreshAncestors(Org parent) {
        List<Org> children = orgMapper.selectList(new LambdaQueryWrapper<Org>()
                .eq(Org::getParentId, parent.getId()));
        for (Org child : children) {
            child.setLevel((parent.getLevel() == null ? 1 : parent.getLevel()) + 1);
            String ancestors = parent.getAncestors();
            child.setAncestors((ancestors == null || ancestors.isBlank() ? "" : ancestors + ",") + parent.getId());
            child.setUpdatedAt(LocalDateTime.now());
            orgMapper.updateById(child);
            refreshAncestors(child);
        }
    }
}
