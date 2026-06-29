package com.deepoove.swagger.diff.model;

import java.util.Map;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

public class ChangedEndpoint implements Changed {

    private String pathUrl;

    private Map<PathItem.HttpMethod, Operation> newOperations;
    private Map<PathItem.HttpMethod, Operation> missingOperations;

    private Map<PathItem.HttpMethod, ChangedOperation> changedOperations;

    public Map<PathItem.HttpMethod, Operation> getNewOperations() {
        return newOperations;
    }

    public void setNewOperations(Map<PathItem.HttpMethod, Operation> newOperations) {
        this.newOperations = newOperations;
    }

    public Map<PathItem.HttpMethod, Operation> getMissingOperations() {
        return missingOperations;
    }

    public void setMissingOperations(Map<PathItem.HttpMethod, Operation> missingOperations) {
        this.missingOperations = missingOperations;
    }

    public Map<PathItem.HttpMethod, ChangedOperation> getChangedOperations() {
        return changedOperations;
    }

    public void setChangedOperations(Map<PathItem.HttpMethod, ChangedOperation> changedOperations) {
        this.changedOperations = changedOperations;
    }

    public String getPathUrl() {
        return pathUrl;
    }

    public void setPathUrl(String pathUrl) {
        this.pathUrl = pathUrl;
    }

    public boolean isDiff() {
        return !changedOperations.isEmpty();
    }

}
