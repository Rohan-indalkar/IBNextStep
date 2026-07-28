package com.infobeans.ibnextstep.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReportInfo {
    private String id;
    private String name;
    private String description;
    private String category;
}
