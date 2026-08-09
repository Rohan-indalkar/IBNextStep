package com.infobeans.ibnextstep.codingassessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** "Hidden Test Cases must never be returned in Student APIs" — enforced by DTO mapping in the controller layer, never by hiding the field here. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "testcase")
public class TestCase {

    @Id
    private String id;

    private String questionId;
    private String input;
    private String expectedOutput;
    private boolean hidden;
}
