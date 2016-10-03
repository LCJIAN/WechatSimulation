package com.lcjian.wechatsimulation;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Manager {

    private static final SharedPreferences SIMULATION_DATABASE_PREFERENCES = APP.getInstance().getSharedPreferences("simulation_database", Context.MODE_PRIVATE);

    public static void addGroupChatMembers(Set<String> names) {
        Set<String> allNames = SIMULATION_DATABASE_PREFERENCES.getStringSet("all_group_chat_member", new HashSet<String>());
        allNames.addAll(names);

        SIMULATION_DATABASE_PREFERENCES
                .edit()
                .putStringSet("all_group_chat_member", allNames)
                .apply();
    }

    public static boolean containGroupChatMember(String name) {
        return SIMULATION_DATABASE_PREFERENCES.getStringSet("all_group_chat_member", new HashSet<String>()).contains(name);
    }

    public static void setCurrentWebContentHeight(int height) {
        SIMULATION_DATABASE_PREFERENCES.edit().putInt("current_web_content_height", height).apply();
    }

    public static int getCurrentWebContentHeight() {
        int height = SIMULATION_DATABASE_PREFERENCES.getInt("current_web_content_height", 0);
        SIMULATION_DATABASE_PREFERENCES.edit().remove("current_web_content_height").apply();
        return height;
    }
}
