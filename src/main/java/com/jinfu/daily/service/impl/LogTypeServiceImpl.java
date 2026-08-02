package com.jinfu.daily.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.daily.entity.LogType;
import com.jinfu.daily.mapper.LogTypeMapper;
import com.jinfu.daily.service.LogTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogTypeServiceImpl extends ServiceImpl<LogTypeMapper, LogType> implements LogTypeService {

    @Override
    public List<LogType> listEnabled() {
        return lambdaQuery()
                .eq(LogType::getEnabled, 1)
                .orderByAsc(LogType::getSortOrder)
                .list();
    }
}
