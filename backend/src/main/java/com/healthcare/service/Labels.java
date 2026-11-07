package com.healthcare.service;

public final class Labels {
    private Labels() {}

    /** "high_fever" -> "High fever" (display only). */
    public static String symptom(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String s = name.replace('_', ' ').trim();
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
