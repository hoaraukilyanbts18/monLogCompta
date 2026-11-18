--creation la base de donnée 

CREATE DATABASE logcompta;

USE DATABASE logcompta;


-- Référentiels

CREATE TABLE entreprise (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    raison_sociale  VARCHAR(150) NOT NULL,
    siren           VARCHAR(9),
    siret           VARCHAR(14),
    tva_intracom    VARCHAR(20),
    adresse         VARCHAR(255),
    code_postal     VARCHAR(10),
    ville           VARCHAR(100),
    pays            VARCHAR(2) DEFAULT 'FR',
    email           VARCHAR(150),
    telephone       VARCHAR(30),
    iban            VARCHAR(34)
);

CREATE TABLE utilisateur (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login           VARCHAR(50) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,        
    role            VARCHAR(20) NOT NULL DEFAULT 'USER'
);

CREATE TABLE taux_tva (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle         VARCHAR(50) NOT NULL,
    taux            NUMERIC(5,2) NOT NULL,        
    actif           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE client (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    raison_sociale  VARCHAR(150),
    nom             VARCHAR(100),
    prenom          VARCHAR(100),
    siren           VARCHAR(9),
    siret           VARCHAR(14),
    tva_intracom    VARCHAR(20),
    adresse         VARCHAR(255),
    code_postal     VARCHAR(10),
    ville           VARCHAR(100),
    pays            VARCHAR(2) DEFAULT 'FR',
    email           VARCHAR(150),
    telephone       VARCHAR(30)
);

CREATE TABLE produit_prestation (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference       VARCHAR(50) UNIQUE,
    libelle         VARCHAR(150) NOT NULL,
    description     TEXT,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('PRODUIT', 'SERVICE')),
    prix_vente_ht   NUMERIC(12,2) NOT NULL,
    cout_achat      NUMERIC(12,2) NOT NULL DEFAULT 0,
    taux_tva_id     INT NOT NULL REFERENCES taux_tva(id),
    actif           BOOLEAN NOT NULL DEFAULT TRUE
);



CREATE TABLE facture (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero          VARCHAR(30) NOT NULL UNIQUE,
    date_emission   DATE NOT NULL,
    date_echeance   DATE,
    client_id       INT NOT NULL REFERENCES client(id),
    statut          VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
                    CHECK (statut IN ('BROUILLON', 'EMISE', 'PAYEE_PARTIELLEMENT', 'PAYEE', 'ANNULEE')),
    remise_globale  NUMERIC(5,2) NOT NULL DEFAULT 0,     
    total_ht        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_tva       NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_ttc       NUMERIC(12,2) NOT NULL DEFAULT 0,
    marge_totale    NUMERIC(12,2) NOT NULL DEFAULT 0,
    commentaire     TEXT
);

CREATE TABLE ligne_facture (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facture_id          INT NOT NULL REFERENCES facture(id) ON DELETE CASCADE,
    produit_id          INT NOT NULL REFERENCES produit_prestation(id),
    libelle             VARCHAR(150) NOT NULL,            
    quantite            NUMERIC(10,2) NOT NULL CHECK (quantite > 0),
    prix_unitaire_ht    NUMERIC(12,2) NOT NULL,           
    remise_ligne        NUMERIC(5,2) NOT NULL DEFAULT 0,  
    taux_tva            NUMERIC(5,2) NOT NULL,            
    cout_unitaire       NUMERIC(12,2) NOT NULL DEFAULT 0, 
    total_ht            NUMERIC(12,2) NOT NULL,
    total_tva           NUMERIC(12,2) NOT NULL,
    total_ttc           NUMERIC(12,2) NOT NULL
);

CREATE TABLE paiement (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facture_id      INT NOT NULL REFERENCES facture(id),
    date_paiement   DATE NOT NULL,
    montant         NUMERIC(12,2) NOT NULL CHECK (montant > 0),
    mode            VARCHAR(20) NOT NULL
                    CHECK (mode IN ('VIREMENT', 'CHEQUE', 'CARTE', 'ESPECES', 'PRELEVEMENT')),
    reference       VARCHAR(100)
);



CREATE TABLE compte_comptable (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero          VARCHAR(10) NOT NULL UNIQUE,          
    libelle         VARCHAR(150) NOT NULL,
    type            VARCHAR(20) NOT NULL
                    CHECK (type IN ('ACTIF', 'PASSIF', 'CHARGE', 'PRODUIT', 'TIERS', 'TRESORERIE'))
);

CREATE TABLE ecriture_comptable (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date_ecriture   DATE NOT NULL,
    journal         VARCHAR(10) NOT NULL,                 
    libelle         VARCHAR(200) NOT NULL,
    facture_id      INT REFERENCES facture(id),          
    paiement_id     INT REFERENCES paiement(id),       
    CHECK (facture_id IS NOT NULL OR paiement_id IS NOT NULL)
);

CREATE TABLE ligne_ecriture (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ecriture_id     INT NOT NULL REFERENCES ecriture_comptable(id) ON DELETE CASCADE,
    compte_id       INT NOT NULL REFERENCES compte_comptable(id),
    debit           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (debit >= 0),
    credit          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (credit >= 0),
    CHECK (debit = 0 OR credit = 0)
);



CREATE TABLE transmission (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facture_id      INT NOT NULL REFERENCES facture(id),
    date_envoi      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut          VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
                    CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REJETEE')),
    motif_rejet     TEXT,
    date_reponse    TIMESTAMP,
    fichier_xml     TEXT                               
);

-- Index utiles 

CREATE INDEX idx_facture_client       ON facture(client_id);
CREATE INDEX idx_ligne_facture        ON ligne_facture(facture_id);
CREATE INDEX idx_paiement_facture     ON paiement(facture_id);
CREATE INDEX idx_ligne_ecriture       ON ligne_ecriture(ecriture_id);
CREATE INDEX idx_transmission_facture ON transmission(facture_id);