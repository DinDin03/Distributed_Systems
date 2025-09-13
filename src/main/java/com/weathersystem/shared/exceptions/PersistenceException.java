package com.weathersystem.shared.exceptions;

public class PersistenceException extends WeatherSystemException {

    private final String filePath;
    private final PersistenceOperation operation;

    public enum PersistenceOperation {
        READ, WRITE, DELETE, BACKUP, RESTORE
    }

    public PersistenceException(PersistenceOperation operation, String filePath, String message) {
        super("PERSISTENCE_ERROR", operation.name() + " operation failed for '" + filePath + "': " + message);
        this.operation = operation;
        this.filePath = filePath;
    }

    public PersistenceException(PersistenceOperation operation, String filePath, String message, Throwable cause) {
        super("PERSISTENCE_ERROR", operation.name() + " operation failed for '" + filePath + "': " + message, cause);
        this.operation = operation;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public PersistenceOperation getOperation() {
        return operation;
    }

    public static PersistenceException readError(String filePath, String message) {
        return new PersistenceException(PersistenceOperation.READ, filePath, message);
    }

    public static PersistenceException readError(String filePath, Throwable cause) {
        return new PersistenceException(PersistenceOperation.READ, filePath, cause.getMessage(), cause);
    }

    public static PersistenceException writeError(String filePath, String message) {
        return new PersistenceException(PersistenceOperation.WRITE, filePath, message);
    }

    public static PersistenceException writeError(String filePath, Throwable cause) {
        return new PersistenceException(PersistenceOperation.WRITE, filePath, cause.getMessage(), cause);
    }

    public static PersistenceException backupError(String filePath, String message) {
        return new PersistenceException(PersistenceOperation.BACKUP, filePath, message);
    }

    public static PersistenceException restoreError(String filePath, String message) {
        return new PersistenceException(PersistenceOperation.RESTORE, filePath, message);
    }
}