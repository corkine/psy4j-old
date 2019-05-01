package com.mazhangjing.rhythm;

import org.junit.Before;
import org.junit.Test;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SequenceProcessImplTest {

    private SequenceProcess process;
    private Path obj = Paths.get("/Users/corkine/工作文件夹/Psy4J/src/test/mzj_data_process/30581.obj");
    private Path csv = Paths.get("/Users/corkine/工作文件夹/Psy4J/src/test/mzj_data_process/30581_record.csv");
    private Path path = Paths.get("/Users/corkine/工作文件夹/Psy4J/src/test/mzj_data_process");

   @Before
   public void before() {
        process = SequenceProcessFactory.newInstance(path,8);
    }

    @Test
    public void groupAlign() {
        ExperimentData data = process.groupAlign().apply(
                process.dataParser().apply(obj.toFile()),
                process.csvParser().apply(new File[]{csv.toFile()}));
        assertNotNull(data);
    }

    @Test
    public void handleGroup() {
       boolean apply = (boolean) process.handleGroup().apply(obj.toFile(),csv.toFile());
       assertTrue("应该只能解析 csv with obj",apply);
       assertFalse("应该只能解析 csv with obj",(boolean) process.handleGroup().apply(csv.toFile(), csv.toFile()));
       assertFalse("应该只能解析 csv with obj",(boolean) process.handleGroup().apply(obj.toFile(), obj.toFile()));
    }

    @Test
    public void handleFolder() {
        assertTrue(process.handleFiles().length != 0);
    }

    @Test
    public void doProcess() {
        ExperimentData[] experimentData = process.doProcess();
        assertNotNull(experimentData);
    }

    @Test
    public void fileGrouper() throws IOException {
       List<File> list = new ArrayList<>();
       Files.walkFileTree(path, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                list.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;

            }
        });
        Tuple2<File, File[]>[] res = process.groupSeeker().apply(list.toArray(new File[]{}), process.handleGroup());
        for (Tuple2<File, File[]> tuple2 : res) {
            File file = tuple2._1;
            File[] files = tuple2._2;
            System.out.println("With Obj " + file + ", and CSVS is " + Arrays.toString(files));
            assertEquals("一个 Object 只应该对应一个或者两个 csv 文件，不包括其自身的 csv，但包括 record_csv",
                    1, files.length);
        }
    }

    @Test
    public void csvParser() {
        Tuple2<Instant, Object>[] s = process.csvParser().apply(new File[]{csv.toFile()});
        int count = 10;
        for (Tuple2<Instant, Object> data : s) {
            count += 1;
            if (count < 10) System.out.println("data = " + data);
            else break;
        }
    }

    @Test
    public void dataParser() {
        ExperimentData ex = process.dataParser().apply(obj.toFile());
        assertEquals(ex.getUserId(), 30581);
        assertTrue(ex.getTrialData().size() > 20);
    }
}