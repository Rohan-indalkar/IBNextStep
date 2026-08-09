package com.infobeans.ibnextstep.codingassessment;

import com.infobeans.ibnextstep.codingassessment.piston.PistonClient;
import com.infobeans.ibnextstep.codingassessment.piston.PistonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sits between the Trainer/Student services and PistonClient — runs a
 * submission against a set of test cases and maps Piston's exit-code/signal
 * result onto our SubmissionStatus enum. Neither Run nor Submit talk to
 * PistonClient directly; both go through here so the mapping logic exists
 * in one place.
 *
 * Note: unlike Judge0, Piston does not report execution time or memory
 * usage in its response, and it does not do output comparison itself — both
 * of those are handled here now. executionTimeMs / memoryKb are therefore
 * always null; downstream consumers (Submission, RunCodeResponse) already
 * treat these as nullable so nothing else needs to change for that.
 */
@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final PistonClient pistonClient;

    public record CaseExecution(TestCase testCase, boolean passed, String actualOutput,
                                 Long executionTimeMs, Long memoryKb, SubmissionStatus status, String compileOutput) {}

    public CaseExecution runAgainstCase(ProgrammingLanguage language, String code, TestCase testCase,
                                         int timeLimitSeconds, int memoryLimitMb) {
        PistonResult result = pistonClient.execute(language, code, testCase.getInput(),
                timeLimitSeconds, memoryLimitMb);

        SubmissionStatus status = mapStatus(result, testCase.getExpectedOutput());
        boolean passed = status == SubmissionStatus.ACCEPTED;

        return new CaseExecution(testCase, passed, result.getStdout(), null, null,
                status, result.getCompileOutput());
    }

    public List<CaseExecution> runAgainstCases(ProgrammingLanguage language, String code, List<TestCase> testCases,
                                                int timeLimitSeconds, int memoryLimitMb) {
        return testCases.stream()
                .map(tc -> runAgainstCase(language, code, tc, timeLimitSeconds, memoryLimitMb))
                .toList();
    }

    /**
     * Piston has no Judge0-style numeric status codes — it only reports an
     * exit code/signal per stage, and leaves correctness comparison to us:
     *  - compile stage present with non-zero exit code -> COMPILATION_ERROR
     *  - run stage killed by SIGKILL -> TIME_LIMIT_EXCEEDED (how Piston enforces
     *    run_timeout; it has no separate OOM signal, so a memory-limit kill is
     *    indistinguishable from a timeout kill here — both surface as SIGKILL)
     *  - run stage killed by any other signal, or non-zero exit code -> RUNTIME_ERROR
     *  - run stage exits cleanly -> compare trimmed stdout against the expected output
     */
    private SubmissionStatus mapStatus(PistonResult result, String expectedOutput) {
        if (result.isCompileStagePresent() && result.getCompileCode() != null && result.getCompileCode() != 0) {
            return SubmissionStatus.COMPILATION_ERROR;
        }
        if ("SIGKILL".equals(result.getRunSignal())) {
            return SubmissionStatus.TIME_LIMIT_EXCEEDED;
        }
        if (result.getRunSignal() != null || (result.getRunCode() != null && result.getRunCode() != 0)) {
            return SubmissionStatus.RUNTIME_ERROR;
        }

        String actual = result.getStdout() == null ? "" : result.getStdout().trim();
        String expected = expectedOutput == null ? "" : expectedOutput.trim();
        return actual.equals(expected) ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER;
    }
}
