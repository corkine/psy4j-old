package com.mazhangjing.rhythm;

import org.junit.Before;
import org.junit.Test;
import scala.Function1;
import scala.None;
import scala.Option;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SequenceProcessFactoryTest {

    private Path path = Paths.get("/Users/corkine/工作文件夹/Psy4J/src/test/mzj_data_process");

    private SequenceProcess process;

    @Before public void doInit() {
        process = SequenceProcessFactory.newInstance(path,-8,file -> {
            if (file.getName().contains("mac")) {
                System.out.println("Find Time For MAC, ADJUST FOR 0");
                return Option.apply(0);
            } else return Option.empty();
        });
    }

    @Test
    public void printToCSV() {
        Path path = Paths.get("result.csv");
        ExperimentData[] datas = process.doProcess();
        SequenceProcessFactory.printToCSV(datas, path);
        assertTrue(path.toFile().exists());
    }

    @Test public void printToCSVWithSPSS() {

    }
}