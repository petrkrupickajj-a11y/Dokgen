package cz.petrk.dokgen.entity;

/**
 * ADMIN vidi a smi vsechno vcetne spravy sablon (/sablony) a pridavani
 * dalsich uctu (/registrace). ASISTENTKA smi jen spravovat klienty a
 * generovat dokumenty - na sablony ani na spravu uctu nema pristup
 * (viz SecurityConfig).
 */
public enum Role {
    ADMIN,
    ASISTENTKA
}
