package fr.monlogcompta.model;

public record SessionUser(long id, String email, String displayName, Role role, Long clientId) {
    public enum Role {
        ADMIN, CLIENT
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
