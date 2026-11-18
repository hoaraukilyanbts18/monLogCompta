package fr.monlogcompta.service;

import java.util.regex.Pattern;

/** Contrôles de saisie (EF-05) ; chaque méthode renvoie un message d'erreur, ou null si tout est valide. */
public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    public static final int MIN_PASSWORD = 8;

    private Validation() {
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value.strip()).matches();
    }

    /** Supprime les espaces d'un SIRET saisi « 412 345 678 90123 ». */
    public static String normalizeSiret(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    public static boolean isValidSiret(String value) {
        return normalizeSiret(value).matches("\\d{14}");
    }

    public static String checkEmail(String email) {
        return isValidEmail(email) ? null : "L'adresse e-mail n'est pas valide.";
    }

    public static String checkPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD
            ? null : "Le mot de passe doit contenir au moins " + MIN_PASSWORD + " caractères.";
    }

    public static String checkClient(String name, String siret, String address, String email, String password) {
        if (name == null || name.isBlank()) return "La raison sociale est obligatoire.";
        if (!isValidSiret(siret)) return "Le SIRET doit contenir 14 chiffres.";
        if (address == null || address.isBlank()) return "L'adresse est obligatoire.";
        String problem = checkEmail(email);
        return problem != null ? problem : checkPassword(password);
    }
}
