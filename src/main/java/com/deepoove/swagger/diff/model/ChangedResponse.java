package com.deepoove.swagger.diff.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.swagger.v3.oas.models.headers.Header;

public class ChangedResponse {

    private String statusCode;

    // --- description ---
    private String oldDescription;
    private String newDescription;

    // --- schema (proprietà del body) ---
    private List<ElProperty> addProps = new ArrayList<>();
    private List<ElProperty> missingProps = new ArrayList<>();
    private List<ElProperty> changedProps = new ArrayList<>();

    // --- content-type (media types) ---
    private List<String> addContentTypes = new ArrayList<>();
    private List<String> missingContentTypes = new ArrayList<>();

    // --- headers ---
    private Map<String, Header> addHeaders = new LinkedHashMap<>();
    private Map<String, Header> missingHeaders = new LinkedHashMap<>();
    private List<String> changedHeaders = new ArrayList<>(); // nomi degli header cambiati

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    // --- description ---
    public String getOldDescription() { return oldDescription; }
    public void setOldDescription(String oldDescription) { this.oldDescription = oldDescription; }

    public String getNewDescription() { return newDescription; }
    public void setNewDescription(String newDescription) { this.newDescription = newDescription; }

    public boolean isDescriptionChanged() {
        return !Objects.equals(oldDescription, newDescription);
    }

    // --- schema ---
    public List<ElProperty> getAddProps() { return addProps; }
    public void setAddProps(List<ElProperty> addProps) {
        this.addProps = addProps != null ? addProps : new ArrayList<>();
    }

    public List<ElProperty> getMissingProps() { return missingProps; }
    public void setMissingProps(List<ElProperty> missingProps) {
        this.missingProps = missingProps != null ? missingProps : new ArrayList<>();
    }

    public List<ElProperty> getChangedProps() { return changedProps; }
    public void setChangedProps(List<ElProperty> changedProps) {
        this.changedProps = changedProps != null ? changedProps : new ArrayList<>();
    }

    public boolean isSchemaChanged() {
        return !addProps.isEmpty() || !missingProps.isEmpty() || !changedProps.isEmpty();
    }

    // --- content-type ---
    public List<String> getAddContentTypes() { return addContentTypes; }
    public void setAddContentTypes(List<String> addContentTypes) {
        this.addContentTypes = addContentTypes != null ? addContentTypes : new ArrayList<>();
    }

    public List<String> getMissingContentTypes() { return missingContentTypes; }
    public void setMissingContentTypes(List<String> missingContentTypes) {
        this.missingContentTypes = missingContentTypes != null ? missingContentTypes : new ArrayList<>();
    }

    public boolean isContentTypeChanged() {
        return !addContentTypes.isEmpty() || !missingContentTypes.isEmpty();
    }

    // --- headers ---
    public Map<String, Header> getAddHeaders() { return addHeaders; }
    public void setAddHeaders(Map<String, Header> addHeaders) {
        this.addHeaders = addHeaders != null ? addHeaders : new LinkedHashMap<>();
    }

    public Map<String, Header> getMissingHeaders() { return missingHeaders; }
    public void setMissingHeaders(Map<String, Header> missingHeaders) {
        this.missingHeaders = missingHeaders != null ? missingHeaders : new LinkedHashMap<>();
    }

    public List<String> getChangedHeaders() { return changedHeaders; }
    public void setChangedHeaders(List<String> changedHeaders) {
        this.changedHeaders = changedHeaders != null ? changedHeaders : new ArrayList<>();
    }

    public boolean isHeadersChanged() {
        return !addHeaders.isEmpty() || !missingHeaders.isEmpty() || !changedHeaders.isEmpty();
    }

    // --- diff globale ---
    public boolean isDiff() {
        return isDescriptionChanged()
                || isSchemaChanged()
                || isContentTypeChanged()
                || isHeadersChanged();
    }
}