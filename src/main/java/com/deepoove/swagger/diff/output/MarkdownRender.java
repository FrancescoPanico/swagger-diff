package com.deepoove.swagger.diff.output;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.*;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

public class MarkdownRender implements Render {

    final String H3 = "### ";
    final String H2 = "## ";
    final String BLOCKQUOTE = "> ";
    final String CODE = "`";
    final String PRE_CODE = "    ";
    final String PRE_LI = "    ";
    final String LI = "* ";
    final String HR = "---\n";

    public MarkdownRender() {}

    public String render(SwaggerDiff diff) {
        String ol_new = ol_newEndpoint(diff.getNewEndpoints());
        String ol_miss = ol_missingEndpoint(diff.getMissingEndpoints());
        String ol_changed = ol_changed(diff.getChangedEndpoints());
        return renderHtml(diff.getOldVersion(), diff.getNewVersion(), ol_new, ol_miss, ol_changed);
    }

    public String renderHtml(String oldVersion, String newVersion, String ol_new,
                             String ol_miss, String ol_changed) {
        return new StringBuilder()
            .append(H2).append("Version ").append(oldVersion).append(" to ").append(newVersion).append("\n").append(HR)
            .append(H3).append("What's New").append("\n").append(HR).append(ol_new).append("\n")
            .append(H3).append("What's Deprecated").append("\n").append(HR).append(ol_miss).append("\n")
            .append(H3).append("What's Changed").append("\n").append(HR).append(ol_changed)
            .toString();
    }

    private String ol_newEndpoint(List<Endpoint> endpoints) {
        if (null == endpoints) return "";
        StringBuilder sb = new StringBuilder();
        for (Endpoint e : endpoints) {
            sb.append(li_newEndpoint(e.getMethod().name(), e.getPathUrl(), e.getSummary()));
        }
        return sb.toString();
    }

    private String li_newEndpoint(String method, String path, String desc) {
        return LI + CODE + method + CODE + " " + path + " " + (desc != null ? desc : "") + "\n";
    }

    private String ol_missingEndpoint(List<Endpoint> endpoints) {
        if (null == endpoints) return "";
        StringBuilder sb = new StringBuilder();
        for (Endpoint e : endpoints) {
            sb.append(li_newEndpoint(e.getMethod().name(), e.getPathUrl(), e.getSummary()));
        }
        return sb.toString();
    }

    private String ol_changed(List<ChangedEndpoint> changedEndpoints) {
        if (null == changedEndpoints) return "";
        StringBuilder sb = new StringBuilder();
        for (ChangedEndpoint changedEndpoint : changedEndpoints) {
            String pathUrl = changedEndpoint.getPathUrl();
            Map<PathItem.HttpMethod, ChangedOperation> changedOps = changedEndpoint.getChangedOperations();
            for (Entry<PathItem.HttpMethod, ChangedOperation> entry : changedOps.entrySet()) {
                String method = entry.getKey().name();
                ChangedOperation op = entry.getValue();
                StringBuilder detail = new StringBuilder();
                if (op.isDiffParam()) detail.append(PRE_LI).append("Parameters").append(ul_param(op));
                if (op.isDiffProp()) detail.append(PRE_LI).append("Return Type").append(ul_response(op));
                if (op.isDiffProduces()) detail.append(PRE_LI).append("Produces").append(ul_produce(op));
                if (op.isDiffConsumes()) detail.append(PRE_LI).append("Consumes").append(ul_consume(op));
                if (isDiffResponses(op)) detail.append(PRE_LI).append("Responses").append(ul_responses(op));
                sb.append(CODE).append(method).append(CODE).append(" ").append(pathUrl)
                  .append(" ").append(op.getSummary() != null ? op.getSummary() : "").append("  \n")
                  .append(detail);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    private String ul_response(ChangedOperation op) {
        StringBuilder sb = new StringBuilder("\n\n");
        for (ElProperty p : op.getAddProps()) sb.append(PRE_LI).append(PRE_CODE).append(li_addProp(p)).append("\n");
        for (ElProperty p : op.getMissingProps()) sb.append(PRE_LI).append(PRE_CODE).append(li_missingProp(p)).append("\n");
        for (ElProperty p : op.getChangedProps()) sb.append(PRE_LI).append(PRE_CODE).append(li_changedProp(p)).append("\n");
        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    private String li_missingProp(ElProperty prop) {
        Schema schema = prop.getProperty();
        String desc = schema != null ? schema.getDescription() : null;
        return "Delete " + prop.getEl() + (null == desc ? "" : " //" + desc);
    }

    @SuppressWarnings("rawtypes")
    private String li_addProp(ElProperty prop) {
        Schema schema = prop.getProperty();
        String desc = schema != null ? schema.getDescription() : null;
        return "Insert " + prop.getEl() + (null == desc ? "" : " //" + desc);
    }

    @SuppressWarnings("rawtypes")
    private String li_changedProp(ElProperty prop) {
        Schema schema = prop.getProperty();
        String desc = schema != null ? schema.getDescription() : null;
        return "Modify " + prop.getEl() + (null == desc ? "" : " //" + desc);
    }

    private String ul_param(ChangedOperation op) {
        StringBuilder sb = new StringBuilder("\n\n");
        for (Parameter p : op.getAddParameters()) sb.append(PRE_LI).append(PRE_CODE).append(li_addParam(p)).append("\n");
        for (ChangedParameter cp : op.getChangedParameter()) {
            for (ElProperty prop : cp.getIncreased()) sb.append(PRE_LI).append(PRE_CODE).append(li_addProp(prop)).append("\n");
        }
        for (ChangedParameter cp : op.getChangedParameter()) {
            if (cp.isChangeRequired() || cp.isChangeDescription()) {
                sb.append(PRE_LI).append(PRE_CODE).append(li_changedParam(cp)).append("\n");
            }
        }
        for (ChangedParameter cp : op.getChangedParameter()) {
            for (ElProperty prop : cp.getMissing()) sb.append(PRE_LI).append(PRE_CODE).append(li_missingProp(prop)).append("\n");
            for (ElProperty prop : cp.getChanged()) sb.append(PRE_LI).append(PRE_CODE).append(li_changedProp(prop)).append("\n");
        }
        for (Parameter p : op.getMissingParameters()) sb.append(PRE_LI).append(PRE_CODE).append(li_missingParam(p)).append("\n");
        return sb.toString();
    }

    private String li_addParam(Parameter param) {
        return "Add " + param.getName() + (null == param.getDescription() ? "" : " //" + param.getDescription());
    }

    private String li_missingParam(Parameter param) {
        return "Delete " + param.getName() + (null == param.getDescription() ? "" : " //" + param.getDescription());
    }

    private String li_changedParam(ChangedParameter cp) {
        Parameter right = cp.getRightParameter();
        Parameter left = cp.getLeftParameter();
        StringBuilder sb = new StringBuilder(right.getName());
        if (cp.isChangeRequired()) {
            sb.append(" change into ").append(Boolean.TRUE.equals(right.getRequired()) ? "required" : "not required");
        }
        if (cp.isChangeDescription()) {
            sb.append(" Notes ").append(left.getDescription()).append(" change into ").append(right.getDescription());
        }
        return sb.toString();
    }

    private String ul_produce(ChangedOperation op) {
        StringBuilder sb = new StringBuilder("\n\n");
        for (String mt : op.getAddProduces()) sb.append(PRE_LI).append(PRE_CODE).append("Insert ").append(mt).append("\n");
        for (String mt : op.getMissingProduces()) sb.append(PRE_LI).append(PRE_CODE).append("Delete ").append(mt).append("\n");
        return sb.toString();
    }

    private String ul_consume(ChangedOperation op) {
        StringBuilder sb = new StringBuilder("\n\n");
        for (String mt : op.getAddConsumes()) sb.append(PRE_LI).append(PRE_CODE).append("Insert ").append(mt).append("\n");
        for (String mt : op.getMissingConsumes()) sb.append(PRE_LI).append(PRE_CODE).append("Delete ").append(mt).append("\n");
        return sb.toString();
    }

    private boolean isDiffResponses(ChangedOperation op) {
        return !op.getAddResponses().isEmpty()
                || !op.getMissingResponses().isEmpty()
                || !op.getChangedResponses().isEmpty();
    }

    private String ul_responses(ChangedOperation op) {
        StringBuilder sb = new StringBuilder("\n\n");

        // status aggiunti
        for (Map.Entry<String, io.swagger.v3.oas.models.responses.ApiResponse> e : op.getAddResponses().entrySet()) {
            String desc = e.getValue() != null ? e.getValue().getDescription() : null;
            sb.append(PRE_LI).append(PRE_CODE)
            .append("Insert response ").append(e.getKey())
            .append(null == desc ? "" : " //" + desc)
            .append("\n");
        }

        // status rimossi
        for (Map.Entry<String, io.swagger.v3.oas.models.responses.ApiResponse> e : op.getMissingResponses().entrySet()) {
            String desc = e.getValue() != null ? e.getValue().getDescription() : null;
            sb.append(PRE_LI).append(PRE_CODE)
            .append("Delete response ").append(e.getKey())
            .append(null == desc ? "" : " //" + desc)
            .append("\n");
        }

        // status modificati
        for (ChangedResponse cr : op.getChangedResponses()) {
            sb.append(PRE_LI).append(PRE_CODE)
            .append("Modify response ").append(cr.getStatusCode()).append("\n");

            // description
            if (cr.isDescriptionChanged()) {
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE)
                .append("Description ")
                .append(cr.getOldDescription() != null ? cr.getOldDescription() : "")
                .append(" change into ")
                .append(cr.getNewDescription() != null ? cr.getNewDescription() : "")
                .append("\n");
            }

            // schema (proprietà)
            for (ElProperty p : cr.getAddProps())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append(li_addProp(p)).append("\n");
            for (ElProperty p : cr.getMissingProps())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append(li_missingProp(p)).append("\n");
            for (ElProperty p : cr.getChangedProps())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append(li_changedProp(p)).append("\n");

            // content-type
            for (String ct : cr.getAddContentTypes())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append("Insert content-type ").append(ct).append("\n");
            for (String ct : cr.getMissingContentTypes())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append("Delete content-type ").append(ct).append("\n");

            // headers
            for (String h : cr.getAddHeaders().keySet())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append("Insert header ").append(h).append("\n");
            for (String h : cr.getMissingHeaders().keySet())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append("Delete header ").append(h).append("\n");
            for (String h : cr.getChangedHeaders())
                sb.append(PRE_LI).append(PRE_CODE).append(PRE_CODE).append("Modify header ").append(h).append("\n");
        }

        return sb.toString();
    }
}
