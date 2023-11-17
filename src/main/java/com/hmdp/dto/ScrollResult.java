package com.hmdp.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScrollResult {
    // 笔记
    private List<?> list;
    // 上一次最小时间
    private Long minTime;
    // 偏移量
    private Integer offset;

}
