package com.infobeans.ibnextstep.assignment.dto;

import com.infobeans.ibnextstep.assignment.AssignmentSubmission;
import com.infobeans.ibnextstep.assignment.AssignmentSubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionResponse {
    private String id;
    private String assignmentId;
    private String studentId;
    private String studentName;

    private List<AnswerResponse> answers;

    private AssignmentSubmissionStatus status;
    private Instant submittedAt;

    private String feedback;
    private Integer rating;
    private Instant gradedAt;

    /**
     * @param downloadUrlPrefix e.g. "/api/trainer/assignments/{id}/submissions/{studentId}"
     *                          or "/api/student/assignments/{id}/submission" — files are
     *                          appended as "/files/{fileId}/download".
     */
    public static AssignmentSubmissionResponse from(AssignmentSubmission s, String downloadUrlPrefix) {
        List<AnswerResponse> answers = s.getAnswers() == null ? List.of() : s.getAnswers().stream()
                .map(a -> AnswerResponse.builder()
                        .questionId(a.getQuestionId())
                        .textAnswer(a.getTextAnswer())
                        .files(a.getFiles() == null ? List.of() : a.getFiles().stream()
                                .map(f -> AssignmentFileResponse.builder()
                                        .fileId(f.getFileId()).fileName(f.getOriginalFileName())
                                        .fileSizeBytes(f.getFileSizeBytes()).mimeType(f.getMimeType())
                                        .downloadUrl(downloadUrlPrefix + "/files/" + f.getFileId() + "/download")
                                        .build())
                                .toList())
                        .build())
                .toList();

        return AssignmentSubmissionResponse.builder()
                .id(s.getId()).assignmentId(s.getAssignmentId())
                .studentId(s.getStudentId()).studentName(s.getStudentName())
                .answers(answers)
                .status(s.getStatus()).submittedAt(s.getSubmittedAt())
                .feedback(s.getFeedback()).rating(s.getRating()).gradedAt(s.getGradedAt())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResponse {
        private String questionId;
        private String textAnswer;
        private List<AssignmentFileResponse> files;
    }
}
