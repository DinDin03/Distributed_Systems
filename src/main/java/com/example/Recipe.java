package com.example;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
class Recipe {
    @JsonProperty("recipe_id")
    private String recipeId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("prep_time_minutes")
    private Integer prepTimeMinutes;

    @JsonProperty("cook_time_minutes")
    private Integer cookTimeMinutes;

    @JsonProperty("servings")
    private Integer servings;

    @JsonProperty("instructions")
    private List<String> instructions;

    @JsonProperty("estimated_rating")
    private Double estimatedRating;

    public Recipe() {}

    public Recipe(String recipeId, String title, Integer prepTimeMinutes,
                  Integer cookTimeMinutes, Integer servings, List<String> instructions,
                  Double estimatedRating) {
        this.recipeId = recipeId;
        this.title = title;
        this.prepTimeMinutes = prepTimeMinutes;
        this.cookTimeMinutes = cookTimeMinutes;
        this.servings = servings;
        this.instructions = instructions;
        this.estimatedRating = estimatedRating;
    }

    public String getTitle() { return title; }

    @Override
    public String toString() {
        return String.format("com.example.Recipe{id='%s', title='%s', prepTime=%d, cookTime=%d, servings=%d, rating=%.1f}",
                recipeId, title, prepTimeMinutes, cookTimeMinutes, servings, estimatedRating);
    }
}