package com.jinfu.daily.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.daily.dto.DailyReportVO;
import com.jinfu.daily.dto.DailySubmitRequest;
import com.jinfu.daily.entity.DailyReport;

public interface DailyReportService extends IService<DailyReport> {

    /**
     * 我的日报表单：按当前用户部门解析对应表单 schema（含今日是否已提交）
     */
    DailyReportVO myForm(Long userId, Long deptId);

    /**
     * 提交日报（当日查重；配置了审批模板则自动发起审批并推送待办消息）
     */
    DailyReportVO submit(DailySubmitRequest request, Long userId, String userName, Long deptId);

    /**
     * 我的日报列表（分页，按日期倒序）
     */
    IPage<DailyReportVO> myReports(Page<DailyReport> page, Long userId);

    /**
     * 日报详情（仅本人可查）
     */
    DailyReportVO detail(Long id, Long userId);
}
