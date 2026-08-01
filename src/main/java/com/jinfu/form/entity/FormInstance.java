package com.jinfu.form.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("form_instance")
public class FormInstance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Form Definition Key */
    private String formKey;

    /** Flowable Process Instance ID */
    private String procInstId;

    /** Business data (JSON) */
    private String businessDataJson;

    /** Instance Title */
    private String title;

    /** Creator User ID */
    private Long creator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
