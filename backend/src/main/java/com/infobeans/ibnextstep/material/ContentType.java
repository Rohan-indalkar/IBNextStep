package com.infobeans.ibnextstep.material;

public enum ContentType {

    PDF(true),
    PPT(true),
    DOCX(true),
    ZIP(true),
    RECORDED_SESSION(true),
    VIDEO_LINK(false),
    EXTERNAL_RESOURCE_LINK(false);

    /** True when this content type is satisfied by uploaded file(s); false when it's a URL. */
    private final boolean fileBased;

    ContentType(boolean fileBased) {
        this.fileBased = fileBased;
    }

    public boolean isFileBased() {
        return fileBased;
    }
}
