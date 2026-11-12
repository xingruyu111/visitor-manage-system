package com.visitor.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.server.mapper.ApprovalFlowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private final ApprovalFlowMapper flowMapper;
    private final FlowValidator flowValidator;
    private final AssigneeResolver assigneeResolver;
    private final ObjectMapper objectMapper;

    public ApprovalFlowServiceImpl(ApprovalFlowMapper flowMapper, FlowValidator flowValidator,
                                   AssigneeResolver assigneeResolver, ObjectMapper objectMapper) {
        this.flowMapper = flowMapper;
        this.flowValidator = flowValidator;
        this.assigneeResolver = assigneeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ApprovalFlow> list() {
        List<ApprovalFlow> all = flowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .orderByAsc(ApprovalFlow::getFlowCode)
                .orderByDesc(ApprovalFlow::getVersion));
        Map<String, ApprovalFlow> latest = new LinkedHashMap<>();
        for (ApprovalFlow flow : all) {
            ApprovalFlow current = latest.get(flow.getFlowCode());
            if (current == null || (flow.getStatus() == 1 && current.getStatus() != 1)) {
                latest.put(flow.getFlowCode(), flow);
            }
        }
        return new ArrayList<>(latest.values());
    }

    @Override
    public ApprovalFlow detail(String code, Integer version) {
        if (version != null) {
            ApprovalFlow flow = findByVersion(code, version);
            if (flow == null) {
                throw new BizException(404, "流程版本不存在");
            }
            return flow;
        }
        return latest(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlow saveDraft(String code, FlowDefinition definition, Long operatorId) {
        if (definition == null || definition.code() == null || !code.equals(definition.code())) {
            throw new BizException(400, "流程编码与路径不一致");
        }
        ApprovalFlow draft = draftOf(code);
        String json = write(definition);
        LocalDateTime now = LocalDateTime.now();
        if (draft == null) {
            draft = new ApprovalFlow();
            draft.setFlowCode(code);
            draft.setName(definition.name() == null ? code : definition.name());
            draft.setVersion(maxVersion(code) + 1);
            draft.setStatus(0);
            draft.setDefinitionJson(json);
            draft.setCreatedBy(operatorId);
            draft.setCreatedAt(now);
            draft.setUpdatedAt(now);
            flowMapper.insert(draft);
        } else {
            draft.setName(definition.name() == null ? draft.getName() : definition.name());
            draft.setDefinitionJson(json);
            draft.setUpdatedAt(now);
            flowMapper.updateById(draft);
        }
        return draft;
    }

    @Override
    public List<String> validate(String code) {
        ApprovalFlow draft = draftOf(code);
        ApprovalFlow target = draft != null ? draft : latest(code);
        if (target == null) {
            return List.of("流程不存在");
        }
        return flowValidator.validate(read(target.getDefinitionJson()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlow publish(String code, Long operatorId) {
        ApprovalFlow draft = draftOf(code);
        if (draft == null) {
            throw new BizException(400, "没有可发布的草稿");
        }
        List<String> errors = flowValidator.validate(read(draft.getDefinitionJson()));
        if (!errors.isEmpty()) {
            throw new BizException(400, "流程校验失败: " + String.join("; ", errors));
        }
        assertTargetTypesNotUsed(code, read(draft.getDefinitionJson()));
        List<ApprovalFlow> published = flowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .eq(ApprovalFlow::getStatus, 1));
        LocalDateTime now = LocalDateTime.now();
        for (ApprovalFlow flow : published) {
            flow.setStatus(2);
            flow.setUpdatedAt(now);
            flowMapper.updateById(flow);
        }
        draft.setStatus(1);
        draft.setPublishedAt(now);
        draft.setPublishedBy(operatorId);
        draft.setUpdatedAt(now);
        flowMapper.updateById(draft);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlow publishVersion(String code, Integer version, Long operatorId) {
        ApprovalFlow selected = findByVersion(code, version);
        if (selected == null) {
            throw new BizException(404, "流程版本不存在");
        }
        List<String> errors = flowValidator.validate(read(selected.getDefinitionJson()));
        if (!errors.isEmpty()) {
            throw new BizException(400, "流程校验失败: " + String.join("; ", errors));
        }
        assertTargetTypesNotUsed(code, read(selected.getDefinitionJson()));
        LocalDateTime now = LocalDateTime.now();
        List<ApprovalFlow> published = flowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .eq(ApprovalFlow::getStatus, 1));
        for (ApprovalFlow flow : published) {
            flow.setStatus(2);
            flow.setUpdatedAt(now);
            flowMapper.updateById(flow);
        }
        selected.setStatus(1);
        selected.setPublishedAt(now);
        selected.setPublishedBy(operatorId);
        selected.setUpdatedAt(now);
        flowMapper.updateById(selected);
        return selected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlow offline(String code, Long operatorId) {
        ApprovalFlow published = flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .eq(ApprovalFlow::getStatus, 1));
        if (published == null) {
            throw new BizException(400, "没有已发布的版本");
        }
        published.setStatus(2);
        published.setUpdatedAt(LocalDateTime.now());
        flowMapper.updateById(published);
        return published;
    }

    @Override
    public List<ApprovalFlow> versions(String code) {
        return flowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .orderByDesc(ApprovalFlow::getVersion));
    }

    @Override
    public List<PreviewNode> preview(String code, Long targetId) {
        ApprovalFlow flow = latest(code);
        if (flow == null) {
            throw new BizException(404, "流程不存在");
        }
        FlowDefinition definition = read(flow.getDefinitionJson());
        if (definition == null || definition.nodes() == null) {
            return List.of();
        }
        return definition.nodes().stream()
                .map(node -> {
                    Long assigneeId = assigneeResolver.resolveUserId(node.assignee(), targetId);
                    return new PreviewNode(node.seq(), node.name(), node.assignee(),
                            assigneeId, assigneeResolver.nameOf(assigneeId), node.isOptional());
                })
                .toList();
    }

    private ApprovalFlow latest(String code) {
        ApprovalFlow flow = flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .eq(ApprovalFlow::getStatus, 1)
                .orderByDesc(ApprovalFlow::getVersion)
                .last("LIMIT 1"));
        if (flow != null) {
            return flow;
        }
        return flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .orderByDesc(ApprovalFlow::getVersion)
                .last("LIMIT 1"));
    }

    private ApprovalFlow draftOf(String code) {
        return flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .eq(ApprovalFlow::getStatus, 0)
                .orderByDesc(ApprovalFlow::getVersion)
                .last("LIMIT 1"));
    }

    private ApprovalFlow findByVersion(String code, int version) {
        return flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .eq(ApprovalFlow::getVersion, version));
    }

    private int maxVersion(String code) {
        ApprovalFlow latest = flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, code)
                .orderByDesc(ApprovalFlow::getVersion)
                .last("LIMIT 1"));
        return latest == null ? 0 : latest.getVersion();
    }

    /**
     * 一个被访人类型只能关联一条已发布流程。
     */
    private void assertTargetTypesNotUsed(String currentCode, FlowDefinition definition) {
        if (definition.targetTypes() == null || definition.targetTypes().isEmpty()) {
            return;
        }
        List<ApprovalFlow> published = flowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getStatus, 1));
        for (ApprovalFlow flow : published) {
            if (currentCode.equals(flow.getFlowCode())) {
                continue;
            }
            FlowDefinition other;
            try {
                other = read(flow.getDefinitionJson());
            } catch (Exception e) {
                continue;
            }
            if (other.targetTypes() == null) {
                continue;
            }
            for (String targetType : definition.targetTypes()) {
                if (other.targetTypes().contains(targetType)) {
                    throw new BizException(400, "被访人类型 " + targetType
                            + " 已关联流程 " + flow.getFlowCode() + "，不能重复关联");
                }
            }
        }
    }

    private FlowDefinition read(String json) {        try {
            return objectMapper.readValue(json, FlowDefinition.class);
        } catch (Exception e) {
            throw new BizException(400, "流程定义不是合法 JSON");
        }
    }

    private String write(FlowDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (Exception e) {
            throw new BizException(400, "流程定义序列化失败");
        }
    }
}
