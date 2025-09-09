package com.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;

public class Question3 {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        try {
            JsonSerialisation();
            JsonDeserialisation();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void JsonSerialisation() throws Exception {

        Preferences preferences = new Preferences(
                List.of("vegetarian"),
                List.of("nuts"),
                "high protein",
                "medium"
        );

        List<Ingredient> ingredients = Arrays.asList(
                new Ingredient("chicken breast", 500),
                new Ingredient("tomatoes", 200),
                new Ingredient("basil"),
                new Ingredient("olive oil")
        );

        RecipeRequest request = new RecipeRequest(
                "dineth_katanwala",
                "a1868070@adelaide.edu.au",
                preferences,
                ingredients
        );

        String jsonString = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(request);

        System.out.println(jsonString);

        String preferencesJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(preferences);
        System.out.println(preferencesJson);
    }

    private static void JsonDeserialisation() throws Exception {

        String recipeRequestJson = """
        {
         "username": "dineth_katanwala",
         "email": "a1868070@adelaide.edu.au",
         "preferences": {
           "dietary_restrictions": ["N/A"],
           "allergies": ["nuts"],
           "cuisine_preference": "high protein",
           "spice_level": "high"
         },
         "ingredients": [
           {
             "ingredient_name": "chicken breast",
             "ingredient_qty": 500
           },
           {
             "ingredient_name": "tomatoes",
             "ingredient_qty": 200
           },
           {
             "ingredient_name": "butter"
           }
         ]
        }
    """;

        RecipeRequest deserialisedRequest = objectMapper.readValue(recipeRequestJson, RecipeRequest.class);

        System.out.println("Username: " + deserialisedRequest.getUsername());
        System.out.println("Email: " + deserialisedRequest.getEmail());
        System.out.println("Cuisine Preference: " + deserialisedRequest.getPreferences().getCuisinePreference());
        System.out.println("Number of ingredients: " + deserialisedRequest.getIngredients().size());

        System.out.println("\nIngredient details:");
        for (Ingredient ingredient : deserialisedRequest.getIngredients()) {
            System.out.println("  - " + ingredient.getIngredientName() +
                    (ingredient.getIngredientQty() != null ?
                            " (" + ingredient.getIngredientQty() + "g)" : " (no quantity)"));
        }

    }

}