package com.weathersystem.shared.exceptions;

import java.util.List;
import java.util.ArrayList;

public class DataValidationException extends WeatherSystemException {

    private final List<String> validationErrors;
    private final String fieldName;

    public DataValidationException(String fieldName, String validationError) {
        super("DATA_VALIDATION_ERROR", "Validation failed for field '" + fieldName + "': " + validationError);
        this.fieldName = fieldName;
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(validationError);
    }

    public DataValidationException(String fieldName, List<String> validationErrors) {
        super("DATA_VALIDATION_ERROR", "Validation failed for field '" + fieldName + "': " +
                String.join(", ", validationErrors));
        this.fieldName = fieldName;
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public DataValidationException(List<String> validationErrors) {
        super("DATA_VALIDATION_ERROR", "Data validation failed: " + String.join(", ", validationErrors));
        this.fieldName = null;
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    public boolean hasFieldSpecificError() {
        return fieldName != null;
    }

    public void addValidationError(String error) {
        validationErrors.add(error);
    }

    public int getErrorCount() {
        return validationErrors.size();
    }
}