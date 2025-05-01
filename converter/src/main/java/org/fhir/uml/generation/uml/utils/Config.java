package org.fhir.uml.generation.uml.utils;

public final class Config {
    private static Config instance;

    // --- Fields (the old AppArguments) ---
    private String mode = "uml";            // default mode
    private String inputFilePath;
    private String outputFilePath;
    private boolean saveTxt = false;
    private String txtOutputFilePath;
    private boolean showHelp = false;

    // Example of the newly added fields:
    private String view = "snapshot";       // can be "snapshot" or "differential"
    private boolean hideRemovedObjects = true; // default is true
    private boolean showConstraints = false;
    private boolean showBindings = true;
    private boolean reduceSliceClasses = false;
    private boolean hideLegend = false;

    // --- Private constructor (singleton) ---
    private Config() {
    }

    /**
     * Initialize the Config from the command-line args.
     * This will set the internal static `instance` and parse the args.
     */
    public static synchronized Config fromArgs(String[] args) {
        if (instance == null) {
            instance = new Config();
            parseArguments(args, instance);
        } else {
            // If you prefer to allow re-parsing, you can either:
            //   1) do nothing, or
            //   2) parse again and overwrite the fields.
            // For now, let's do nothing:
            System.err.println("WARNING: Config has already been initialized; ignoring subsequent calls to fromArgs().");
        }
        return instance;
    }

    /**
     * Returns the existing Config instance or throws if not initialized.
     */
    public static Config getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Config not yet initialized. Call Config.fromArgs(...) first.");
        }
        return instance;
    }

    /**
     * Internal method that parses the args and populates a Config.
     */
    private static void parseArguments(String[] args, Config config) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg.toLowerCase()) {
                case "--help":
                    config.showHelp = true;
                    break;
                case "--mode":
                    if (i + 1 < args.length) {
                        config.mode = args[++i];
                    }
                    break;
                case "--input":
                    if (i + 1 < args.length) {
                        config.inputFilePath = args[++i];
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        config.outputFilePath = args[++i];
                    }
                    break;
                case "--txt":
                    config.saveTxt = true;
                    if ((i + 1) < args.length && !args[i + 1].startsWith("--")) {
                        config.txtOutputFilePath = args[++i];
                    }
                    break;
                case "--view":
                    if (i + 1 < args.length) {
                        config.view = args[++i];
                    }
                    break;
                case "--hide_removed_objects":
                    if (i + 1 < args.length) {
                        config.hideRemovedObjects = Boolean.parseBoolean(args[++i]);
                    }
                    break;
                case "--show_constraints":
                    if (i + 1 < args.length) {
                        config.showConstraints = Boolean.parseBoolean(args[++i]);
                    }
                    break;
                case "--show_bindings":
                    if (i + 1 < args.length) {
                        config.showBindings = Boolean.parseBoolean(args[++i]);
                    }
                    break;
                case "--reduce_slice_classes":
                    if (i + 1 < args.length) {
                        config.reduceSliceClasses = Boolean.parseBoolean(args[++i]);
                    }
                    break;
                case "--hide_legend":
                    if (i + 1 < args.length) {
                        config.hideLegend = Boolean.parseBoolean(args[++i]);
                    }
                    break;
            }
        }
    }

    // --- Getters (and setters if needed) ---
    public String getMode() {
        return mode;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public boolean isSaveTxt() {
        return saveTxt;
    }

    public String getTxtOutputFilePath() {
        return txtOutputFilePath;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public String getView() {
        return view;
    }

    public boolean isHideRemovedObjects() {
        return hideRemovedObjects;
    }

    public boolean isDifferential() {
        return view.equalsIgnoreCase("differential");
    }

    public boolean isShowConstraints() {
        return showConstraints;
    }

    public boolean isShowBindnigs() {
        return showBindings;
    }

    public boolean isShowBindings() {
        return showBindings;
    }

    public boolean isReduceSliceClasses() {
        return reduceSliceClasses;
    }

    public boolean isHideLegend() {
        return hideLegend;
    }

    // If you'd like to do fancy printing:
    public void print() {
        System.out.println("Config:");
        System.out.println("  mode = " + mode);
        System.out.println("  inputFilePath = " + inputFilePath);
        System.out.println("  outputFilePath = " + outputFilePath);
        System.out.println("  saveTxt = " + saveTxt);
        System.out.println("  txtOutputFilePath = " + txtOutputFilePath);
        System.out.println("  showHelp = " + showHelp);
        System.out.println("  view = " + view);
        System.out.println("  hideRemovedObjects = " + hideRemovedObjects);
        System.out.println("  showConstraints = " + showConstraints);
        System.out.println("  showBindings = " + showBindings);
    }
}
