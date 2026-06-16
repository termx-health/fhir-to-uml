package org.fhir.uml.generation.uml;

import org.fhir.uml.generation.uml.elements.*;
import org.fhir.uml.generation.uml.types.RelationShipType;
import org.fhir.uml.generation.uml.utils.Utils;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.*;
import org.hl7.fhir.r4.model.ElementDefinition;

import java.util.*;
import java.util.stream.Collectors;

public class StructureDefinitionWrapper {
    private final UML uml;

    private StructureDefinition structureDefinition;

    private List<ElementDefinition> snapshotElements;
    private List<ElementDefinition> differentialElements;

    private final Map<String, String> fixedValues = new HashMap<>();

    private Map<String, List<Element>> snapshotTableMap = new LinkedHashMap<>();
    private Map<String, List<Element>> differentialTableMap = new LinkedHashMap<>();

    private Map<String, Element> snapshotElementMapper = new LinkedHashMap<>();
    private Map<String, Element> differentialElementMapper = new LinkedHashMap<>();

    private final ElementFactory factory;

    public StructureDefinitionWrapper(StructureDefinition structureDefinition, UML uml) throws Exception {
        this.structureDefinition = structureDefinition;
        this.uml = uml;
        this.factory = new ElementFactory(fixedValues, uml.getConstraints());
    }

    public void processSnapshot() {
        this.snapshotElements = structureDefinition.getSnapshot().getElement();
        processElements(snapshotElements, snapshotTableMap, snapshotElementMapper);
    }

    public void processDifferential() {
        this.differentialElements = structureDefinition.getDifferential().getElement();
        processElements(differentialElements, differentialTableMap, differentialElementMapper);
    }

    private void processElements(List<ElementDefinition> structureElements, Map<String, List<Element>> tableMap, Map<String, Element> elementMapper) {
        boolean firstElementProcessed = false;
        expandAllFixedValues(structureElements);

        for (ElementDefinition elementDefinition : structureElements) {
            firstElementProcessed = processElementsToTables(
                    elementDefinition,
                    tableMap,
                    elementMapper,
                    firstElementProcessed
            );
        }
    }

    public void reduceSnapshotSliceClasses() {
        Map<String, String> reduceMap = generateReduceMap(snapshotTableMap);
        snapshotTableMap = transformMap(snapshotTableMap, reduceMap);
        snapshotElementMapper = transformKeys(snapshotElementMapper, reduceMap);
    }

    public void reduceDifferentialSliceClasses() {
        Map<String, String> reduceMap = generateReduceMap(differentialTableMap);
        differentialTableMap = transformMap(differentialTableMap, reduceMap);
        differentialElementMapper = transformKeys(differentialElementMapper, reduceMap);
    }

    private Map<String, String> generateReduceMap(Map<String, List<Element>> tableMap) {
        return tableMap.entrySet().stream()
                .filter(entry -> entry.getValue().stream().allMatch(Element::getHasSliceName))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            int lastDotIndex = entry.getKey().lastIndexOf('.');
                            return (lastDotIndex != -1) ? entry.getKey().substring(0, lastDotIndex) : entry.getKey();
                        },
                        (existing, replacement) -> existing, // Merge strategy (keep existing)
                        LinkedHashMap::new // Preserve insertion order
                ));
    }

    private static Map<String, List<Element>> transformMap(Map<String, List<Element>> originalMap, Map<String, String> renameMap) {
        Map<String, List<Element>> transformedMap = new LinkedHashMap<>();
        boolean changesApplied;

        do {
            changesApplied = false;
            Map<String, List<Element>> tempMap = new LinkedHashMap<>();

            for (Map.Entry<String, List<Element>> entry : originalMap.entrySet()) {
                String oldKey = entry.getKey();
                String newKey = renameMap.getOrDefault(oldKey, renameNestedKey(oldKey, renameMap));

                // If key has changed, mark that changes were applied
                if (!newKey.equals(oldKey)) {
                    changesApplied = true;
                }

                // Merge elements if the key already exists in the transformed map
                tempMap.computeIfAbsent(newKey, k -> new ArrayList<>()).addAll(entry.getValue());
            }

            originalMap = tempMap; // Update originalMap with the transformed map
        } while (changesApplied); // Repeat until no more changes are detected

        return originalMap;
    }

    private static Map<String, Element> transformKeys(Map<String, Element> originalMap, Map<String, String> renameMap) {
        return originalMap.entrySet().stream().collect(Collectors.toMap(
                entry -> renameMap.getOrDefault(entry.getKey(), renameNestedKey(entry.getKey(), renameMap)),
                entry -> {
                    Element element = entry.getValue();
                    String newKey = renameMap.getOrDefault(entry.getKey(), renameNestedKey(entry.getKey(), renameMap));
                    String removedPart = Utils.detectRemovedPathPart(element.getElementId(), newKey);
                    if (!element.getElementId().equals(newKey) && !element.getName().equalsIgnoreCase(removedPart) && element.getHasSliceName()) {
                        element.setGroup(String.format("Slices for %s", removedPart));
                    }
                    element.setId(newKey); // Ensure the Element ID is updated
                    return element;
                },
                (existing, replacement) -> existing, // Keep existing element in case of conflict
                LinkedHashMap::new // Maintain order
        ));
    }

    private static String renameNestedKey(String key, Map<String, String> renameMap) {
        for (Map.Entry<String, String> renameEntry : renameMap.entrySet()) {
            if (key.startsWith(renameEntry.getKey() + ":")) {
                return key.replace(renameEntry.getKey() + ":", renameEntry.getValue() + ":");
            }
        }
        return key; // Return unchanged if no match found
    }

    public void generateSnapshotUMLClasses() {
        generateUMLClasses(snapshotTableMap, snapshotElementMapper);
    }

    public void generateDifferentialUMLClasses() {
        generateUMLClasses(differentialTableMap, differentialElementMapper);
    }

    public void mapDifferentialElementsWithSnapshotElements() {
        List<String> keys = new ArrayList<>(differentialElementMapper.keySet());

        for (String key : keys) {
            Element differentialElement = differentialElementMapper.get(key);
            Element snapshotElement = snapshotElementMapper.get(key);

            if (differentialElement != null && snapshotElement != null) {
                differentialElement.copyValuesFrom(snapshotElement);
            }

            // Determine the parent from snapshot and set it in differential
            String parentId = (differentialElement != null) ? differentialElement.getParentId() : null;
            if (parentId != null) {
                Element parentElement = snapshotElementMapper.get(parentId);
                differentialElementMapper.computeIfAbsent(parentId, k -> parentElement);
            }

            String parentParentElementId = differentialElement.getElementId();
            String parentElementId = differentialElement.getElementId();
            while (true) {
                // Find the last '.' or the last ':'
                int lastDot = parentElementId.lastIndexOf('.');
                int lastColon = parentElementId.lastIndexOf(':');

                // Whichever is further to the right is the "last" delimiter
                int lastDelimiter = Math.max(lastDot, lastColon);

                // If no '.' or ':' is found, we're done
                if (lastDelimiter == -1) {
                    break;
                }

                parentElementId = parentElementId.substring(0, lastDelimiter);
                String finalParentParentElementId = parentParentElementId;
                differentialTableMap.computeIfAbsent(parentElementId, k -> new ArrayList<>(List.of(snapshotElementMapper.get(finalParentParentElementId))));
                differentialElementMapper.computeIfAbsent(parentElementId, snapshotElementMapper::get);
                differentialElementMapper.computeIfAbsent(parentParentElementId, snapshotElementMapper::get);

                parentParentElementId = parentElementId;
            }
        }
    }

    private void generateUMLClasses(Map<String, List<Element>> tableMap, Map<String, Element> elementMapper) {
        boolean firstClassPassed = false;
        for (Map.Entry<String, List<Element>> entry : tableMap.entrySet()) {
            Element umlElement = elementMapper.get(entry.getKey());
            Element parentUmlElement = elementMapper.get(umlElement.getParentId());

//            System.out.printf("Class Type: %s | Name: %s | Parent Element: %s \n", umlElement.getType(), umlElement.getName(), parentUmlElement.getElementId());
            UMLClass umlClass = new UMLClass(umlElement.getType(), umlElement.getName(), umlElement, parentUmlElement, umlElement.isRemoved());

            if (!firstClassPassed) {
                firstClassPassed = true;
                umlClass.setMainClass(true);
            }

            entry.getValue().forEach(umlClass::addElement);
            uml.addClass(umlClass);
        }
    }

    public void generateUMLRelations() {
        for (UMLClass umlClass : uml.getClasses()) {
            UMLClass parentUmlClass = uml.findClassByElement(umlClass.getParentElement());
            if (parentUmlClass != null && !parentUmlClass.equals(umlClass)) {
                RelationShipType relationship = RelationShipType.AGGREGATION;
                if (parentUmlClass.getMainElement().isMain()) {
                    relationship = RelationShipType.COMPOSITION;
                }

                uml.addRelation(Relation.from(
                        parentUmlClass,
                        umlClass,
                        relationship,
                        umlClass.getMainElement().getName(),
                        umlClass.getMainElement().getCardinality()
                ));
            }
        }
    }

    private void expandAllFixedValues(List<ElementDefinition> structureElements) {
        List<ElementDefinition> copyList = new ArrayList<>(structureElements);
        factory.defineFixedValues(copyList, structureElements);
    }

    private boolean processElementsToTables(ElementDefinition element, Map<String, List<Element>> tableMap, Map<String, Element> elementMapper, boolean firstElementProcessed) {
        String id = element.getId();
        Element umlElement = factory.fromElementDefinition(element);

        if (!element.getSlicing().getDiscriminator().isEmpty()) {
            umlElement.setSliceHeader(true);
        }

        if (!firstElementProcessed) {
            umlElement.setType(umlElement.getName());
            umlElement.setIsMain(true);
            tableMap.computeIfAbsent(umlElement.getParentId(), k -> new ArrayList<>()).add(umlElement);
            elementMapper.put(id, umlElement);
            return true;
        }

        elementMapper.put(id, umlElement);
        tableMap.computeIfAbsent(umlElement.getParentId(), k -> new ArrayList<>())
                .add(umlElement);

        if (umlElement.isChoiceOfTypeHeader()) {
            Arrays.stream(umlElement.getType().split(", ")).forEach(type -> {
                String choiseId = umlElement.getElementId() + "." + type;
                Element choiseUML = new Element.Builder().choiceOfTypeElement(true).name(umlElement.getName().replace("[x]", "") + Utils.capitalize(type)).type(type).build();
                tableMap.computeIfAbsent(umlElement.getElementId(), k -> new ArrayList<>()).add(choiseUML);
                elementMapper.put(choiseId, choiseUML);
            });
        }

        return true;
    }
}
