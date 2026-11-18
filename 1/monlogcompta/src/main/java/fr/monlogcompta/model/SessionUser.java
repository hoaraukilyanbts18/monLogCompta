package fr.monlogcompta.model;

public record SessionUser(long id, String email, String displayName, Role role, Long clientId) {
    public enum Role {
        /** Gère les comptes utilisateurs. */
        ADMIN,
        /** Gère l'activité : clients, produits, factures, comptabilité, exports. */
        UTILISATEUR,
        /** Consulte uniquement ses propres factures. */
        CLIENT
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.UTILISATEUR;
    }

    public boolean isClient() {
        return role == Role.CLIENT;
    }
}
