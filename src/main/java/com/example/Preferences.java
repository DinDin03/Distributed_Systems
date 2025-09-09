package com.example;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class Preferences {
    @JsonProperty("dietary_restrictions")
    private List<String> dietaryRestrictions;

    @JsonProperty("allergies")
    private List<String> allergies;

    @JsonProperty("cuisine_preference")
    private String cuisinePreference;

    @JsonProperty("spice_level")
    private String spiceLevel;

    public Preferences() {}

    public Preferences(List<String> dietaryRestrictions, List<String> allergies,
                       String cuisinePreference, String spiceLevel) {
        this.dietaryRestrictions = dietaryRestrictions;
        this.allergies = allergies;
        this.cuisinePreference = cuisinePreference;
        this.spiceLevel = spiceLevel;
    }

    public String getCuisinePreference() { return cuisinePreference; }

    @Override
    public String toString() {
        return String.format("com.example.Preferences{dietary=%s, allergies=%s, cuisine='%s', spice='%s'}",
                dietaryRestrictions, allergies, cuisinePreference, spiceLevel);
    }
}