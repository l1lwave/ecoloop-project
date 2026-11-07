package com.ecoapp.scan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "materials")
public class Material {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private boolean recyclable;

    @Column(length = 500)
    private String instructions;

    @Column(name = "instructions_en", length = 500)
    private String instructionsEn;

    @Column(name = "instructions_ua", length = 500)
    private String instructionsUa;

    public String getCode()           { return code; }
    public void setCode(String code)  { this.code = code; }
    public String getLabel()          { return label; }
    public void setLabel(String l)    { this.label = l; }
    public boolean isRecyclable()     { return recyclable; }
    public void setRecyclable(boolean r) { this.recyclable = r; }
    public String getInstructions()   { return instructions; }
    public void setInstructions(String i) { this.instructions = i; }
    public String getInstructionsEn() { return instructionsEn; }
    public void setInstructionsEn(String i) { this.instructionsEn = i; }
    public String getInstructionsUa() { return instructionsUa; }
    public void setInstructionsUa(String i) { this.instructionsUa = i; }
}
