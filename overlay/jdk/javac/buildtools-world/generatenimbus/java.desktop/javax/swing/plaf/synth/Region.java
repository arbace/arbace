/*
 * Arbace (overlay/jdk/javac/buildtools-world/generatenimbus, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359
 * stages 2-3 (day)", JCS368 (b)): a tool-world variant for the JDK build's TOOL_GENERATENIMBUS
 * (make/jdk/src/classes/build/tools/generatenimbus) on javac's tool executable, which lacks these classes of StAX and Swing: only
 * what the generator calls, as the API documents it. Read with the generator by bin/javac-vendor gensrc and defined by
 * the interpreter beside it; never part of the platform or the program.
 * Written for the documented Java SE API (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package javax.swing.plaf.synth;

/**
 * javax.swing.plaf.synth.Region's constants and getName: each region named as the API documents it, the constant's
 * words in camel case (ARROW_BUTTON is "ArrowButton").
 */
public class Region {
    public static final Region ARROW_BUTTON = new Region("ArrowButton");
    public static final Region BUTTON = new Region("Button");
    public static final Region CHECK_BOX = new Region("CheckBox");
    public static final Region CHECK_BOX_MENU_ITEM = new Region("CheckBoxMenuItem");
    public static final Region COLOR_CHOOSER = new Region("ColorChooser");
    public static final Region COMBO_BOX = new Region("ComboBox");
    public static final Region DESKTOP_PANE = new Region("DesktopPane");
    public static final Region DESKTOP_ICON = new Region("DesktopIcon");
    public static final Region EDITOR_PANE = new Region("EditorPane");
    public static final Region FILE_CHOOSER = new Region("FileChooser");
    public static final Region FORMATTED_TEXT_FIELD = new Region("FormattedTextField");
    public static final Region INTERNAL_FRAME = new Region("InternalFrame");
    public static final Region INTERNAL_FRAME_TITLE_PANE = new Region("InternalFrameTitlePane");
    public static final Region LABEL = new Region("Label");
    public static final Region LIST = new Region("List");
    public static final Region MENU = new Region("Menu");
    public static final Region MENU_BAR = new Region("MenuBar");
    public static final Region MENU_ITEM = new Region("MenuItem");
    public static final Region MENU_ITEM_ACCELERATOR = new Region("MenuItemAccelerator");
    public static final Region OPTION_PANE = new Region("OptionPane");
    public static final Region PANEL = new Region("Panel");
    public static final Region PASSWORD_FIELD = new Region("PasswordField");
    public static final Region POPUP_MENU = new Region("PopupMenu");
    public static final Region POPUP_MENU_SEPARATOR = new Region("PopupMenuSeparator");
    public static final Region PROGRESS_BAR = new Region("ProgressBar");
    public static final Region RADIO_BUTTON = new Region("RadioButton");
    public static final Region RADIO_BUTTON_MENU_ITEM = new Region("RadioButtonMenuItem");
    public static final Region ROOT_PANE = new Region("RootPane");
    public static final Region SCROLL_BAR = new Region("ScrollBar");
    public static final Region SCROLL_BAR_TRACK = new Region("ScrollBarTrack");
    public static final Region SCROLL_BAR_THUMB = new Region("ScrollBarThumb");
    public static final Region SCROLL_PANE = new Region("ScrollPane");
    public static final Region SEPARATOR = new Region("Separator");
    public static final Region SLIDER = new Region("Slider");
    public static final Region SLIDER_TRACK = new Region("SliderTrack");
    public static final Region SLIDER_THUMB = new Region("SliderThumb");
    public static final Region SPINNER = new Region("Spinner");
    public static final Region SPLIT_PANE = new Region("SplitPane");
    public static final Region SPLIT_PANE_DIVIDER = new Region("SplitPaneDivider");
    public static final Region TABBED_PANE = new Region("TabbedPane");
    public static final Region TABBED_PANE_TAB = new Region("TabbedPaneTab");
    public static final Region TABBED_PANE_TAB_AREA = new Region("TabbedPaneTabArea");
    public static final Region TABBED_PANE_CONTENT = new Region("TabbedPaneContent");
    public static final Region TABLE = new Region("Table");
    public static final Region TABLE_HEADER = new Region("TableHeader");
    public static final Region TEXT_AREA = new Region("TextArea");
    public static final Region TEXT_FIELD = new Region("TextField");
    public static final Region TEXT_PANE = new Region("TextPane");
    public static final Region TOGGLE_BUTTON = new Region("ToggleButton");
    public static final Region TOOL_BAR = new Region("ToolBar");
    public static final Region TOOL_BAR_CONTENT = new Region("ToolBarContent");
    public static final Region TOOL_BAR_DRAG_WINDOW = new Region("ToolBarDragWindow");
    public static final Region TOOL_TIP = new Region("ToolTip");
    public static final Region TOOL_BAR_SEPARATOR = new Region("ToolBarSeparator");
    public static final Region TREE = new Region("Tree");
    public static final Region TREE_CELL = new Region("TreeCell");
    public static final Region VIEWPORT = new Region("Viewport");

    private final String name;

    private Region(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
