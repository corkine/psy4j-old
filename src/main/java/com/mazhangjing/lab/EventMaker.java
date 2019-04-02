package com.mazhangjing.lab;

import javafx.scene.Scene;

/**
 * EventMaker 是一个在 SimpleExperimentHelperImpl 中触发的，独立运行的线程，其可以通过 Experiment.initStage 向 Scene 中发送 event，调用
 * scene.eventHandler 来向当前显示的 Screen 中传递事件。
 * @apiNote  继承此类，并且重写 run 方法，当满足条件后，调用 experiment 以向 Screen 传递事件。
 *           定义完毕此类后，需要将此类注册到 ExpRunner 的 eventMakerSet 属性中，以便于 SimpleExperimentHelperImpl 从 classpath 下的 invoke.properties
 *           文件中反射创建 ExpRunner 后，调用 eventMakerSet 反射创建 EventMaker，并且初始化并调用。
 * <pre>{@code
 * public class VoiceEventMaker extends EventMaker {
 *
 *     private Logger logger = LoggerFactory.getLogger(VoiceEventMaker.class);
 *
 *     public VoiceEventMaker(Experiment experiment, Scene scene) {
 *         super(experiment, scene);
 *     }
 *
 *     @Override public void run() {
 *         LiveSpeechRecognizer recognizer = initRecognizer();
 *         logger.debug("invoke runnable task now...");
 *         while (true) {
 *             assert recognizer != null;
 *             SpeechResult result = recognizer.getResult();
 *             logger.info("Get result" + result.getWords());
 *             if (result.getWords() != null) {
 *                 logger.info("Get result with not null");
 *                 Objects.requireNonNull(experiment.getScreen()).eventHandler(new Event(Event.ANY), experiment, scene);
 *             }
 *         }
 *     }
 *
 *     private static LiveSpeechRecognizer initRecognizer() {
 *         Configuration configuration = new Configuration();
 *         configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
 *         configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
 *         configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
 *         LiveSpeechRecognizer recognizer = null;
 *         try {
 *             recognizer = new LiveSpeechRecognizer(configuration);
 *             recognizer.startRecognition(false);
 *             return recognizer;
 *         } catch (IOException e) {
 *             e.printStackTrace();
 *         } return null;
 *     }
 * }}</pre>
 */
public abstract class EventMaker implements Runnable {
    protected Experiment experiment;
    protected Scene scene;
    public EventMaker(Experiment experiment, Scene scene) {
        this.experiment = experiment;
        this.scene = scene;
    }
}

/*package com.mazhangjing;

import com.mazhangjing.lab.EventMaker;
import com.mazhangjing.lab.Experiment;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import javafx.event.Event;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class VoiceEventMaker extends EventMaker {

    private Logger logger = LoggerFactory.getLogger(VoiceEventMaker.class);

    public VoiceEventMaker(Experiment experiment, Scene scene) {
        super(experiment, scene);
    }

    @Override
    public void run() {
        LiveSpeechRecognizer recognizer = initRecognizer();
        logger.debug("invoke runnable task now...");
        while (true) {
            assert recognizer != null;
            SpeechResult result = recognizer.getResult();
            logger.info("Get result" + result.getWords());
            if (result.getWords() != null) {
                logger.info("Get result with not null");
                Objects.requireNonNull(experiment.getScreen()).eventHandler(new Event(Event.ANY), experiment, scene);
            }
        }
    }

    private static LiveSpeechRecognizer initRecognizer() {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        LiveSpeechRecognizer recognizer = null;
        try {
            recognizer = new LiveSpeechRecognizer(configuration);
            // Start recognition process pruning previously cached data.
            recognizer.startRecognition(false);
            return recognizer;
        } catch (IOException e) {
            e.printStackTrace();
        } return null;
    }

}*/

