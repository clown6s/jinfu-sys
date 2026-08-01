package com.jinfu.flowable.dto;

import lombok.Data;
import org.flowable.engine.history.HistoricActivityInstance;

import java.util.List;

@Data
public class ProcessDiagramVO {
    private String imageContent;
    private List<HistoricActivityInstance> highlights;
}
