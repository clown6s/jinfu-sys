package com.jinfu.daily.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.daily.dto.DailyConfigVO;
import com.jinfu.daily.entity.DailyFormConfig;

import java.util.List;

public interface DailyFormConfigService extends IService<DailyFormConfig> {

    /** 分页查询配置（含部门/表单/模板名称） */
    IPage<DailyConfigVO> pageConfigs(Page<DailyFormConfig> page, String keyword);

    /** 全量配置（含名称，供下拉选择） */
    List<DailyConfigVO> listConfigs();

    /** 新增配置（部门唯一校验） */
    void addConfig(DailyFormConfig config);

    /** 修改配置 */
    void updateConfig(DailyFormConfig config);

    /** 删除配置 */
    void removeConfig(Long id);
}
