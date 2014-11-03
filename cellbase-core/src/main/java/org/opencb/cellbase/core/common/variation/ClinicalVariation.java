package org.opencb.cellbase.core.common.variation;

import org.opencb.biodata.formats.variant.clinvar.ClinvarPublicSet;
import org.opencb.biodata.models.variant.clinical.Cosmic;
import org.opencb.biodata.models.variant.clinical.Gwas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parce on 10/31/14.
 */
public class ClinicalVariation {

    private String chromosome;
    private int start;
    private int end;
    private String reference;
    private String alternate;
    private List<ClinvarPublicSet> clinvarList;
    private List<Cosmic> cosmicList;
    private List<Gwas> gwasList;

    public ClinicalVariation(String chromosome, int start, int end, String reference, String alternate) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.reference = reference;
        this.alternate = alternate;
    }

    public ClinicalVariation(ClinvarPublicSet clinvarSet) {
        this(clinvarSet.getChromosome(), clinvarSet.getStart(), clinvarSet.getEnd(), clinvarSet.getReference(), clinvarSet.getAlternate());
        this.clinvarList = new ArrayList<>();
        this.clinvarList.add(clinvarSet);
    }

    public ClinicalVariation(Cosmic cosmic) {
        this(cosmic.getChromosome(), cosmic.getStart(), cosmic.getEnd(), cosmic.getReference(), cosmic.getAlternate());
        this.cosmicList = new ArrayList<>();
        this.cosmicList.add(cosmic);
    }

    public ClinicalVariation(Gwas gwas) {
        this(gwas.getChromosome(), gwas.getStart(), gwas.getEnd(), gwas.getReference(), gwas.getAlternate());
        this.gwasList = new ArrayList<>();
        this.gwasList.add(gwas);
    }


    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAlternate() {
        return alternate;
    }

    public void setAlternate(String alternate) {
        this.alternate = alternate;
    }

    public void addClinvar(ClinvarPublicSet clinvar) {
        if (clinvarList == null) {
            clinvarList = new ArrayList<>();
        }
        clinvarList.add(clinvar);
    }

    public void addCosmic(Cosmic cosmic) {
        if (cosmicList == null) {
            cosmicList = new ArrayList<>();
        }
        cosmicList.add(cosmic);
    }

    public void addGwas(Gwas gwas) {
        if (gwasList == null) {
            gwasList = new ArrayList<>();
        }
        gwasList.add(gwas);
    }
}
