package com.mulya.employee.timesheet.dto;

import java.time.LocalDateTime;

public class AttachmentDto {

    private Long id;
    private String filename;
    private String filetype;
    private LocalDateTime uploadedAt;

    public AttachmentDto() {
    }

    public AttachmentDto(Long id, String filename, String filetype, LocalDateTime uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.filetype = filetype;
        this.uploadedAt = uploadedAt;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFiletype() { return filetype; }
    public void setFiletype(String filetype) { this.filetype = filetype; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
