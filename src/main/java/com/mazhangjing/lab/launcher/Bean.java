package com.mazhangjing.lab.launcher;

public class Bean {
    String name;
    String command;

    public Bean(String name, String command) {
        this.name = name;
        this.command = command;
    }

    public Bean() {
    }

    @Override
    public String toString() {
        return "Bean{" +
                "name='" + name + '\'' +
                ", command='" + command + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
