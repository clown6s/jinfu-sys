package com.jinfu.daily.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.daily.entity.LogType;

import java.util.List;

public interface LogTypeService extends IService<LogType> {

    /** 查询所有启用的日志类型（按排序号升序） */
    List<LogType> listEnabled();
}
