package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.deepoove.swagger.diff.model.ChangedParameter;
import com.deepoove.swagger.diff.model.ElProperty;
import com.google.common.collect.Lists;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * Compare two lists of OAS3 parameters (path/query/header/cookie only;
 * request body is handled separately in SpecificationDiff).
 *
 * @author Sayi (adapted for OAS3)
 */
@SuppressWarnings({"rawtypes"})
public class ParameterDiff {

    private List<Parameter> increased;
    private List<Parameter> missing;
    private List<ChangedParameter> changed;

    Map<String, Schema> oldDefinitions;
    Map<String, Schema> newDefinitions;

    private ParameterDiff() {
        this.increased = new ArrayList<>();
        this.missing = new ArrayList<>();
        this.changed = new ArrayList<>();
    }

    public static ParameterDiff buildWithDefinition(Map<String, Schema> left, Map<String, Schema> right) {
        ParameterDiff diff = new ParameterDiff();
        diff.oldDefinitions = left != null ? left : new HashMap<>();
        diff.newDefinitions = right != null ? right : new HashMap<>();
        return diff;
    }

    public ParameterDiff diff(List<Parameter> left, List<Parameter> right) {
        if (null == left) left = new ArrayList<>();
        if (null == right) right = new ArrayList<>();

        ListDiff<Parameter> paramDiff = ListDiff.diff(left, right, (t, param) -> {
            for (Parameter para : t) {
                if (param.getName() != null && param.getName().equals(para.getName())) return para;
            }
            return null;
        });
        this.increased.addAll(paramDiff.getIncreased());
        this.missing.addAll(paramDiff.getMissing());

        Map<Parameter, Parameter> shared = paramDiff.getShared();
        shared.forEach((leftPara, rightPara) -> {
            ChangedParameter changedParameter = new ChangedParameter();
            changedParameter.setLeftParameter(leftPara);
            changedParameter.setRightParameter(rightPara);

            // Compare schema type changes (replaces AbstractSerializableParameter check)
            Schema leftSchema = leftPara.getSchema();
            Schema rightSchema = rightPara.getSchema();
            if (leftSchema != null && rightSchema != null) {
                String leftType = leftSchema.getType();
                String rightType = rightSchema.getType();
                if (leftType != null && !leftType.equals(rightType)) {
                    ElProperty elProperty = new ElProperty();
                    elProperty.setEl(rightPara.getName());
                    elProperty.setProperty(rightSchema);
                    changedParameter.setChanged(Lists.newArrayList(elProperty));
                }
            }

            // required
            boolean rightRequired = Boolean.TRUE.equals(rightPara.getRequired());
            boolean leftRequired = Boolean.TRUE.equals(leftPara.getRequired());
            changedParameter.setChangeRequired(leftRequired != rightRequired);

            // description
            String description = rightPara.getDescription();
            String oldDescription = leftPara.getDescription();
            if (StringUtils.isBlank(description)) description = "";
            if (StringUtils.isBlank(oldDescription)) oldDescription = "";
            changedParameter.setChangeDescription(!description.equals(oldDescription));

            if (changedParameter.isDiff()) {
                this.changed.add(changedParameter);
            }
        });

        return this;
    }

    public List<Parameter> getIncreased() { return increased; }
    public void setIncreased(List<Parameter> increased) { this.increased = increased; }

    public List<Parameter> getMissing() { return missing; }
    public void setMissing(List<Parameter> missing) { this.missing = missing; }

    public List<ChangedParameter> getChanged() { return changed; }
    public void setChanged(List<ChangedParameter> changed) { this.changed = changed; }
}
