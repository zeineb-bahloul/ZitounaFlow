package com.example.zitounaproject.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Classe abstraite Utilisateur (diagramme de classes).
 * Stockée dans Firestore : collection "utilisateurs", document = uid Firebase Auth.
 */
public abstract class Utilisateur {

    private String id;
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private Role role;
    private Date dateCreation;

    protected Utilisateur() {
        // Constructeur vide requis par Firestore
    }

    protected Utilisateur(Role role) {
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    @Exclude
    public String getNomComplet() {
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        return (p + " " + n).trim();
    }

    @Exclude
    public String getInitiales() {
        StringBuilder sb = new StringBuilder();
        if (prenom != null && !prenom.trim().isEmpty()) sb.append(prenom.trim().charAt(0));
        if (nom != null && !nom.trim().isEmpty()) sb.append(nom.trim().charAt(0));
        return sb.length() == 0 ? "?" : sb.toString().toUpperCase();
    }

    /** Nom affiché dans les en-têtes ("Bonjour, ..."). */
    @Exclude
    public String getNomAffichage() {
        return prenom != null && !prenom.trim().isEmpty() ? prenom.trim() : getNomComplet();
    }
}
