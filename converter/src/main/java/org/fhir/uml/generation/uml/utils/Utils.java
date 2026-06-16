package org.fhir.uml.generation.uml.utils;

import org.fhir.uml.generation.uml.elements.UML;

import java.io.*;
import java.util.*;

public class Utils {
    public static void saveUMLAsText(UML uml, String outputFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(uml.toString());
        }
    }

    public static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String detectRemovedPathPart(String oldPath, String newPath) {
        // Split old and new strings by "." or ":"
        String[] oldTokens = oldPath.split("\\.|:");
        String[] newTokens = newPath.split("\\.|:");

        // Convert arrays to sets
        Set<String> oldSet = new HashSet<>(Arrays.asList(oldTokens));
        Set<String> newSet = new HashSet<>(Arrays.asList(newTokens));

        // Find tokens in oldPath that are not in newPath
        oldSet.removeAll(newSet);

        // Join them into a single string (comma-separated if multiple)
        return String.join(",", oldSet);
    }
}
