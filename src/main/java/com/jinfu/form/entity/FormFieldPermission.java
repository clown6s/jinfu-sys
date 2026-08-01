package com.jinfu.form.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("form_field_permission")
public class FormFieldPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Form Definition ID */
    private Long formId;

    /** Flowable Process Definition ID */
    private String procDefId;

    /** BPMN Node ID (e.g. activity_manager_approve) */
    private String nodeId;

    /** Field business key (e.g. leave_days) */
    private String fieldKey;

    /** edit / readonly / required / hidden */
    private String permission;

    private LocalDateTime createTime;

    /** DTO for batch permission save */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldPermItem {
        private String fieldKey;
        private String permission;
    }
}
