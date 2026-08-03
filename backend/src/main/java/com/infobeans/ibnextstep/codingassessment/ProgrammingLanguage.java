package com.infobeans.ibnextstep.codingassessment;

/**
 * pistonLanguage maps to the "language" field Piston expects (matches
 * GET /api/v2/runtimes on a self-hosted instance, or the names emkc.org
 * advertises). fileName is the name we upload the submission under —
 * for Java specifically this MUST match the public class name
 * ("Main"), matching the convention the previous Judge0 integration
 * also relied on. For the others the name is cosmetic.
 */
public enum ProgrammingLanguage {
    JAVA("java", "Main.java"),
    PYTHON3("python", "main.py"),
    CPP("cpp", "main.cpp"),
    C("c", "main.c"),
    JAVASCRIPT("javascript", "main.js");

    private final String pistonLanguage;
    private final String fileName;

    ProgrammingLanguage(String pistonLanguage, String fileName) {
        this.pistonLanguage = pistonLanguage;
        this.fileName = fileName;
    }

    public String pistonLanguage() {
        return pistonLanguage;
    }

    public String fileName() {
        return fileName;
    }
}
