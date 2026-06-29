package com.deepoove.swagger.diff;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deepoove.swagger.diff.compare.SpecificationDiff;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.Endpoint;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class SwaggerDiff {

    public static final String SWAGGER_VERSION_V2 = "2.0";
    public static final String OPENAPI_VERSION_V3 = "3.0";

    private static Logger logger = LoggerFactory.getLogger(SwaggerDiff.class);

    private OpenAPI oldSpecSwagger;
    private OpenAPI newSpecSwagger;

    private List<Endpoint> newEndpoints;
    private List<Endpoint> missingEndpoints;
    private List<ChangedEndpoint> changedEndpoints;

    /**
     * Compare two OpenAPI/Swagger documents (supports both Swagger 2.0 and OpenAPI 3.x).
     *
     * @param oldSpec old api-doc location: file path or HTTP URL
     * @param newSpec new api-doc location: file path or HTTP URL
     */
    public static SwaggerDiff compareV2(String oldSpec, String newSpec) {
        return compare(oldSpec, newSpec);
    }

    /**
     * Compare two OpenAPI 3.x documents.
     *
     * @param oldSpec old api-doc location: file path or HTTP URL
     * @param newSpec new api-doc location: file path or HTTP URL
     */
    public static SwaggerDiff compareV3(String oldSpec, String newSpec) {
        return compare(oldSpec, newSpec);
    }

    /**
     * Compare two OpenAPI/Swagger documents from raw JSON/YAML strings.
     */
    public static SwaggerDiff compareRaw(String oldSpec, String newSpec) {
        return new SwaggerDiff(oldSpec, newSpec, true).compare();
    }

    /** @deprecated Use compareV2 or compareV3 */
    @Deprecated
    public static SwaggerDiff compareV2Raw(String oldSpec, String newSpec) {
        return compareRaw(oldSpec, newSpec);
    }

    public static SwaggerDiff compare(String oldSpec, String newSpec) {
        return new SwaggerDiff(oldSpec, newSpec, false).compare();
    }

    // ---- Constructors ----

    /** Constructor for file path / URL */
    private SwaggerDiff(String oldSpec, String newSpec, boolean isRawContent) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPIParser parser = new OpenAPIParser();

        SwaggerParseResult oldResult = isRawContent
                ? parser.readContents(oldSpec, null, options)
                : parser.readLocation(oldSpec, null, options);
        SwaggerParseResult newResult = isRawContent
                ? parser.readContents(newSpec, null, options)
                : parser.readLocation(newSpec, null, options);

        oldSpecSwagger = oldResult.getOpenAPI();
        newSpecSwagger = newResult.getOpenAPI();

        if (oldResult.getMessages() != null && !oldResult.getMessages().isEmpty()) {
            logger.warn("Warnings parsing old spec: {}", oldResult.getMessages());
        }
        if (newResult.getMessages() != null && !newResult.getMessages().isEmpty()) {
            logger.warn("Warnings parsing new spec: {}", newResult.getMessages());
        }

        if (null == oldSpecSwagger || null == newSpecSwagger) {
            throw new RuntimeException("Cannot read api-doc from spec. " +
                    "Old: " + oldResult.getMessages() +
                    " New: " + newResult.getMessages());
        }
    }

    private SwaggerDiff compare() {
        SpecificationDiff diff = SpecificationDiff.diff(oldSpecSwagger, newSpecSwagger);
        this.newEndpoints = diff.getNewEndpoints();
        this.missingEndpoints = diff.getMissingEndpoints();
        this.changedEndpoints = diff.getChangedEndpoints();
        return this;
    }

    public List<Endpoint> getNewEndpoints() {
        return newEndpoints;
    }

    public List<Endpoint> getMissingEndpoints() {
        return missingEndpoints;
    }

    public List<ChangedEndpoint> getChangedEndpoints() {
        return changedEndpoints;
    }

    public String getOldVersion() {
        return oldSpecSwagger.getInfo() != null ? oldSpecSwagger.getInfo().getVersion() : "unknown";
    }

    public String getNewVersion() {
        return newSpecSwagger.getInfo() != null ? newSpecSwagger.getInfo().getVersion() : "unknown";
    }
}
