package com.visitor.workflow;

import com.visitor.domain.entity.ApprovalFlow;

import java.util.List;

public interface ApprovalFlowService {

    List<ApprovalFlow> list();

    ApprovalFlow detail(String code, Integer version);

    ApprovalFlow saveDraft(String code, FlowDefinition definition, Long operatorId);

    List<String> validate(String code);

    ApprovalFlow publish(String code, Long operatorId);

    ApprovalFlow publishVersion(String code, Integer version, Long operatorId);

    ApprovalFlow offline(String code, Long operatorId);

    List<ApprovalFlow> versions(String code);

    List<PreviewNode> preview(String code, Long targetId);

    record PreviewNode(Integer seq, String name, String assigneeType,
                       Long assigneeId, String assigneeName, boolean optional) {
    }
}
