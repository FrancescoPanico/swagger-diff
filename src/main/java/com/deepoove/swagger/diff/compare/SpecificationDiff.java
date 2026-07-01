package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedResponse;
import com.deepoove.swagger.diff.model.Endpoint;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.headers.Header;


/**
 * Compare two OpenAPI 3.x specifications.
 *
 * @author Sayi (adapted for OAS3)
 */
@SuppressWarnings({"rawtypes"})
public class SpecificationDiff {

    private List<Endpoint> newEndpoints;
    private List<Endpoint> missingEndpoints;
    private List<ChangedEndpoint> changedEndpoints;

    private SpecificationDiff() {}

    public static SpecificationDiff diff(OpenAPI oldSpec, OpenAPI newSpec) {
        if (null == oldSpec || null == newSpec) {
            throw new IllegalArgumentException("cannot diff null spec.");
        }
        SpecificationDiff instance = new SpecificationDiff();

        Map<String, Schema> oldDefs = getSchemas(oldSpec);
        Map<String, Schema> newDefs = getSchemas(newSpec);

        Map<String, PathItem> oldPaths = oldSpec.getPaths() != null ? oldSpec.getPaths() : new HashMap<>();
        Map<String, PathItem> newPaths = newSpec.getPaths() != null ? newSpec.getPaths() : new HashMap<>();

        // Diff paths
        MapKeyDiff<String, PathItem> pathDiff = MapKeyDiff.diff(oldPaths, newPaths);
        instance.newEndpoints = convert2EndpointList(pathDiff.getIncreased());
        instance.missingEndpoints = convert2EndpointList(pathDiff.getMissing());
        instance.changedEndpoints = new ArrayList<>();

        List<String> sharedKey = pathDiff.getSharedKey();
        sharedKey.forEach(pathUrl -> {
            ChangedEndpoint changedEndpoint = new ChangedEndpoint();
            changedEndpoint.setPathUrl(pathUrl);
            PathItem oldPath = oldPaths.get(pathUrl);
            PathItem newPath = newPaths.get(pathUrl);

            // Diff operations
            Map<PathItem.HttpMethod, Operation> oldOpMap = oldPath.readOperationsMap() != null
                    ? oldPath.readOperationsMap() : new HashMap<>();
            Map<PathItem.HttpMethod, Operation> newOpMap = newPath.readOperationsMap() != null
                    ? newPath.readOperationsMap() : new HashMap<>();

            MapKeyDiff<PathItem.HttpMethod, Operation> operationDiff =
                    MapKeyDiff.diff(oldOpMap, newOpMap);

            changedEndpoint.setNewOperations(operationDiff.getIncreased());
            changedEndpoint.setMissingOperations(operationDiff.getMissing());

            Map<PathItem.HttpMethod, ChangedOperation> operas = new HashMap<>();
            operationDiff.getSharedKey().forEach(method -> {
                ChangedOperation changedOperation = new ChangedOperation();
                Operation oldOp = oldOpMap.get(method);
                Operation newOp = newOpMap.get(method);
                changedOperation.setSummary(newOp.getSummary());

                // Diff parameters (path/query/header/cookie — NOT body)
                List<Parameter> oldParams = oldOp.getParameters() != null
                        ? oldOp.getParameters() : new ArrayList<>();
                List<Parameter> newParams = newOp.getParameters() != null
                        ? newOp.getParameters() : new ArrayList<>();
                ParameterDiff paramDiff = ParameterDiff.buildWithDefinition(oldDefs, newDefs)
                        .diff(oldParams, newParams);
                changedOperation.setAddParameters(paramDiff.getIncreased());
                changedOperation.setMissingParameters(paramDiff.getMissing());
                changedOperation.setChangedParameter(paramDiff.getChanged());

                // Diff request body schema
                Schema oldRequestSchema = getRequestSchema(oldOp);
                Schema newRequestSchema = getRequestSchema(newOp);
                PropertyDiff requestPropertyDiff = PropertyDiff.buildWithDefinition(oldDefs, newDefs);
                requestPropertyDiff.diff(oldRequestSchema, newRequestSchema);
                changedOperation.setAddRequestProps(requestPropertyDiff.getIncreased());
                changedOperation.setMissingRequestProps(requestPropertyDiff.getMissing());
                changedOperation.setChangedRequestProps(requestPropertyDiff.getChanged());

                // --- LEGACY: diff schema del 200 sui campi addProps/missingProps/changedProps
                //     (mantenuto per retrocompatibilità con i test/render esistenti) ---
                Schema oldResponseSchema = getResponseSchema(oldOp);   // il vecchio helper che prende il 200
                Schema newResponseSchema = getResponseSchema(newOp);
                PropertyDiff propertyDiff = PropertyDiff.buildWithDefinition(oldDefs, newDefs);
                propertyDiff.diff(oldResponseSchema, newResponseSchema);
                changedOperation.setAddProps(propertyDiff.getIncreased());
                changedOperation.setMissingProps(propertyDiff.getMissing());
                changedOperation.setChangedProps(propertyDiff.getChanged());
                // Diff RESPONSES per status code
                ApiResponses oldResponses = oldOp.getResponses() != null
                        ? oldOp.getResponses() : new ApiResponses();
                ApiResponses newResponses = newOp.getResponses() != null
                        ? newOp.getResponses() : new ApiResponses();

                MapKeyDiff<String, ApiResponse> responseDiff = MapKeyDiff.diff(oldResponses, newResponses);

                // status aggiunti (es. nuovo 201) e rimossi (es. 404 tolto)
                changedOperation.setAddResponses(responseDiff.getIncreased());
                changedOperation.setMissingResponses(responseDiff.getMissing());

                // status presenti in entrambi: diff dello schema per ciascuno
                List<ChangedResponse> changedResponseList = new ArrayList<>();

                responseDiff.getSharedKey().forEach(statusCode -> {
                    ApiResponse oldResp = oldResponses.get(statusCode);
                    ApiResponse newResp = newResponses.get(statusCode);

                    ChangedResponse cr = new ChangedResponse();
                    cr.setStatusCode(statusCode);

                    // --- description ---
                    cr.setOldDescription(oldResp != null ? oldResp.getDescription() : null);
                    cr.setNewDescription(newResp != null ? newResp.getDescription() : null);

                    // --- schema (application/json, come prima) ---
                    Schema oldRespSchema = getResponseSchema(oldResp);
                    Schema newRespSchema = getResponseSchema(newResp);
                    PropertyDiff respPropDiff = PropertyDiff.buildWithDefinition(oldDefs, newDefs);
                    respPropDiff.diff(oldRespSchema, newRespSchema);
                    cr.setAddProps(respPropDiff.getIncreased());
                    cr.setMissingProps(respPropDiff.getMissing());
                    cr.setChangedProps(respPropDiff.getChanged());

                    // --- content-type ---
                    ListDiff<String> ctDiff = getMediaTypeDiff(
                            getContentTypes(oldResp), getContentTypes(newResp));
                    cr.setAddContentTypes(ctDiff.getIncreased());
                    cr.setMissingContentTypes(ctDiff.getMissing());

                    // --- headers ---
                    Map<String, Header> oldHeaders = getHeaders(oldResp);
                    Map<String, Header> newHeaders = getHeaders(newResp);
                    MapKeyDiff<String, Header> headerDiff = MapKeyDiff.diff(oldHeaders, newHeaders);
                    cr.setAddHeaders(headerDiff.getIncreased());
                    cr.setMissingHeaders(headerDiff.getMissing());
                    // header presenti in entrambi ma cambiati (per tipo/descrizione)
                    List<String> changedHeaderNames = new ArrayList<>();
                    headerDiff.getSharedKey().forEach(headerName -> {
                        Header oh = oldHeaders.get(headerName);
                        Header nh = newHeaders.get(headerName);
                        if (isHeaderChanged(oh, nh)) {
                            changedHeaderNames.add(headerName);
                        }
                    });
                    cr.setChangedHeaders(changedHeaderNames);

                    if (cr.isDiff()) {
                        changedResponseList.add(cr);
                    }
                });
                changedOperation.setChangedResponses(changedResponseList);

                // Diff consumes (request body content types)
                ListDiff<String> consumeDiff = getMediaTypeDiff(
                        getRequestContentTypes(oldOp), getRequestContentTypes(newOp));
                changedOperation.setAddConsumes(consumeDiff.getIncreased());
                changedOperation.setMissingConsumes(consumeDiff.getMissing());

                // Diff produces (response content types)
                ListDiff<String> producesDiff = getMediaTypeDiff(
                        getResponseContentTypes(oldOp), getResponseContentTypes(newOp));
                changedOperation.setAddProduces(producesDiff.getIncreased());
                changedOperation.setMissingProduces(producesDiff.getMissing());

                if (changedOperation.isDiff()) {
                    operas.put(method, changedOperation);
                }
            });

            changedEndpoint.setChangedOperations(operas);

            instance.newEndpoints.addAll(
                    convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getNewOperations()));
            instance.missingEndpoints.addAll(
                    convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getMissingOperations()));

            if (changedEndpoint.isDiff()) {
                instance.changedEndpoints.add(changedEndpoint);
            }
        });

        return instance;
    }

    // ---- Helpers ----

    @SuppressWarnings("unchecked")
    private static Map<String, Schema> getSchemas(OpenAPI api) {
        if (api.getComponents() == null) return new HashMap<>();
        Map<String, Schema> schemas = api.getComponents().getSchemas();
        return schemas != null ? schemas : new HashMap<>();
    }

    // TODO: remove this old  method that only manage 200 response 
    private static Schema getResponseSchema(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) return null;
        ApiResponse response = responses.get("200");
        if (response == null && !responses.isEmpty()) {
            response = responses.values().iterator().next();
        }
        if (response == null) return null;
        Content content = response.getContent();
        if (content == null || content.isEmpty()) return null;
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) mediaType = content.values().iterator().next();
        return mediaType != null ? mediaType.getSchema() : null;
    }

private static Schema getResponseSchema(ApiResponse response) {
    if (response == null) { System.out.println(">>> response NULL"); return null; }
    Content content = response.getContent();
    //System.out.println(">>> response content keys = " + (content != null ? content.keySet() : "NULL"));
    if (content == null || content.isEmpty()) return null;
    MediaType mediaType = content.get("application/json");
    if (mediaType == null) mediaType = content.values().iterator().next();
    Schema s = mediaType != null ? mediaType.getSchema() : null;
    //System.out.println(">>> response schema = " + (s != null ? s.getType() + " ref=" + s.get$ref() : "NULL"));
    return s;
}

    private static List<String> getResponseContentTypes(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) return new ArrayList<>();
        ApiResponse response = responses.get("200");
        if (response == null && !responses.isEmpty()) {
            response = responses.values().iterator().next();
        }
        if (response == null) return new ArrayList<>();
        Content content = response.getContent();
        return content != null ? new ArrayList<>(content.keySet()) : new ArrayList<>();
    }

    private static List<String> getRequestContentTypes(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) return new ArrayList<>();
        Content content = requestBody.getContent();
        return content != null ? new ArrayList<>(content.keySet()) : new ArrayList<>();
    }

    private static Schema getRequestSchema(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) return null;
        Content content = requestBody.getContent();
        if (content == null || content.isEmpty()) return null;
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) mediaType = content.values().iterator().next();
        return mediaType != null ? mediaType.getSchema() : null;
    }

    private static List<Endpoint> convert2EndpointList(Map<String, PathItem> map) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (null == map) return endpoints;
        map.forEach((url, pathItem) -> {
            Map<PathItem.HttpMethod, Operation> opMap = pathItem.readOperationsMap();
            if (opMap == null) return;
            opMap.forEach((httpMethod, operation) -> {
                Endpoint endpoint = new Endpoint();
                endpoint.setPathUrl(url);
                endpoint.setMethod(httpMethod);
                endpoint.setSummary(operation.getSummary());
                endpoint.setPath(pathItem);
                endpoint.setOperation(operation);
                endpoints.add(endpoint);
            });
        });
        return endpoints;
    }

    private static Collection<? extends Endpoint> convert2EndpointList(
            String pathUrl, Map<PathItem.HttpMethod, Operation> map) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (null == map) return endpoints;
        map.forEach((httpMethod, operation) -> {
            Endpoint endpoint = new Endpoint();
            endpoint.setPathUrl(pathUrl);
            endpoint.setMethod(httpMethod);
            endpoint.setSummary(operation.getSummary());
            endpoint.setOperation(operation);
            endpoints.add(endpoint);
        });
        return endpoints;
    }

    private static ListDiff<String> getMediaTypeDiff(List<String> oldTypes, List<String> newTypes) {
        return ListDiff.diff(oldTypes, newTypes, (t, sample) -> {
            for (String mediaType : t) {
                if (sample.equalsIgnoreCase(mediaType)) return mediaType;
            }
            return null;
        });
    }

    public List<Endpoint> getNewEndpoints() { return newEndpoints; }
    public List<Endpoint> getMissingEndpoints() { return missingEndpoints; }
    public List<ChangedEndpoint> getChangedEndpoints() { return changedEndpoints; }


    private static List<String> getContentTypes(ApiResponse response) {
        if (response == null) return new ArrayList<>();
        Content content = response.getContent();
        return content != null ? new ArrayList<>(content.keySet()) : new ArrayList<>();
    }

    private static Map<String, Header> getHeaders(ApiResponse response) {
        if (response == null || response.getHeaders() == null) return new HashMap<>();
        return response.getHeaders();
    }

    /**
     * Confronto "leggero" di due header: cambio di descrizione, deprecated o tipo schema.
     */
    private static boolean isHeaderChanged(Header oldHeader, Header newHeader) {
        if (oldHeader == null || newHeader == null) return oldHeader != newHeader;

        if (!java.util.Objects.equals(oldHeader.getDescription(), newHeader.getDescription())) return true;
        if (!java.util.Objects.equals(oldHeader.getDeprecated(), newHeader.getDeprecated())) return true;
        if (!java.util.Objects.equals(oldHeader.getRequired(), newHeader.getRequired())) return true;

        String oldType = oldHeader.getSchema() != null ? oldHeader.getSchema().getType() : null;
        String newType = newHeader.getSchema() != null ? newHeader.getSchema().getType() : null;
        if (!java.util.Objects.equals(oldType, newType)) return true;

        return false;
    }
}
