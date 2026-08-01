package com.jinfu.approval.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.approval.entity.SysProcessTemplate;
import com.jinfu.approval.mapper.SysProcessTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTemplateServiceImpl
        extends ServiceImpl<SysProcessTemplateMapper, SysProcessTemplate>
        implements ProcessTemplateService {
}
