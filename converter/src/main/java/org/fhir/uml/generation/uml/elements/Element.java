package org.fhir.uml.generation.uml.elements;

import org.fhir.uml.generation.uml.types.ElementModifiers;
import org.fhir.uml.generation.uml.types.ElementVisability; // If you control this class name, consider renaming to ElementVisibility
import org.fhir.uml.generation.uml.utils.Config;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UrlType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a UML Element with FHIR-specific properties (e.g., cardinalities, slice headers, etc.).
 */
public class Element {

    // ---------------------------------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------------------------------
    private String name;
    private String type;
    private ElementVisability visibility;
    private Cardinality cardinality;
    private String description;
    private Boolean isMain;
    private Boolean isSliceHeader = false;
    private Boolean hasFixedValue;
    private String fixedValue;
    private Integer commentId;
    private Boolean choiceOfTypeHeader;
    private Boolean choiceOfTypeElement;
    private String id;
    private String path;
    private Boolean hasSliceName;
    private final List<ElementModifiers> differentialModifiers = new ArrayList<>();
    private Binding binding;
    private List<Constraint> constraints;
    private String group;

    // ---------------------------------------------------------------------------------------------
    // Private Constructor
    // ---------------------------------------------------------------------------------------------
    private Element(String name,
                    String type,
                    ElementVisability visibility,
                    Cardinality cardinality,
                    String description,
                    Boolean hasFixedValue,
                    Integer commentId,
                    Boolean choiceOfTypeHeader,
                    Boolean choiceOfTypeElement,
                    String fixedValue,
                    String id,
                    Boolean hasSliceName,
                    Boolean isMain,
                    Binding binding,
                    List<Constraint> constraints) {

        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.cardinality = cardinality;
        this.description = description;
        this.hasFixedValue = hasFixedValue;
        this.commentId = commentId;
        this.choiceOfTypeHeader = choiceOfTypeHeader;
        this.choiceOfTypeElement = choiceOfTypeElement;
        this.fixedValue = fixedValue;
        this.id = id;
        this.path = "";
        this.hasSliceName = hasSliceName;
        this.isMain = isMain;
        this.binding = binding;
        this.constraints = constraints;
        this.group = "";
    }

    // ---------------------------------------------------------------------------------------------
    // Static Utility Methods
    // ---------------------------------------------------------------------------------------------

    /**
     * Resolves the type(s) of an ElementDefinition into a comma-separated string.
     * If no types exist, returns "N/A".
     */
    public static String resolveType(ElementDefinition element) {
        if (!element.hasType()) {
            return "N/A";
        }
        return element.getType().stream()
                .map(Element::resolveTypeComponent)
                .collect(Collectors.joining(", "));
    }

    /**
     * Helper method to resolve a single TypeRefComponent into a string representation.
     */
    private static String resolveTypeComponent(ElementDefinition.TypeRefComponent type) {
        if (type == null) {
            return "Unknown";
        }

        // Check for extension-based type override
        if (!type.getExtension().isEmpty()) {
            String extensionType = handleExtensionType(type.getExtension());
            if (extensionType != null) {
                return extensionType;
            }
        }

        String baseType = type.getCode();

        if (!type.getProfile().isEmpty() && "Extension".equals(baseType)) {
            CanonicalType profile = type.getProfile().getFirst();
            String lastSegment = getURLLastPath(profile.getValue());
            return "Extension" + (lastSegment.isEmpty() ? "" : "(" + lastSegment + ")");
        }

        if (!type.getProfile().isEmpty()) {
            CanonicalType profile = type.getProfile().getFirst();
            String lastSegment = getURLLastPath(profile.getValue());
            return baseType + (lastSegment.isEmpty() ? "" : "(" + lastSegment + ")");
        }

        if ("Reference".equals(baseType)) {
            return handleReferenceType(type);
        }
        if ("canonical".equals(baseType)) {
            return handleCanonicalType(type);
        }

        return baseType != null ? baseType : "N/A";
    }

    public static String getURLLastPath(String value) {
        return value.substring(Math.max(0, value.lastIndexOf('/') + 1));
    }

    /**
     * Extracts a type override from the first extension if it exists.
     */
    private static String handleExtensionType(List<Extension> extensions) {
        Extension firstExtension = extensions.getFirst();
        if (firstExtension != null && firstExtension.getValue() instanceof UrlType) {
            return ((UrlType) firstExtension.getValue()).getValue();
        }
        return null;
    }

    /**
     * Formats a Reference type string, including its possible target profiles.
     */
    private static String handleReferenceType(ElementDefinition.TypeRefComponent type) {
        if (type.hasTargetProfile()) {
            String targetProfiles = type.getTargetProfile().stream()
                    .map(profile -> extractProfileName(profile.getValue()))
                    .collect(Collectors.joining(" | "));
            return String.format("Reference(%s)", targetProfiles);
        }
        return "Reference";
    }

    /**
     * Formats a canonical type string, including its possible target profiles.
     */
    private static String handleCanonicalType(ElementDefinition.TypeRefComponent type) {
        if (type.hasTargetProfile()) {
            String targetProfiles = type.getTargetProfile().stream()
                    .map(profile -> extractProfileName(profile.getValue()))
                    .collect(Collectors.joining(" | "));
            return String.format("canonical(%s)", targetProfiles);
        }
        return "canonical";
    }

    /**
     * Extracts the trailing portion of a URL (after the last slash) as the profile name.
     */
    private static String extractProfileName(String profileValue) {
        if (profileValue == null) {
            return "UnknownProfile";
        }
        return profileValue.substring(profileValue.lastIndexOf('/') + 1);
    }

    // ---------------------------------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------------------------------

    public static class Builder {
        private String name;
        private String type;
        private ElementVisability visibility;
        private Cardinality cardinality;
        private String description;
        private Boolean hasFixedValue;
        private Integer commentId;
        private Boolean choiceOfTypeHeader;
        private Boolean choiceOfTypeElement;
        private String fixedValue;
        private String id;
        private boolean hasSliceName;
        private boolean isMain;
        private Binding binding;
        private final List<Constraint> constraints;

        public Builder() {
            // Default initialization
            this.name = "";
            this.type = "";
            this.description = "";
            this.hasFixedValue = false;
            this.commentId = null;
            this.choiceOfTypeHeader = false;
            this.choiceOfTypeElement = false;
            this.fixedValue = "";
            this.id = "";
            this.hasSliceName = false;
            this.isMain = false;
            this.cardinality = new Cardinality("", "");
            this.binding = null;
            this.constraints = new ArrayList<>();
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder visibility(ElementVisability visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder cardinality(Cardinality cardinality) {
            this.cardinality = cardinality;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder hasFixedValue(Boolean hasFixedValue) {
            this.hasFixedValue = hasFixedValue;
            return this;
        }

        public Builder commentId(Integer commentId) {
            this.commentId = commentId;
            return this;
        }

        public Builder choiceOfTypeHeader(Boolean choiceOfTypeHeader) {
            this.choiceOfTypeHeader = choiceOfTypeHeader;
            return this;
        }

        public Builder choiceOfTypeElement(Boolean choiceOfTypeElement) {
            this.choiceOfTypeElement = choiceOfTypeElement;
            return this;
        }

        public Builder fixedValue(String fixedValue) {
            this.fixedValue = fixedValue;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder hasSliceName(Boolean hasSliceName) {
            this.hasSliceName = hasSliceName;
            return this;
        }

        public Builder isMain(Boolean isMain) {
            this.isMain = isMain;
            return this;
        }

        public Builder binding(Binding binding) {
            this.binding = binding;
            return this;
        }

        public Builder addConstraint(Constraint constraint) {
            this.constraints.add(constraint);
            return this;
        }

        public Element build() {
            return new Element(
                    this.name,
                    this.type,
                    this.visibility,
                    this.cardinality,
                    this.description,
                    this.hasFixedValue,
                    this.commentId,
                    this.choiceOfTypeHeader,
                    this.choiceOfTypeElement,
                    this.fixedValue,
                    this.id,
                    this.hasSliceName,
                    this.isMain,
                    this.binding,
                    this.constraints
            );
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------------------------------------
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public ElementVisability getVisibility() {
        return visibility;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getChoiceOfTypeElement() {
        return choiceOfTypeElement;
    }

    public Boolean isChoiceOfTypeHeader() {
        return choiceOfTypeHeader;
    }

    public Boolean isChoiceOfTypeElement() {
        return choiceOfTypeElement;
    }

    public Boolean getHasSliceName() {
        return hasSliceName;
    }

    public void setHasSliceName(Boolean hasSliceName) {
        this.hasSliceName = hasSliceName;
    }

    public Boolean isSliceHeader() {
        return isSliceHeader;
    }

    public void setSliceHeader(Boolean sliceHeader) {
        this.isSliceHeader = sliceHeader;
    }

    public String getElementId() {
        return this.id;
    }

    public Boolean isMain() {
        return isMain;
    }

    public void setIsMain(Boolean isMain) {
        this.isMain = isMain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Binding getBinding() {
        return binding;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    // ---------------------------------------------------------------------------------------------
    // Additional Utilities
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the element's parent name by splitting the ID on '.' or ':'.
     * Example: "Patient.identifier" → parent name is "Patient".
     */
    public String getParentName() {
        String[] parts = this.id.split("\\.|:");
        if (parts.length < 2) {
            return null;
        }
        return parts[parts.length - 2];
    }

    /**
     * Returns the parent ID by trimming everything after the last '.' or ':'.
     * Example: "Patient.identifier.system" → "Patient.identifier"
     */
    public String getParentId() {
        int lastDotIndex = id.lastIndexOf('.');
        int lastColonIndex = id.lastIndexOf(':');
        int splitIndex = Math.max(lastDotIndex, lastColonIndex);

        if (splitIndex == -1) {
            return id;
        }
        return id.substring(0, splitIndex);
    }

    /**
     * Returns the slice's parent ID by trimming everything after the last ':'.
     */
    public String getSliceParentId() {
        int lastColonIndex = id.lastIndexOf(':');
        if (lastColonIndex == -1) {
            return id;
        }
        return id.substring(0, lastColonIndex);
    }

    /**
     * Determines if this element is a data type (i.e., if the type starts with an uppercase letter).
     */
    public Boolean isDataType() {
        return type != null
                && !type.isEmpty()
                && Character.isUpperCase(type.charAt(0));
    }

    /**
     * Matches visibility to its UML representation symbol (e.g., "+" for public).
     */
    public String matchVisibilitySymbol() {
        if (Boolean.TRUE.equals(this.isMain)) {
            return "-";
        }
        if (this.visibility == null) {
            return "+";
        }
        return this.visibility.toSymbol();
    }

    /**
     * Determines if the cardinality indicates the element is removed.
     */
    public boolean isRemoved() {
        return cardinality != null && cardinality.isRemoved();
    }

    public void copyValuesFrom(Element source) {
        copyValuesFrom(source, false);
    }

    public List<String> copyValuesFrom(Element source, boolean logDifferences) {
        List<String> changes = new ArrayList<>();
        if (source == null) {
            return changes; // Empty list, no changes.
        }

        // -- Example: name --
        if (!Objects.equals(this.name, source.name)) {
            changes.add("name: " + this.name + " -> " + source.name);
            this.name = source.name;
        }

        // -- type --
        if (!Objects.equals(this.type, source.type)) {
            changes.add("type: " + this.type + " -> " + source.type);
            this.type = source.type;
        } else {
            differentialModifiers.add(ElementModifiers.TYPE);
        }

        // -- visibility --
        if (!Objects.equals(this.visibility, source.visibility)) {
            changes.add("visibility: " + this.visibility + " -> " + source.visibility);
            this.visibility = source.visibility;
        }

        if (!Objects.equals(this.binding, source.binding) && this.binding != null) {
            differentialModifiers.add(ElementModifiers.BINDING);
        }

        // -- cardinality MAX --
        if (!Objects.equals(this.cardinality.getMax(), source.cardinality.getMax())) {
            changes.add("MAX cardinality: " + this.cardinality.getMax() + " -> " + source.cardinality.getMax());
            this.cardinality.setMax(source.cardinality.getMax());
        } else {
            differentialModifiers.add(ElementModifiers.CARDINALITY_MAX);
        }

        // -- cardinality MIN -
        if (!Objects.equals(this.cardinality.getMin(), source.cardinality.getMin())) {
            changes.add("MIN cardinality: " + this.cardinality.getMin() + " -> " + source.cardinality.getMin());
            this.cardinality.setMin(source.cardinality.getMin());
        } else {
            differentialModifiers.add(ElementModifiers.CARDINALITY_MIN);
        }

        // -- description --
        if (!Objects.equals(this.description, source.description)) {
            changes.add("description: " + this.description + " -> " + source.description);
            this.description = source.description;
        }

        // -- isMain --
        if (!Objects.equals(this.isMain, source.isMain)) {
            changes.add("isMain: " + this.isMain + " -> " + source.isMain);
            this.isMain = source.isMain;
        }

        // -- isSliceHeader --
        if (!Objects.equals(this.isSliceHeader, source.isSliceHeader)) {
            changes.add("isSliceHeader: " + this.isSliceHeader + " -> " + source.isSliceHeader);
            this.isSliceHeader = source.isSliceHeader;
        }

        // -- hasFixedValue --
        if (!Objects.equals(this.hasFixedValue, source.hasFixedValue)) {
            changes.add("hasFixedValue: " + this.hasFixedValue + " -> " + source.hasFixedValue);
            this.hasFixedValue = source.hasFixedValue;
        }

        // -- fixedValue --
        if (!Objects.equals(this.fixedValue, source.fixedValue)) {
            changes.add("fixedValue: " + this.fixedValue + " -> " + source.fixedValue);
            this.fixedValue = source.fixedValue;
        } else if (!this.fixedValue.isEmpty() && !source.fixedValue.isEmpty()) {
            differentialModifiers.add(ElementModifiers.FIXED_VALUE);
        }

        // -- commentId --
        if (!Objects.equals(this.commentId, source.commentId)) {
            changes.add("commentId: " + this.commentId + " -> " + source.commentId);
            this.commentId = source.commentId;
        }

        // -- choiceOfTypeHeader --
        if (!Objects.equals(this.choiceOfTypeHeader, source.choiceOfTypeHeader)) {
            changes.add("choiceOfTypeHeader: " + this.choiceOfTypeHeader + " -> " + source.choiceOfTypeHeader);
            this.choiceOfTypeHeader = source.choiceOfTypeHeader;
        }

        // -- choiceOfTypeElement --
        if (!Objects.equals(this.choiceOfTypeElement, source.choiceOfTypeElement)) {
            changes.add("choiceOfTypeElement: " + this.choiceOfTypeElement + " -> " + source.choiceOfTypeElement);
            this.choiceOfTypeElement = source.choiceOfTypeElement;
        }

        // -- id --
        if (!Objects.equals(this.id, source.id)) {
            changes.add("id: " + this.id + " -> " + source.id);
            this.id = source.id;
        }

        // -- path --
        if (!Objects.equals(this.path, source.path)) {
            changes.add("path: " + this.path + " -> " + source.path);
            this.path = source.path;
        }

        // -- hasSliceName --
        if (!Objects.equals(this.hasSliceName, source.hasSliceName)) {
            changes.add("hasSliceName: " + this.hasSliceName + " -> " + source.hasSliceName);
            this.hasSliceName = source.hasSliceName;
        }

        if (!Objects.equals(this.constraints, source.constraints)) {
            changes.add("constraints: " + this.constraints + " -> " + source.constraints);
            this.constraints = source.constraints;
        }

        // Optionally log differences
        if (logDifferences && !changes.isEmpty()) {
            System.out.println("Changes applied to " + getElementId() + ": ");
            for (String change : changes) {
                System.out.println("  " + change);
            }
        }

        return changes;
    }

    private String wrapVariable(String value, ElementModifiers modifier) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            return value;
        }

        if (isRemoved() && modifier == ElementModifiers.STRICKEN_THROUGH) {
            return "strikethrough('" + value + "')";
        }

        if (modifier == ElementModifiers.NAME && !differentialModifiers.isEmpty()) {
            return "black('" + value + "')";
        }

        if (differentialModifiers.contains(modifier)) {
            return "black(bold('" + value + "'))";
        }
        return value;
    }

    private String matchCardinality() {
        String min = wrapVariable(cardinality.getMin(), ElementModifiers.CARDINALITY_MIN);
        String max = wrapVariable(cardinality.getMax(), ElementModifiers.CARDINALITY_MAX);

        if (min.isBlank() && max.isBlank()) {
            return "";
        }

        return String.format("[%s..%s]",min,max);
    }

    private String matchConstraints() {
        StringBuilder sb = new StringBuilder();
        if (!constraints.isEmpty() && Config.getInstance().isShowConstraints()) {
            String constraintListStr = constraints.stream()
                    .map(Constraint::getKey) // Extract keys
                    .collect(Collectors.joining(","));
            sb.append("<sup>(").append(constraintListStr).append(")</sup>");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        if (isRemoved() && !Config.getInstance().isHideRemovedObjects()) {
            return String.format("{field} %s %s", matchVisibilitySymbol(), wrapVariable(String.format("%s : %s %s %s", name, type, fixedValue, cardinality), ElementModifiers.STRICKEN_THROUGH));
        }

        String fixedValueStr = wrapVariable(fixedValue, ElementModifiers.FIXED_VALUE);

        String fixedValuePart = (fixedValueStr == null || fixedValueStr.isBlank())
                ? ""
                : "= " + fixedValueStr;

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                "{field} %s %s : %s %s %s %s%s",
                matchVisibilitySymbol(),
                wrapVariable(name, ElementModifiers.NAME),
                wrapVariable(type, ElementModifiers.TYPE),
                fixedValuePart,
                matchCardinality(),
                wrapVariable(description, ElementModifiers.DESCRIPTION),
                this.matchConstraints()
        ));

        if (binding != null && Config.getInstance().isShowBindnigs()) {
            sb.append("\n\t").append(wrapVariable(binding.toString(), ElementModifiers.BINDING));
        }

        return sb.toString();
    }
}
