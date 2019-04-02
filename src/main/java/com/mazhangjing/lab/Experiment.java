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

/**
 * <p>Experiment 类主要用来保存一组 Trial，并且提供和 GUI 交互的各种接口：
 * 得到一个Trial中的一个Screen对象，得到下一个Screen对象...得到下一个Trial，
 * 保存用户输入的数据..等等</p>
 * <p>实现指导：你需要实现这个抽象类的 initExperiment 方法，设置好一组实例变量并保存在 trials 列表中。你可以为此 Experiment 添加文字说明，保存在 information 字符串中。</p>
 * <p>使用方法：此类包含了一个内部类实现的遍历器，调用 getTrial 方法获取当前的 Trial 对象。多次调用不会导致指针改变，你需要手动调用 release 方法并且再次调用
 * getTrial 方法来获取下一个 Trial 对象。当全部获取完毕，返回 null。
 * <p>我们整合了Trial类的方法，避免其和GUI界面类耦合，因此，你可以直接调用 getScreen 方法返回第一个 Trial 的第一个 Screen。当第一个 Trial 对象的所有
 * Screen 对象遍历完毕，会自动调用内部迭代器返回下一个 Trial，当所有都调用完毕，则返回 null。</p>
 * <p>此类在 release 的基础上添加了一个 SimpleIntegerProperty 类型的 terminal 变量，用来指示实验情况。你应该在GUI类中为此变量添加监听器，以
 * 进行下列处理：当 Screen 类中的 eventHander 中 terminal 改变的情况出现时，调用 release 方法、
 * 通过调用 getScreen 方法以绘制新的Screen的Layout到当前GUI界面。为做到这一点，你应该在GUI中应实现一个计时器，将鼠标、键盘和计时器的事件统统通过
 * eventHandler 进行处理。这样的话，不论是用户响应界面还是超时，均会绘制下一个Screen</p>
 *
 * @author <a href='http://www.mazhangjing.com'>Corkine Ma</a>
 * @author Marvin Studio @ Central China Normal University
 * @version 1.1
 * */
public abstract class Experiment {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SimpleBooleanProperty canNext = new SimpleBooleanProperty(false);

    private Iterator<Trial> iterator;

    private String data;

    private class ExperimentIterator<Trial> implements Iterator {
        private Integer MAX = trials.size();
        private Integer POINT = 0;
        @Override
        public boolean hasNext() {
            if (POINT < MAX) return true;
            else return false;
        }
        @Override
        public Trial next() {
            if (canNext.get()) { POINT++; }
            //System.out.printf("P: %s, M: %s\n",POINT,MAX);
            if (POINT >= MAX) return null;
            else return (Trial) trials.get(POINT);
        }
    }

    /**保存试次的队列容器*/
    protected ArrayList<Trial> trials = new ArrayList<>();

    /**标记 Experiment 的附加信息，比如姓名，实验类型，实验日期等*/
    protected String information = "Experiment";

    /**你应该在此实现并且传递一组 Trial 类型的数据到 trials 变量中。*/
    protected abstract void initExperiment();

    public void resetExperiment() {
        System.out.println("[EXPERIMENT] Resting experiment. iterator now is null");
        iterator = null;
        canNext.set(false);
        for (Trial trial : trials) {
            trial.resetTrial();
        }
    }

    /**实验控制台，每个Experiment一个，用来指示当前实验的状态，0表示当前界面，1表示需要切换到下一个界面。
     * 控制台状态变化应该通过监听器调用 Experiment 中的 release 方法和 getScreen 方法来或许下一个 Screen。
     * 控制台的变化来自于计时器超时或者用户相应，这也需要通过监听器绑定。
     * 即：GUI组件变化 -- 控制台状态变化 -- 保存数据并且绘制下一个Screen或者进行其他相应（比如弹出对话框，给被试反馈等）。这些实现在GUI类中完成。
     **/
    volatile public SimpleIntegerProperty terminal = new SimpleIntegerProperty(0);

    /**默认构造器，会执行 initExperiment 方法来设置 trials 实例*/
    public Experiment() {
        initExperiment();
    }

    /**实现此方法来通过 getTrial 方法获取 Trial 对象，通过 getUserData 来保存数据到文件。一般应该对GUI关闭事件添加监听器完成此步骤*/
    public abstract void saveData();

    /**保存数据到当前的Trial中。
     * @param data 实验数据信息*/
    public final void setData(String data) {
        getTrial().setUserdata(data);
    }

    public final void setGlobalData(String data) {
        this.data = data;
    }

    public String getGlobalData() { return data; }

    /**@return 当前指针指向的 Trial 对象*/
    public final Trial getTrial() {
        Trial result = null;
        //当第一次调用时，初始化迭代器，放在这里的原因是，在构造器时initXXX，只有放在其后，迭代器才能获取内容
        if (iterator == null) {
            System.out.println("[EXPERIMENT] Setting experiment and getTrial. iterator now is null -- yes");
            iterator = new ExperimentIterator<Trial>();
            if (iterator.hasNext()) result = iterator.next();
            //System.out.println("iter has next?" + iterator.hasNext());
        //当有迭代器的情况下，返回一个合适的Trial(当前，下一个，null)
        } else if (iterator.hasNext()){
            result =  iterator.next();
        }
        canNext.set(false);
        return result;
    }

    /**@return 当前指针指向的 Trial 对象的指针指向的 Screen 对象*/
    public final Screen getScreen() {
        try{
            Screen result = null;
            result = getTrial().getScreen();
            //当result=null时，说明无法从此Trial获取更多的Screen，即遍历完毕
            //通过设置canNext，使迭代器返回下一个Trial
            if (result == null) {
                canNext.set(true);
                result = getTrial().getScreen();
            }
            return result;
        } catch (NullPointerException e) {
            //当遍历所有Trial的Screen完毕，使用getTrial会返回null，从null中进行getScreen会出错
            System.out.println("[EXPERIMENT] Run out.");
            return null;
        }
    }

    public final void initScreensAndTrials(Scene scene) {
        System.out.println("init now...");
        trials.forEach(trial -> {
            trial.setScene(scene);
            trial.setExperiment(this);
            trial.screens.forEach(screen -> {
                screen.setScene(scene);
                screen.setExperiment(this);
            });
        });
    }

    /**调用此方法手动移动指针，再次调用 getScreen 方法获取下一 Screen。不提供任何手动切换 Trials 的方法，必须当 getScreen 返回 null
     * 后才会自动调用下一个 Trial（canNext用来指示当前的Trial，这是个私有方法）*/
    public final void release() {
        getTrial().release();
    }

    public String toString() {
        return information;
    }

    public static void testRun(String[] args) {
        SimpleIntegerProperty terminal = new SimpleIntegerProperty(0);
        Screen screen1 = new Screen() {
            @Override
            public Screen initScreen() {
                duration = 300;
                layout = new FlowPane();
                return this;
            }

            @Override
            public void eventHandler(Event event, Experiment experiment, Scene scene) {

            }
        };
        Screen screen2 = new Screen() {
            @Override
            public Screen initScreen() {
                duration = 600;
                layout = new FlowPane();
                return this;
            }

            @Override
            public void eventHandler(Event event, Experiment experiment, Scene scene) {

            }
        };
        Trial trial = new Trial() {
            @Override
            public Trial initTrial() {
                screens.add(screen1);
                screens.add(screen2);
                return this;
            }
        };
        Trial trial2 = new Trial() {
            @Override
            public Trial initTrial() {
                screens.add(screen1);
                screens.add(screen2);
                return this;
            }
        };
        class MyExperiment extends Experiment {
            @Override
            protected void initExperiment() {
                trials.add(trial); trials.add(trial2);
            }

            @Override
            public void saveData() {

            }
        }
        Experiment experiment = new MyExperiment();
        //模拟GUI程序添加事件监听器用来自动解锁、翻页和绘制图像
        terminal.addListener(((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 1 && oldValue.intValue() == 0) {
                experiment.release();
            }
            terminal.set(0);
        }));
        System.out.println(experiment.getScreen());
        System.out.println(experiment.getScreen());
        experiment.release();
        experiment.release();
        System.out.println(experiment.getScreen());
        experiment.release();
        System.out.println(experiment.getScreen());
        //experiment.release();
        experiment.terminal.set(1);
        System.out.println(experiment.getScreen());
        System.out.println(experiment.terminal.get());
        experiment.release();
        System.out.println(experiment.getScreen());
    }

    /*

    @SuppressWarnings("unchecked")
    private static Class<? extends Config> getConfigClazz(File file) {
        try {
            HashMap hashMap = new Yaml().loadAs(new FileReader(file), HashMap.class);
            String configName = (String) hashMap.getOrDefault("configClassName","com.mazhangjing.lab.Config");
            return (Class<? extends Config>) Class.forName(configName);
        } catch (Exception ignored) {} return null;
    }

    public static void run() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        File file = Paths.get("/Users/corkine/工作文件夹/cmPsyLab/Lab/Lab/src/main/resources/config.yml").toFile();
        assert (file.exists());
        Config config = loadConfig(file, getConfigClazz(file));
        Setting setting = config.getSetting();
        String expClassName = setting.getExpClassName();
        Class<?> aClass = Class.forName(expClassName);
        Form form = ((Form) aClass.newInstance());
        Form.main(null);
    }

    public static void main3(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        File file = Paths.get("/Users/corkine/工作文件夹/cmPsyLab/Lab/Lab/src/main/resources/config.yml").toFile();
        assert (file.exists());
        Config config = loadConfig(file, getConfigClazz(file));
        Setting setting = config.getSetting();

        //Experiment experiment = (Experiment) Class.forName(setting.getExpClassName()).newInstance();
        Experiment experiment = null;
        FxRunner form = (FxRunner)
                Class.forName(config.getFormClassName(), true, ClassLoader.getSystemClassLoader()).newInstance();
        form.prepareExperiment(experiment);
    }*/
}
