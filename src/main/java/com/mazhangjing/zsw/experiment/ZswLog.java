package com.mazhangjing.zsw.experiment;

import com.mazhangjing.lab.Log;

public class ZswLog implements Log {

    private final String VERSION = "2.0.1";

    private final String LOG = "2018-12-01 1.0.0 编写程序\n" +
            "2018-12-02 1.0.1 处理多线程计时器 BUG。优化 Main 代码。添加日志系统。\n" +
            "2018-12-14 1.5.0 重设了错误页面字体颜色。更改鼠标移入事件为鼠标点击事件检测。修复了点击后如果正确立马回到界面的代码。修复了异步展示先展示的刺激之后不展示的问题。\n" +
            "2018-12-14 1.5.2 修复了异步状态相同字体的问题。\n" +
            "2018-12-24 2.0.1 修改了刺激材料，现在有 384 个试次。添加指导语部分、休息试次，添加用户鼠标反应监测，添加了练习试次，添加了日志收集系统。";

    @Override
    public String getCurrentVersion() {
        return VERSION;
    }

    @Override
    public String getLog() {
        return LOG;
    }

    @Override
    public String getCopyRight() {
        return "The copyright of this software belongs to the Virtual Behavior Laboratory of the School of Psychology, Central China Normal University. Any unauthorized copy of the program or copy of the code will be legally held liable.\n" +
                "" +
                "The software is based on the Java platform design and Java TM is a registered trademark of Oracle Corporation.\n" +
                "" +
                "The software is based on the Java FX framework and Java FX is based on the GNU v2 distribution protocol.\n" +
                "" +
                "The software is based on the PSY4J framework design. PSY4J is the work of Corkine Ma, which allows binary packages and source code to be used, but the source code must not be tampered with in any way and closed source.\n" +
                "" +
                "Contact: psy4j@mazhangjing.com and Support site: www.mazhangjing.com" +
                "" +
                " © Marvin Studio 2018";
    }
}
