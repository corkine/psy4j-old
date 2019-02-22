package com.mazhangjing.lab;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Trial 类主要用来抽象试次：包含并且遍历一组Screen，接受用户实验的数据</p>
 * <p>实现指导：你需要实现这个抽象类的 initTrial 方法，设置好一组实例变量并保存在 screens 列表中。你可以为此 Trial 添加文字说明，保存在 information 字符串中。</p>
 * <p>使用方法：此类包含了一个内部类实现的遍历器，调用 getScreen 方法获取当前的 Screen 对象。多次调用不会导致指针改变，你需要手动调用 release 方法并且再次调用
 * getScreen 方法来获取下一个 Screen 对象。当全部获取完毕，返回 null。
 * <p>我们提供了获取和保存用户数据的方法 getSetUserData() 但是你一般不需要使用它。此类不和GUI直接交互，其仅和 Experiment 耦合，一般情况下，你只需要为 Experiment 提供实现好 Screen 的 Trial 对象即可。</p>
 *
 * @author <a href='http://www.mazhangjing.com'>Corkine Ma</a>
 * @author Marvin Studio @ Central China Normal University
 * @version 1.1
 * */
public abstract class Trial {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private Experiment experiment;

    private Scene scene;

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        System.out.println("Setting trial now...");
        this.experiment = experiment;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    private SimpleBooleanProperty canNext = new SimpleBooleanProperty(false);

    private Iterator<Screen> iterator;

    private String userData;

    private class TrialIterator<Screen> implements Iterator {
        private Integer MAX = screens.size();
        private Integer POINT = 0;
        @Override
        public boolean hasNext() {
            if (POINT < MAX) return true;
            else return false;
        }
        @Override
        public Screen next() {
            if (canNext.get()) { POINT++; }
            if (POINT >= MAX) return null;
            return (Screen) screens.get(POINT);
        }
    }

    /**一个用来保存Screen对象的容器*/
    protected List<Screen> screens = new ArrayList<>();

    /**一个用来指明当前Trial含义的辅助标签字符串，使用 toString 方法调用*/
    protected String information = "Trial";

    /**你应该实现这个方法，用来保存一组 Screen 对象到 screens 中，保存 Trial 的信息到 information 中。*/
    public abstract Trial initTrial();

    public void resetTrial() {
        System.out.println("[TRIAL] Reset trial iterator...");
        iterator = null;
        canNext.set(false);
    }

    /**公共构造器，通过调用 initTrial 方法来实例化 screens 变量*/
    public Trial() { }

    /**@return 通过内部迭代器返回当前指针指向的 Screen 对象，多次调用这个方法返回值相同*/
    public final Screen getScreen() {
        Screen result = null;
        if (iterator == null) {
            iterator = new TrialIterator<Screen>();
            if (iterator.hasNext()) result = iterator.next();
        }
        else if (iterator.hasNext()) result =  iterator.next();
        canNext.set(false);
        return result;
    }

    /**@return 返回保存的用户数据*/
    public final String getUserdata() {
        return userData;
    }

    /**设置用户实验数据
     * @param userData 用户数据字符串*/
    public final void setUserdata(String userData) {
        this.userData = userData;
    }

    /**用来改变指针位置的方法，调用后再调用 getScreen 会指向下一个 Screen*/
    public final void release() { this.canNext.set(true);}

    public String toString() {
        return information;
    }

}
