package com.infobeans.ibnextstep.codingassessment.piston;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PistonResult {
    /** True only for languages with a separate compile step (Java, C, C++). */
    private boolean compileStagePresent;
    /** Exit code of the compile stage, null if killed by signal or no compile stage. */
    private Integer compileCode;
    private String compileOutput;
    /** Exit code of the run stage, null if killed by signal. */
    private Integer runCode;
    /** e.g. "SIGKILL", "SIGSEGV" — null if the process exited normally. */
    private String runSignal;
    private String stdout;
    private String stderr;
}
