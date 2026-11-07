package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.SchoolClassSaveDTO;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.vo.SchoolClassVO;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.service.SchoolClassService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SchoolClassServiceImpl implements SchoolClassService {

    private final SchoolClassMapper classMapper;

    public SchoolClassServiceImpl(SchoolClassMapper classMapper) {
        this.classMapper = classMapper;
    }

    @Override
    public List<SchoolClassVO> list(Long schoolId) {
        return classMapper.selectList(new LambdaQueryWrapper<SchoolClass>()
                        .eq(SchoolClass::getSchoolId, schoolId)
                        .orderByAsc(SchoolClass::getGrade)
                        .orderByAsc(SchoolClass::getName))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchoolClassVO create(Long schoolId, SchoolClassSaveDTO dto) {
        Long exists = classMapper.selectCount(new LambdaQueryWrapper<SchoolClass>()
                .eq(SchoolClass::getSchoolId, schoolId)
                .eq(SchoolClass::getName, dto.getName().trim()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "班级名称已存在");
        }
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName(dto.getName().trim());
        schoolClass.setGrade(dto.getGrade());
        schoolClass.setHeadTeacherId(dto.getHeadTeacherId());
        schoolClass.setCounselorId(dto.getCounselorId());
        schoolClass.setStatus(1);
        schoolClass.setCreatedAt(LocalDateTime.now());
        schoolClass.setUpdatedAt(LocalDateTime.now());
        classMapper.insert(schoolClass);
        return toVO(schoolClass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchoolClassVO update(Long schoolId, Long id, SchoolClassSaveDTO dto) {
        SchoolClass schoolClass = classMapper.selectById(id);
        if (schoolClass == null || !Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new BizException(404, "班级不存在");
        }
        schoolClass.setName(dto.getName().trim());
        schoolClass.setGrade(dto.getGrade());
        schoolClass.setHeadTeacherId(dto.getHeadTeacherId());
        schoolClass.setCounselorId(dto.getCounselorId());
        schoolClass.setUpdatedAt(LocalDateTime.now());
        classMapper.updateById(schoolClass);
        return toVO(schoolClass);
    }

    private SchoolClassVO toVO(SchoolClass schoolClass) {
        SchoolClassVO vo = new SchoolClassVO();
        vo.setId(schoolClass.getId());
        vo.setSchoolId(schoolClass.getSchoolId());
        vo.setName(schoolClass.getName());
        vo.setGrade(schoolClass.getGrade());
        vo.setHeadTeacherId(schoolClass.getHeadTeacherId());
        vo.setCounselorId(schoolClass.getCounselorId());
        vo.setStatus(schoolClass.getStatus());
        return vo;
    }
}
