package com.example;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class RecipeRequest {
    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("preferences")
    private Preferences preferences;

    @JsonProperty("ingredients")
    private List<Ingredient> ingredients;

    public RecipeRequest() {}

    public RecipeRequest(String username, String email, Preferences preferences,
                         List<Ingredient> ingredients) {
        this.username = username;
        this.email = email;
        this.preferences = preferences;
        this.ingredients = ingredients;
    }

    public String getUsername() { return username; }

    public String getEmail() { return email; }

    public Preferences getPreferences() { return preferences; }

    public List<Ingredient> getIngredients() { return ingredients; }

    @Override
    public String toString() {
        return String.format("com.example.RecipeRequest{username='%s', email='%s', ingredients=%d items}",
                username, email, ingredients != null ? ingredients.size() : 0);
    }
}