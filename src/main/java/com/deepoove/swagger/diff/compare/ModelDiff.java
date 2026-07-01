package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.deepoove.swagger.diff.model.ElProperty;

import io.swagger.v3.oas.models.media.Schema;

/**
 * compare two OAS3 Schema (replaces Model + Property from Swagger 2.0)
 *
 * @author Sayi (adapted for OAS3)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelDiff {

    private List<ElProperty> increased;
    private List<ElProperty> missing;
    private List<ElProperty> changed;

    Map<String, Schema> oldDefinitions;
    Map<String, Schema> newDefinitions;

    private ModelDiff() {
        increased = new ArrayList<>();
        missing = new ArrayList<>();
        changed = new ArrayList<>();
    }

    public static ModelDiff buildWithDefinition(Map<String, Schema> left, Map<String, Schema> right) {
        ModelDiff diff = new ModelDiff();
        diff.oldDefinitions = left != null ? left : new HashMap<>();
        diff.newDefinitions = right != null ? right : new HashMap<>();
        return diff;
    }

    public ModelDiff diff(Schema leftModel, Schema rightModel) {
        return this.diff(leftModel, rightModel, null, new HashSet<>());
    }

    public ModelDiff diff(Schema leftModel, Schema rightModel, String parentEl) {
        return this.diff(leftModel, rightModel, parentEl, new HashSet<>());
    }

    /** Entry point when comparing two property-level schemas (resolves $ref before diffing) */
    public ModelDiff diffSchema(Schema leftProperty, Schema rightProperty) {
        return this.diff(resolveRef(leftProperty, oldDefinitions),
                resolveRef(rightProperty, newDefinitions));
    }

    private ModelDiff diff(Schema leftInputModel, Schema rightInputModel,
                           String parentEl, Set<Schema> visited) {
        if ((null == leftInputModel && null == rightInputModel)
                || visited.contains(leftInputModel)
                || visited.contains(rightInputModel)) {
            return this;
        }

        Schema leftModel = resolveRef(leftInputModel, oldDefinitions);
        Schema rightModel = resolveRef(rightInputModel, newDefinitions);

        while (leftModel != null && leftModel.getItems() != null && rightModel != null && rightModel.getItems() != null) {
            leftModel = resolveRef(leftModel.getItems(), oldDefinitions);
            rightModel = resolveRef(rightModel.getItems(), newDefinitions);
        }

        Map<String, Schema> leftProperties = leftModel == null ? null : leftModel.getProperties();
        Map<String, Schema> rightProperties = rightModel == null ? null : rightModel.getProperties();

        MapKeyDiff<String, Schema> propertyDiff = MapKeyDiff.diff(leftProperties, rightProperties);

        increased.addAll(convert2ElProperties(propertyDiff.getIncreased(), parentEl));
        missing.addAll(convert2ElProperties(propertyDiff.getMissing(), parentEl));

        Set<Schema> newVisited = copyAndAdd(visited, leftModel, rightModel);

        List<String> sharedKey = propertyDiff.getSharedKey();
        sharedKey.forEach(key -> {
            Schema left = leftProperties.get(key);
            Schema right = rightProperties.get(key);

            // Try to resolve as sub-model (ref or object with properties)
            Schema leftSub = resolveToSubModel(left, oldDefinitions);
            Schema rightSub = resolveToSubModel(right, newDefinitions);

            if (leftSub != null || rightSub != null) {
                diff(leftSub, rightSub, buildElString(parentEl, key), newVisited);
            } else if (left != null && right != null) {
                ElProperty diffProp = convert2ElProperty(key, parentEl, left);
                addChangeMetadata(diffProp, left, right);
                if (diffProp.isTypeChange() || diffProp.isNewEnums() || diffProp.isRemovedEnums()) {
                    changed.add(diffProp);
                }
            }
        });

        return this;
    }

    /** Returns the resolved object schema if schema is a $ref or has properties; null otherwise */
    private Schema resolveToSubModel(Schema schema, Map<String, Schema> definitions) {
        if (schema == null) return null;
        if (schema.get$ref() != null) {
            return definitions.get(getSimpleRef(schema.get$ref()));
        }
        // Array: try to resolve items
        if (schema.getItems() != null) {
            String itemRef = schema.getItems().get$ref();
            if (itemRef != null) {
                return definitions.get(getSimpleRef(itemRef));
            }
            if (schema.getItems().getProperties() != null && !schema.getItems().getProperties().isEmpty()) {
                return schema.getItems();
            }
        }
        // Object inline with properties
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            return schema;
        }
        return null;
    }

    /** Resolve a $ref schema to the referenced schema in definitions */
    private Schema resolveRef(Schema schema, Map<String, Schema> definitions) {
        if (schema == null) return null;
        if (schema.get$ref() != null) {
            String name = getSimpleRef(schema.get$ref());
            Schema resolved = definitions.get(name);
            return resolved != null ? resolved : schema;
        }
        return schema;
    }

    private boolean schemasCompatible(Schema left, Schema right) {
        String lt = left.getType();
        String rt = right.getType();
        if (lt != null && !lt.equals(rt)) return false;
        String lr = left.get$ref();
        String rr = right.get$ref();
        if (lr != null && !lr.equals(rr)) return false;
        return true;
    }

    private Collection<? extends ElProperty> convert2ElProperties(
            Map<String, Schema> propMap, String parentEl) {
        List<ElProperty> result = new ArrayList<>();
        if (propMap == null) return result;
        for (Entry<String, Schema> entry : propMap.entrySet()) {
            result.add(convert2ElProperty(entry.getKey(), parentEl, entry.getValue()));
        }
        return result;
    }

    private String buildElString(String parentEl, String propName) {
        return null == parentEl ? propName : (parentEl + "." + propName);
    }

    private ElProperty convert2ElProperty(String propName, String parentEl, Schema schema) {
        ElProperty pWithPath = new ElProperty();
        pWithPath.setProperty(schema);
        pWithPath.setEl(buildElString(parentEl, propName));
        return pWithPath;
    }

    private ElProperty addChangeMetadata(ElProperty diffProperty, Schema left, Schema right) {
        String leftType = left.getType();
        String rightType = right.getType();
        diffProperty.setTypeChange(leftType != null && !leftType.equalsIgnoreCase(
                rightType != null ? rightType : ""));

        List<String> leftEnums = enumValues(left);
        List<String> rightEnums = enumValues(right);
        if (!leftEnums.isEmpty() && !rightEnums.isEmpty()) {
            ListDiff<String> enumDiff = ListDiff.diff(leftEnums, rightEnums, (t, enumVal) -> {
                for (String value : t) {
                    if (enumVal.equalsIgnoreCase(value)) return value;
                }
                return null;
            });
            diffProperty.setNewEnums(enumDiff.getIncreased() != null && !enumDiff.getIncreased().isEmpty());
            diffProperty.setRemovedEnums(enumDiff.getMissing() != null && !enumDiff.getMissing().isEmpty());
        }
        return diffProperty;
    }

    @SuppressWarnings("unchecked")
    private <T> Set<T> copyAndAdd(Set<T> set, T... add) {
        Set<T> newSet = new HashSet<>(set);
        newSet.addAll(Arrays.asList(add));
        return newSet;
    }

    private List<String> enumValues(Schema schema) {
        List<String> result = new ArrayList<>();
        if (schema.getEnum() != null) {
            for (Object val : schema.getEnum()) {
                if (val != null) result.add(val.toString());
            }
        }
        return result;
    }

    private String getSimpleRef(String ref) {
        if (ref == null) return null;
        int lastSlash = ref.lastIndexOf('/');
        return lastSlash >= 0 ? ref.substring(lastSlash + 1) : ref;
    }

    public List<ElProperty> getIncreased() { return increased; }
    public void setIncreased(List<ElProperty> increased) { this.increased = increased; }

    public List<ElProperty> getMissing() { return missing; }
    public void setMissing(List<ElProperty> missing) { this.missing = missing; }

    public List<ElProperty> getChanged() { return changed; }
    public void setChanged(List<ElProperty> changed) { this.changed = changed; }
}
