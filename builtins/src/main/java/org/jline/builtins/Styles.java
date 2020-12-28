/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jline.utils.StyleResolver;

public class Styles {
    private static final String DEFAULT_LS_COLORS = "di=1;91:ex=1;92:ln=1;96:fi=";
    private static final String DEFAULT_HELP_COLORS = "ti=1;34:co=1:ar=3:op=33";
    private static final String DEFAULT_PRNT_COLORS = "th=1;34:rn=1;34:mk=1;34:em=31:vs=32";
    private static final String LS_COLORS = "LS_COLORS";
    private static final String HELP_COLORS = "HELP_COLORS";
    private static final String PRNT_COLORS = "PRNT_COLORS";

    private static final String KEY = "([a-z]{2}|\\*\\.[a-zA-Z0-9]+)";
    private static final String VALUE = "[!~]?[a-zA-Z0-9]+[a-z0-9-;]*";
    private static final String VALUES = VALUE + "(," + VALUE + ")*";
    private static final String STYLE_PATTERN = KEY + "=" + VALUES + "(:" + KEY + "=" + VALUES + ")*(:|)";

    public static StyleResolver lsStyle() {
        return style(LS_COLORS, DEFAULT_LS_COLORS);
    }

    public static StyleResolver helpStyle() {
        return style(HELP_COLORS, DEFAULT_HELP_COLORS);
    }

    public static StyleResolver prntStyle() {
        return style(PRNT_COLORS, DEFAULT_PRNT_COLORS);
    }

    public static boolean isAnsiStylePattern(String style) {
        return style.matches(STYLE_PATTERN);
    }

    private static StyleResolver style(String name, String defStyle) {
        String style = consoleOption(name);
        if (style == null) {
            style = defStyle;
        }
        return style(style);
    }

    private static String consoleOption(String name) {
        String out = null;
        try {
            ConsoleOptionGetter cog = (ConsoleOptionGetter)Class.forName("org.jline.console.SystemRegistry")
                                                                .getDeclaredMethod("get").invoke(null);
            if (cog != null) {
                out = (String)cog.consoleOption(name);
                if (out != null && !out.matches(STYLE_PATTERN)) {
                    out = null;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        if (out == null) {
            out = System.getenv(name);
            if (out != null && !out.matches(STYLE_PATTERN)) {
                out = null;
            }
        }
        return out;
    }

    private static StyleResolver style(String style) {
        Map<String, String> colors = Arrays.stream(style.split(":"))
                .collect(Collectors.toMap(s -> s.substring(0, s.indexOf('=')),
                        s -> s.substring(s.indexOf('=') + 1)));
        return new StyleResolver(new StyleCompiler(colors)::getStyle);
    }

    protected static class StyleCompiler {
        private static final String ANSI_VALUE = "[0-9]*(;[0-9]+){0,2}";
        private static final List<String> JLINE_NAMED_STYLES = Arrays.asList("blink", "bold", "conceal", "crossed-out"
                , "crossedout", "faint", "hidden", "inverse", "inverse-neg", "inverseneg", "italic", "underline");
        private static final List<String> COLORS_8 = Arrays.asList("white", "black", "red", "blue", "green", "yellow", "magenta", "cyan");
        // https://github.com/lhmouse/nano-win/commit/a7aab18dfeef8a0e8073d5fa420677dc8fe548da
        private static final Map<String,Integer> COLORS_NANO = new HashMap<>();
        static {
            COLORS_NANO.put("pink", 204);
            COLORS_NANO.put("purple", 163);
            COLORS_NANO.put("mauve", 134);
            COLORS_NANO.put("lagoon", 38);
            COLORS_NANO.put("mint", 48);
            COLORS_NANO.put("lime", 148);
            COLORS_NANO.put("peach", 215);
            COLORS_NANO.put("orange", 208);
            COLORS_NANO.put("latte", 137);
        }
        private final Map<String,String> colors;
        private final boolean nanoStyle;

        public StyleCompiler(Map<String,String> colors) {
            this(colors, false);
        }
        public StyleCompiler(Map<String,String> colors, boolean nanoStyle) {
            this.colors = colors;
            this.nanoStyle = nanoStyle;
        }

        public String getStyle(String reference) {
            if (!colors.containsKey(reference)) {
                throw new IllegalStateException();
            }
            String rawStyle = colors.get(reference);
            if (!nanoStyle && rawStyle.matches(ANSI_VALUE)) {
                return rawStyle;
            }
            StringBuilder out = new StringBuilder();
            boolean first = true;
            boolean fg = true;
            for (String s : rawStyle.split(",")) {
                if (s.trim().isEmpty()) {
                    fg = false;
                    continue;
                }
                if (!first) {
                    out.append(",");
                }
                if (JLINE_NAMED_STYLES.contains(s)) {
                    out.append(s);
                } else if (COLORS_8.contains(s) || COLORS_NANO.containsKey(s) || s.startsWith("light")
                        || s.startsWith("bright") || s.startsWith("~") || s.startsWith("!") || s.matches("\\d+")
                        || s.equals("normal") || s.equals("default")) {
                    if (s.matches("\\d+") || COLORS_NANO.containsKey(s)) {
                        if (fg) {
                            out.append("38;5;");
                        } else {
                            out.append("48;5;");
                        }
                        out.append(s.matches("\\d+") ? s : COLORS_NANO.get(s).toString());
                    } else {
                        if (fg) {
                            out.append("fg:");
                        } else {
                            out.append("bg:");
                        }
                        if (COLORS_8.contains(s) || s.startsWith("~") || s.startsWith("!") || s.startsWith("bright-")) {
                            out.append(s);
                        } else if (s.startsWith("light")) {
                            out.append("!").append(s.substring(5));
                        } else if (s.startsWith("bright")) {
                            out.append("!").append(s.substring(6));
                        } else {
                            out.append("default");
                        }
                    }
                    fg = false;
                }
                first = false;
            }
            return out.toString();
        }
    }
}
