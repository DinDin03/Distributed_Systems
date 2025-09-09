package com.example;
import com.fasterxml.jackson.annotation.JsonProperty;

class Ingredient {
    @JsonProperty("ingredient_name")
    private String ingredientName;

    @JsonProperty("ingredient_qty")
    private Integer ingredientQty;

    public Ingredient() {}

    public Ingredient(String ingredientName, Integer ingredientQty) {
        this.ingredientName = ingredientName;
        this.ingredientQty = ingredientQty;
    }

    public Ingredient(String ingredientName) {
        this.ingredientName = ingredientName;
        this.ingredientQty = null;
    }

    public String getIngredientName() { return ingredientName; }

    public Integer getIngredientQty() { return ingredientQty; }

    @Override
    public String toString() {
        return String.format("com.example.Ingredient{name='%s', qty=%s}", ingredientName, ingredientQty);
    }
}