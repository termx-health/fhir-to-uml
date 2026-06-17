package org.fhir.uml.generation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.fhir.uml.generation.uml.FHIRGenerator;
import org.fhir.uml.generation.uml.StructureDefinitionWrapper;
import org.fhir.uml.generation.uml.elements.Element;
import org.fhir.uml.generation.uml.elements.Legend;
import org.fhir.uml.generation.uml.elements.UML;
import org.fhir.uml.generation.uml.types.LegendPosition;
import org.fhir.uml.generation.uml.utils.Config;
import org.fhir.uml.generation.uml.utils.StructureDefinitionLoader;
import org.fhir.uml.generation.uml.utils.Utils;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


public class App {
    private static Config config;
    public static void main(String[] args) throws Exception {
        config = Config.fromArgs(args);

        if (config.isShowHelp() || config.getInputFilePath() == null
                || (config.getTxtOutputFilePath() == null && config.getOutputFilePath() == null)) {
            printUsage();
            return;
        }

        Map<String, Runnable> modeHandlers = new HashMap<>();
        modeHandlers.put("uml", App::runUmlMode);
        modeHandlers.put("fhir", App::runFhirMode);

        Runnable handler = modeHandlers.getOrDefault(config.getMode().toLowerCase(), App::runUmlMode);
        handler.run();
    }

    private static void runUmlMode() {
        try {
            StringBuilder jsonContent = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(config.getInputFilePath()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    jsonContent.append(line);
                }
            }

            // Accept R4 / R4B / R5 input; normalize to the R4 model the generator is written against.
            StructureDefinition structureDefinition =
                    StructureDefinitionLoader.loadAsR4(jsonContent.toString());

            UML uml = new UML();
            StructureDefinitionWrapper structureDefinitionWrapper = new StructureDefinitionWrapper(structureDefinition, uml);
            structureDefinitionWrapper.processSnapshot();
            structureDefinitionWrapper.processDifferential();

            if (config.isDifferential()) {
                structureDefinitionWrapper.mapDifferentialElementsWithSnapshotElements();
                if (config.isReduceSliceClasses()) {
                    structureDefinitionWrapper.reduceDifferentialSliceClasses();
                }
                structureDefinitionWrapper.generateDifferentialUMLClasses();
            } else {
                if (config.isReduceSliceClasses()) {
                    structureDefinitionWrapper.reduceSnapshotSliceClasses();
                }
                structureDefinitionWrapper.generateSnapshotUMLClasses();
            }

            // A definition with no renderable elements (e.g. an empty differential) yields no
            // classes and thus no main class — guard against NPE so we still emit a valid diagram.
            if (uml.getMainClass() != null) {
                uml.getMainClass().setName(Element.getURLLastPath(structureDefinition.getBaseDefinition()));
            }

            structureDefinitionWrapper.generateUMLRelations();

            Legend legend = new Legend();
            legend.setXPosition(LegendPosition.XPosition.RIGHT);
            legend.setYPosition(LegendPosition.YPosition.TOP);

            // Legend fields are optional on the StructureDefinition (e.g. FSH-authored logical
            // models often omit status). Guard every accessor so a missing field can't NPE the
            // whole render — coalesce strings and only read enum .getDisplay() when present.
            String status = structureDefinition.hasStatus() ? structureDefinition.getStatus().getDisplay() : "";
            String kind = structureDefinition.hasKind() ? structureDefinition.getKind().getDisplay() : "";
            legend.addGroup("StructureDefinition")
                    .setHeader("Type", "Value")
                    .addRow("url", Objects.toString(structureDefinition.getUrl(), ""))
                    .addRow("version", Objects.toString(structureDefinition.getVersion(), ""))
                    .addRow("name", Objects.toString(structureDefinition.getName(), ""))
                    .addRow("status", status)
                    .addRow("kind", kind)
                    .addRow("type", Objects.toString(structureDefinition.getType(), ""))
                    .addRow("abstract", String.valueOf(structureDefinition.getAbstract()))
                    .addRow("baseDefinition", Objects.toString(structureDefinition.getBaseDefinition(), ""));

            if (config.isShowConstraints()) {
                Legend.LegendGroup constraintGroup = legend.addGroup("Constraints");
                constraintGroup.setHeader("Key", "Severity", "Human");

                uml.getConstraints().values().forEach(constraint -> {
                    constraintGroup.addRow(constraint.getKey(), constraint.getSeverity(), String.format("wrap2(\"%s\", 50)", constraint.getHuman()));
                });
            }

            uml.setLegend(legend);

            // Rendering to PNG/SVG is offloaded to a PlantUML server by the caller;
            // this tool only emits PlantUML text. Write it to --txt (preferred) or --output.
            String textOutputPath = config.getTxtOutputFilePath() != null
                    ? config.getTxtOutputFilePath()
                    : config.getOutputFilePath();
            Utils.saveUMLAsText(uml, textOutputPath);
            System.out.println("PlantUML text written to: " + textOutputPath);
        } catch (Exception e) {
            System.err.println("Error in UML mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runFhirMode() {
        try {
            if (config.isSaveTxt()) {
                System.out.println("Warning: --txt is not used in 'fhir' mode. Ignoring.");
            }

            StringBuilder umlContent = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(config.getInputFilePath()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    umlContent.append(line).append("\n");
                }
            }

            FHIRGenerator generator = new FHIRGenerator();
            StructureDefinition structureDefinition = generator.parseUMLFile(umlContent.toString());
            FhirContext ctx = FhirContext.forR4();
            IParser parser = ctx.newJsonParser().setPrettyPrint(true);
            String structureDefinitionJson = parser.encodeResourceToString(structureDefinition);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(config.getOutputFilePath()))) {
                writer.write(structureDefinitionJson);
            }
            System.out.println("Transformation complete. FHIR StructureDefinition written to: " + config.getOutputFilePath());

        } catch (Exception e) {
            System.err.println("Error in FHIR mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static StructureDefinition createStructureDefinitionFromUml(String umlText) {
        // TODO: Implement UML -> StructureDefinition converter
        StructureDefinition sd = new StructureDefinition();
        return sd;
    }

    private static void printUsage() {
        System.out.println("FHIR UML Converter - Usage Guide");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar fhir-uml-generation.jar \\");
        System.out.println("       --input <input_file> \\");
        System.out.println("       --txt <txt_output_file> \\");
        System.out.println("       [--output <output_file>] \\");
        System.out.println("       [--mode <uml|fhir>] \\");
        System.out.println("       [--view <snapshot|differential>] \\");
        System.out.println("       [--hide_removed_objects <true|false>] \\");
        System.out.println("       [--show_constraints <true|false>] \\");
        System.out.println("       [--show_bindings <true|false>] \\");
        System.out.println("       [--reduce_slice_classes <true|false>] \\");
        System.out.println("       [--hide_legend <true|false>] \\");
        System.out.println("       [--help]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  uml (default): Transform FHIR StructureDefinition -> PlantUML text (rendered to PNG/SVG by a PlantUML server)");
        System.out.println("    --input       Path to input FHIR StructureDefinition (JSON)");
        System.out.println("    --txt         Path to write the generated PlantUML text");
        System.out.println("    --output      Alternative output path for the PlantUML text if --txt is not given");
        System.out.println();
        System.out.println("  fhir: Transform UML -> FHIR StructureDefinition");
        System.out.println("    --input       Path to UML in PlantUML text format (.txt)");
        System.out.println("    --output      Output FHIR StructureDefinition (usually .json)");
        System.out.println("    --txt         (ignored in this mode)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --view <snapshot|differential>       What elements need to generate from StructureDefinition. View mode used in UML generation (default: snapshot)");
        System.out.println("  --hide_removed_objects <true|false>  Hide removed/unsupported FHIR objects (default: true)");
        System.out.println("  --show_constraints <true|false>      Display FHIR constraints in diagram (default: true)");
        System.out.println("  --show_bindings <true|false>         Show value set bindings (default: true)");
        System.out.println("  --reduce_slice_classes <true|false>  Simplify representation of slices into fewer UML classes (default: false)");
        System.out.println("  --hide_legend <true|false>           Hide the legend/notes section in UML output (default: false)");
        System.out.println("  --help                               Show this help message and exit");
    }

}
