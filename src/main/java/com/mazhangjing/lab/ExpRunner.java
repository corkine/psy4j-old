package com.mazhangjing.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Experiment 类的一个辅助类，用于在 Experiment 和 SimpleExperimentHelperImpl 之间建立连接。在 SimpleExperimentHelperImpl 中通过反射创建此 ExpRunner，
 * 之后，从 ExpRunner 中获取 title、log、EventMaker Set 等信息，初始化 Experiment 实例，开始试验。
 *
 * <pre>{@code
 * public class ZswExpRunner extends ExpRunner {
 *
 *     @Override
 *     public void initExpRunner() {
 *         setTitle("WSH EXP");
 *         setVersion("0.0.2");
 *         setFullScreen(false);
 *         setExperimentClassName("com.mazhangjing.wsh.experiment.WshRealExperiment");
 *
 *         Set<OpenedEvent> openedSet = new HashSet<>();
 *         //openedSet.add(OpenedEvent.MOUSE_MOVE);
 *         openedSet.add(OpenedEvent.KEY_PRESSED);
 *         openedSet.add(OpenedEvent.MOUSE_CLICK);
 *         setOpenedEventSet(openedSet);
 *
 *         Set<String> makerSet = new HashSet<>();
 *         makerSet.add("com.mazhangjing.demo.VoiceEventMaker");
 *         setEventMakerSet(makerSet);
 *     }
 * }
 * }</pre>
 */
public abstract class ExpRunner {
    /**
     * SimpleExperimentHelperImpl 中传递给 Screen 事件的触发器的集合，其会在 SimpleExperimentHelperImpl 中作为独立线程运行，比如语音检测，如果有事件发生，
     * 则调用自身实现的 makeEvent 方法将事件传递给 Screen、Experiment 等。
     */
    private Set<String> eventMakerSet;
    /**
     * 允许 GUI 向 Screen 传播的 JavaFx 内置事件，比如鼠标点击、鼠标移动、按键按下等
     */
    private Set<OpenedEvent> openedEventSet;
    /**
     * 当前的实验对象
     */
    private String experimentClassName;
    /**
     * 实验的标题，会显示在 GUI 窗口的标题栏
     */
    private String title = "DEMO";
    /**
     * 实验的版本，会显示在 GUI 窗口的标题栏，以及 SimpleExperimentHelperImpl 启动的首屏幕上。
     */
    private String version = "0.0.1";
    /**
     * 实验的更新日志，在 SimpleExperimentHelperImpl 启动后的首屏幕上显示。
     */
    private List<String> logs = new ArrayList<>();

    {
        logs.add("0.0.1 编写实验");
    }

    private Boolean fullScreen = false;

    public ExpRunner() {
        initExpRunner();
    }

    /**
     * 在其中初始化 eventMakerSet， openedEventSet， experiment，title，version，logs，fullScreen
     */
    public abstract void initExpRunner();

    public Boolean getFullScreen() {
        return fullScreen;
    }

    public void setFullScreen(Boolean fullScreen) {
        this.fullScreen = fullScreen;
    }

    public Set<String> getEventMakerSet() {
        return eventMakerSet;
    }

    public void setEventMakerSet(Set<String> eventMakerSet) {
        this.eventMakerSet = eventMakerSet;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<OpenedEvent> getOpenedEventSet() {
        return openedEventSet;
    }

    public void setOpenedEventSet(Set<OpenedEvent> openedEventSet) {
        this.openedEventSet = openedEventSet;
    }

    public String getExperimentClassName() {
        return experimentClassName;
    }

    public void setExperimentClassName(String experimentClassName) {
        this.experimentClassName = experimentClassName;
    }

    @Override
    public String toString() {
        return "ExpRunner{" +
                "eventMakerSet=" + eventMakerSet +
                ", openedEventSet=" + openedEventSet +
                ", experimentClassName='" + experimentClassName + '\'' +
                ", title='" + title + '\'' +
                ", version='" + version + '\'' +
                ", logs=" + logs +
                ", fullScreen=" + fullScreen +
                '}';
    }
}
/*
package com.mazhangjing;

import com.mazhangjing.lab.ExpRunner;
import com.mazhangjing.lab.OpenedEvent;

import java.util.HashSet;
import java.util.Set;

public class ZswExpRunner extends ExpRunner {

    @Override
    public void initExpRunner() {
        setTitle("WSH EXP");
        setVersion("0.0.2");
        setFullScreen(false);
        setExperimentClassName("com.mazhangjing.wsh.experiment.WshRealExperiment");

        Set<OpenedEvent> openedSet = new HashSet<>();
        //openedSet.add(OpenedEvent.MOUSE_MOVE);
        openedSet.add(OpenedEvent.KEY_PRESSED);
        openedSet.add(OpenedEvent.MOUSE_CLICK);
        setOpenedEventSet(openedSet);

        Set<String> makerSet = new HashSet<>();
        makerSet.add("com.mazhangjing.demo.VoiceEventMaker");
        setEventMakerSet(makerSet);
    }
}*/

