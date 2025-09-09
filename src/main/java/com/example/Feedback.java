package com.example;
import com.fasterxml.jackson.annotation.JsonProperty;

class Feedback {
    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("recipe")
    private Recipe recipe;

    @JsonProperty("rating")
    private Integer rating;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("cooking_notes")
    private String cookingNotes;

    @JsonProperty("would_make_again")
    private Boolean wouldMakeAgain;

    public Feedback() {}

    public Feedback(String username, String email, Recipe recipe, Integer rating,
                    String comments, String cookingNotes, Boolean wouldMakeAgain) {
        this.username = username;
        this.email = email;
        this.recipe = recipe;
        this.rating = rating;
        this.comments = comments;
        this.cookingNotes = cookingNotes;
        this.wouldMakeAgain = wouldMakeAgain;
    }

    @Override
    public String toString() {
        return String.format("com.example.Feedback{user='%s', recipe='%s', rating=%d, wouldMakeAgain=%s}",
                username, recipe != null ? recipe.getTitle() : "null", rating, wouldMakeAgain);
    }
}