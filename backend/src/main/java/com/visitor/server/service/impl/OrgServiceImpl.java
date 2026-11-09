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
