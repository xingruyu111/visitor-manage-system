package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.DepartmentSaveDTO;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.vo.DepartmentVO;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final StaffMapper staffMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper, StaffMapper staffMapper) {
        this.departmentMapper = departmentMapper;
        this.staffMapper = staffMapper;
    }

    @Override
    public List<DepartmentVO> tree(Long schoolId) {
        List<Department> departments = departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, schoolId)
                .orderByAsc(Department::getSortOrder)
                .orderByAsc(Department::getId));

        Map<Long, DepartmentVO> index = new LinkedHashMap<>();
        for (Department department : departments) {
            DepartmentVO vo = toVO(department);
            index.put(department.getId(), vo);
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentVO vo : index.values()) {
            if (vo.getParentId() == null || !index.containsKey(vo.getParentId())) {
                roots.add(vo);
            } else {
                index.get(vo.getParentId()).getChildren().add(vo);
            }
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO create(Long schoolId, DepartmentSaveDTO dto) {
        Department department = new Department();
        department.setSchoolId(schoolId);
        department.setName(dto.getName().trim());
        department.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        department.setStatus(1);
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        applyParent(department, schoolId, dto.getParentId());
        departmentMapper.insert(department);
        return toVO(department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO update(Long schoolId, Long id, DepartmentSaveDTO dto) {
        Department department = require(schoolId, id);
        department.setName(dto.getName().trim());
        if (dto.getSortOrder() != null) {
            department.setSortOrder(dto.getSortOrder());
        }
        department.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(department);
        return toVO(department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long schoolId, Long id) {
        require(schoolId, id);
        Long children = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, schoolId)
                .eq(Department::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(400, "请先删除下级部门");
        }
        Long staffCount = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getDeptId, id));
        if (staffCount != null && staffCount > 0) {
            throw new BizException(400, "部门下还有人员，无法删除");
        }
        departmentMapper.deleteById(id);
    }

    private void applyParent(Department department, Long schoolId, Long parentId) {
        if (parentId == null) {
            department.setParentId(null);
            department.setLevel(1);
            department.setAncestors("");
            return;
        }
        Department parent = departmentMapper.selectById(parentId);
        if (parent == null || !Objects.equals(parent.getSchoolId(), schoolId)) {
            throw new BizException(400, "上级部门不存在");
        }
        department.setParentId(parentId);
        department.setLevel((parent.getLevel() == null ? 1 : parent.getLevel()) + 1);
        String ancestors = parent.getAncestors();
        department.setAncestors((ancestors == null || ancestors.isBlank() ? "" : ancestors + ",") + parent.getId());
    }

    private Department require(Long schoolId, Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null || !Objects.equals(department.getSchoolId(), schoolId)) {
            throw new BizException(404, "部门不存在");
        }
        return department;
    }

    private DepartmentVO toVO(Department department) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setParentId(department.getParentId());
        vo.setName(department.getName());
        vo.setLevel(department.getLevel());
        vo.setSortOrder(department.getSortOrder());
        vo.setStatus(department.getStatus());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
