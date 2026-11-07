package com.ecoapp.scan.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsResponse {

    /** OFF API: 1 = found, 0 = not found */
    @JsonProperty("status")
    private int status;

    @JsonProperty("product")
    private OFFProduct product;

    public boolean isFound() { return status == 1 && product != null; }
    public OFFProduct getProduct() { return product; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OFFProduct {

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("product_name_nl")
        private String productNameNl;

        @JsonProperty("product_name_en")
        private String productNameEn;

        /** OFF uses ISO 639-1 code 'uk' for Ukrainian */
        @JsonProperty("product_name_uk")
        private String productNameUk;

        @JsonProperty("brands")
        private String brands;

        /** Human-readable categories string, e.g. "Exotische vruchtenfrisdranken" */
        @JsonProperty("categories")
        private String categories;

        /** Machine-readable category tags, e.g. ["en:sodas", "en:carbonated-drinks"] */
        @JsonProperty("categories_tags")
        private List<String> categoriesTags;

        /** Packaging text description */
        @JsonProperty("packaging_text")
        private String packagingText;

        /** Material tags, e.g. ["en:pet-1-polyethylene-terephthalate", "en:plastic"] */
        @JsonProperty("packaging_materials_tags")
        private List<String> packagingMaterialsTags;

        @JsonProperty("quantity")
        private String quantity;

        public String getLocalizedName(String lang) {
            return switch (lang) {
                case "en" -> firstNonBlank(productNameEn, productName, productNameNl, productNameUk);
                case "ua" -> firstNonBlank(productNameUk, productName, productNameEn, productNameNl);
                default   -> firstNonBlank(productNameNl, productName, productNameEn, productNameUk);
            };
        }

        private static String firstNonBlank(String... candidates) {
            for (String s : candidates) {
                if (s != null && !s.isBlank()) return s;
            }
            return "Onbekend product";
        }

        public String getBrands()                          { return brands; }
        public String getCategories()                      { return categories; }
        public List<String> getCategoriesTags()            { return categoriesTags; }
        public String getPackagingText()                   { return packagingText; }
        public List<String> getPackagingMaterialsTags()    { return packagingMaterialsTags; }
        public String getQuantity()                        { return quantity; }
    }
}
