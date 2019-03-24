package com.mazhangjing.lab.launcher;

import java.util.HashMap;
import java.util.Map;

public class Config {
    Map<String, Bean> commandList = new HashMap<>();

    public Map<String, Bean> getCommandList() {
        return commandList;
    }

    public void setCommandList(Map<String, Bean> commandList) {
        this.commandList = commandList;
    }

    public Config() {
    }

    @Override
    public String toString() {
        return "Config{" +
                "commandList=" + commandList +
                '}';
    }
}
