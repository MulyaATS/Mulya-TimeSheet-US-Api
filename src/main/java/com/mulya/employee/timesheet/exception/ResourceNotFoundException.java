package com.mulya.employee.timesheet.exception;

public class ResourceNotFoundException extends RuntimeException {

    public enum ResourceType {
        TIMESHEET, PLACEMENT, ATTACHMENT, USER, EMPLOYEE
    }

    private final ResourceType resourceType;

    public ResourceNotFoundException(String message, ResourceType resourceType) {
        super(message);
        this.resourceType = resourceType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}
