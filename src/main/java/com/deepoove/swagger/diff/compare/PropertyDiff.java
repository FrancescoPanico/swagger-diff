package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deepoove.swagger.diff.model.ElProperty;

import io.swagger.v3.oas.models.media.Schema;

@SuppressWarnings({"rawtypes"})
public class PropertyDiff {

    private List<ElProperty> increased;
    private List<ElProperty> missing;
    private List<ElProperty> changed;

    Map<String, Schema> oldDefinitions;
    Map<String, Schema> newDefinitions;

    private PropertyDiff() {
        increased = new ArrayList<>();
        missing = new ArrayList<>();
        changed = new ArrayList<>();
    }

    public static PropertyDiff buildWithDefinition(Map<String, Schema> left, Map<String, Schema> right) {
        PropertyDiff diff = new PropertyDiff();
        diff.oldDefinitions = left != null ? left : new HashMap<>();
        diff.newDefinitions = right != null ? right : new HashMap<>();
        return diff;
    }

    public PropertyDiff diff(Schema left, Schema right) {
        ModelDiff modelDiff = ModelDiff.buildWithDefinition(oldDefinitions, newDefinitions)
                .diffSchema(left, right);
        increased.addAll(modelDiff.getIncreased());
        missing.addAll(modelDiff.getMissing());
        changed.addAll(modelDiff.getChanged());
        return this;
    }

    public List<ElProperty> getIncreased() { return increased; }
    public void setIncreased(List<ElProperty> increased) { this.increased = increased; }

    public List<ElProperty> getMissing() { return missing; }
    public void setMissing(List<ElProperty> missing) { this.missing = missing; }

    public List<ElProperty> getChanged() { return changed; }
    public void setChanged(List<ElProperty> changed) { this.changed = changed; }
}
