package com.talentgrid.auth.constants;

public final class PermissionConstants {

    private PermissionConstants() {
        throw new IllegalStateException("Utility class");
    }

    // User Permissions
    public static final String CREATE_USER = "CREATE_USER";
    public static final String UPDATE_USER = "UPDATE_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String VIEW_USER = "VIEW_USER";

    // Demand Permissions
    public static final String CREATE_DEMAND = "CREATE_DEMAND";
    public static final String APPROVE_DEMAND = "APPROVE_DEMAND";
    public static final String VIEW_DEMAND = "VIEW_DEMAND";
    public static final String UPDATE_DEMAND = "UPDATE_DEMAND";

    // Candidate Permissions
    public static final String CREATE_CANDIDATE = "CREATE_CANDIDATE";
    public static final String VIEW_CANDIDATE = "VIEW_CANDIDATE";
    public static final String UPDATE_CANDIDATE = "UPDATE_CANDIDATE";

    // Interview Permissions
    public static final String SCHEDULE_INTERVIEW = "SCHEDULE_INTERVIEW";
    public static final String SUBMIT_FEEDBACK = "SUBMIT_FEEDBACK";

    // Admin Permissions
    public static final String MANAGE_ROLES = "MANAGE_ROLES";
    public static final String VIEW_AUDIT_LOGS = "VIEW_AUDIT_LOGS";
}