package com.mazhangjing.zsw.experiment;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Trial;
import com.mazhangjing.zsw.screen.RelaxScreen;
import com.mazhangjing.zsw.sti.Array;
import com.mazhangjing.zsw.sti.StiFactory;
import com.mazhangjing.zsw.trial.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ZswExperiment extends Experiment {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void initExperiment() {
        //信息收集试次
        trials.add(new InfoTrial().initTrial());

        //练习试次
        trials.add(new ReadyTrial(false).initTrial());
        List<Array> testArrays = StiFactory.getTestArrays();
        testArrays.forEach(array -> {
            trials.add(new TestBasicTrial(array).initTrial());
        });

        //正式试次和休息试次
        trials.add(new ReadyTrial(true).initTrial());
        List<Array> arrays = StiFactory.getArrays();
        //arrays.forEach( array -> trials.add(new BasicTrial(array).initTrial()));
        for (int i = 0; i < arrays.size(); i++) {
            if (i % 96 == 0 && i != 0  && i != arrays.size() - 1)
                trials.add(new RelaxTrial().initTrial());
            trials.add(new BasicTrial(arrays.get(i)).initTrial());
        }
    }

    @Override
    public void saveData() {
        File data = Paths.get(System.getProperty("user.dir") + File.separator + "log/logFile.log").toFile();
        logger.info(data.toString());
        String info = Optional.ofNullable(getGlobalData())
                    .filter(data1 -> !data1.isEmpty()).filter(data2 -> !data2.equals("__"))
                    .orElse(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm")));
        Path newPath = Paths.get(System.getProperty("user.dir") + File.separator + "log" + File.separator + info + ".log");
        if (data.exists()) {
            logger.info("Log file Exist, moving now");
            try {
                Files.copy(data.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.info("Log file move failed.");
                e.printStackTrace();
            }
        } else {
            logger.info("Log file not exist!!");
        }

    }
}
