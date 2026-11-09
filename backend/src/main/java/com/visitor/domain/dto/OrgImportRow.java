package com.visitor.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class OrgImportRow {

    @ExcelProperty("组织名称")
    private String name;

    /** 上级组织路径，如：人工智能学院/24级；留空表示根节点 */
    @ExcelProperty("上级组织")
    private String parentPath;
}
