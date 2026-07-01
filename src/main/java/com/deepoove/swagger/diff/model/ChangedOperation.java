package com.deepoove.swagger.diff.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;

import io.swagger.v3.oas.models.responses.ApiResponse;

public class ChangedOperation implements Changed {

    private String summary;

    private List<io.swagger.v3.oas.models.parameters.Parameter> addParameters = new ArrayList<>();
    private List<io.swagger.v3.oas.models.parameters.Parameter> missingParameters = new ArrayList<>();

    private List<ChangedParameter> changedParameter = new ArrayList<>();

    private List<ElProperty> addProps = new ArrayList<>();
    private List<ElProperty> missingProps = new ArrayList<>();
    private List<ElProperty> changedProps = new ArrayList<>();

    private List<ElProperty> addRequestProps = new ArrayList<>();
    private List<ElProperty> missingRequestProps = new ArrayList<>();
    private List<ElProperty> changedRequestProps = new ArrayList<>();

    private List<String> addConsumes = new ArrayList<>();
    private List<String> missingConsumes = new ArrayList<>();
    private List<String> addProduces = new ArrayList<>();
    private List<String> missingProduces = new ArrayList<>();

    public List<io.swagger.v3.oas.models.parameters.Parameter> getAddParameters() {
        return addParameters;
    }

    public void setAddParameters(List<io.swagger.v3.oas.models.parameters.Parameter> addParameters) {
        this.addParameters = addParameters;
    }

    public List<io.swagger.v3.oas.models.parameters.Parameter> getMissingParameters() {
        return missingParameters;
    }

    public void setMissingParameters(List<io.swagger.v3.oas.models.parameters.Parameter> missingParameters) {
        this.missingParameters = missingParameters;
    }

    public List<ChangedParameter> getChangedParameter() {
        return changedParameter;
    }

    public void setChangedParameter(List<ChangedParameter> changedParameter) {
        this.changedParameter = changedParameter;
    }

    public List<ElProperty> getAddProps() {
        return addProps;
    }

    public void setAddProps(List<ElProperty> addProps) {
        this.addProps = addProps;
    }

    public List<ElProperty> getMissingProps() {
        return missingProps;
    }

    public void setMissingProps(List<ElProperty> missingProps) {
        this.missingProps = missingProps;
    }

    public List<ElProperty> getChangedProps() {
        return changedProps;
    }

    public void setChangedProps(List<ElProperty> changedProps) {
        this.changedProps = changedProps;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isDiff() {
        return !addParameters.isEmpty() || !missingParameters.isEmpty() || !changedParameter.isEmpty()
                || isDiffProp() || isDiffRequestProp() || isDiffConsumes() || isDiffProduces()|| !addResponses.isEmpty()
            || !missingResponses.isEmpty() || !changedResponses.isEmpty();
    }


    public boolean isDiffProp() {
        return !addProps.isEmpty() || !missingProps.isEmpty() || !changedProps.isEmpty();
    }

    public boolean isDiffRequestProp() {
        return !addRequestProps.isEmpty() || !missingRequestProps.isEmpty() || !changedRequestProps.isEmpty();
    }

    public boolean isDiffParam() {
        return !addParameters.isEmpty() || !missingParameters.isEmpty() || !changedParameter.isEmpty();
    }

    public boolean isDiffConsumes() {
        return !addConsumes.isEmpty() || !missingConsumes.isEmpty();
    }

    public boolean isDiffProduces() {
        return !addProduces.isEmpty() || !missingProduces.isEmpty();
    }

    public List<String> getAddConsumes() {
        return this.addConsumes;
    }

    public void setAddConsumes(List<String> increased) {
        this.addConsumes = increased == null ? new ArrayList<>() : increased;
    }

    public List<String> getMissingConsumes() {
        return this.missingConsumes;
    }

    public void setMissingConsumes(List<String> missing) {
        this.missingConsumes = missing == null ? new ArrayList<>() : missing;
    }

    public List<String> getAddProduces() {
        return this.addProduces;
    }

    public void setAddProduces(List<String> increased) {
        this.addProduces = increased == null ? new ArrayList<>() : increased;
    }

    public List<String> getMissingProduces() {
        return this.missingProduces;
    }

    public void setMissingProduces(List<String> missing) {
        this.missingProduces = missing == null ? new ArrayList<>() : missing;
    }

    public List<ElProperty> getAddRequestProps() {
        return addRequestProps;
    }

    public void setAddRequestProps(List<ElProperty> addRequestProps) {
        this.addRequestProps = addRequestProps;
    }

    public List<ElProperty> getMissingRequestProps() {
        return missingRequestProps;
    }

    public void setMissingRequestProps(List<ElProperty> missingRequestProps) {
        this.missingRequestProps = missingRequestProps;
    }

    public List<ElProperty> getChangedRequestProps() {
        return changedRequestProps;
    }

    public void setChangedRequestProps(List<ElProperty> changedRequestProps) {
        this.changedRequestProps = changedRequestProps;
    }

    // --- Response diffs per status code ---
    private Map<String, ApiResponse> addResponses = new LinkedHashMap<>();
    private Map<String, ApiResponse> missingResponses = new LinkedHashMap<>();
    // status presenti in entrambi ma con schema cambiato
    private List<ChangedResponse> changedResponses = new ArrayList<>();

    public Map<String, ApiResponse> getAddResponses() { return addResponses; }
    public void setAddResponses(Map<String, ApiResponse> addResponses) {
        this.addResponses = addResponses != null ? addResponses : new LinkedHashMap<>();
    }

    public Map<String, ApiResponse> getMissingResponses() { return missingResponses; }
    public void setMissingResponses(Map<String, ApiResponse> missingResponses) {
        this.missingResponses = missingResponses != null ? missingResponses : new LinkedHashMap<>();
    }

    public List<ChangedResponse> getChangedResponses() { return changedResponses; }
    public void setChangedResponses(List<ChangedResponse> changedResponses) {
        this.changedResponses = changedResponses != null ? changedResponses : new ArrayList<>();
    }
}