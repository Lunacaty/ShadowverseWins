package com.shadowverse;

import com.shadowverse.utils.API;
import com.shadowverse.utils.Response;
import org.htmlunit.javascript.host.speech.SpeechSynthesisUtterance;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String[] MORE = {"5", "10"};
    private static final String[] MPS = {"B", "A", "AA", "Master"};
    private static final String[] GROUPS = {"ruby", "sapphire", "diamond"};
    //依次是:妖精,皇家,法师,龙族,主教,梦魇,超越
    private static final String[] LEADER_TYPES = {"E", "R", "W", "D", "B", "Nmr", "Nm"};
    private static final Map<String, Integer> RESULT = new HashMap<>();
    private static final Map<String, Double> RESULT_PERCENT = new HashMap<>();
    private static final Map<String, Integer> RESULT_DIAMOND = new HashMap<>();
    private static final Map<String, Double> RESULT_DIAMOND_PERCENT = new HashMap<>();
    private static final API api = new API("https://shadowverse-wins.com");
    private static final String[][] START_END = {{null, null, null}, {null, null, null}};
    
    static {
        RESULT.put("妖精", 0);
        RESULT.put("皇家", 0);
        RESULT.put("法师", 0);
        RESULT.put("龙族", 0);
        RESULT.put("主教", 0);
        RESULT.put("梦魇", 0);
        RESULT.put("超越", 0);
        RESULT_PERCENT.put("妖精", 0.0);
        RESULT_PERCENT.put("皇家", 0.0);
        RESULT_PERCENT.put("法师", 0.0);
        RESULT_PERCENT.put("龙族", 0.0);
        RESULT_PERCENT.put("主教", 0.0);
        RESULT_PERCENT.put("梦魇", 0.0);
        RESULT_PERCENT.put("超越", 0.0);
        RESULT_DIAMOND.put("妖精", 0);
        RESULT_DIAMOND.put("皇家", 0);
        RESULT_DIAMOND.put("法师", 0);
        RESULT_DIAMOND.put("龙族", 0);
        RESULT_DIAMOND.put("主教", 0);
        RESULT_DIAMOND.put("梦魇", 0);
        RESULT_DIAMOND.put("超越", 0);
        RESULT_DIAMOND_PERCENT.put("妖精", 0.0);
        RESULT_DIAMOND_PERCENT.put("皇家", 0.0);
        RESULT_DIAMOND_PERCENT.put("法师", 0.0);
        RESULT_DIAMOND_PERCENT.put("龙族", 0.0);
        RESULT_DIAMOND_PERCENT.put("主教", 0.0);
        RESULT_DIAMOND_PERCENT.put("梦魇", 0.0);
        RESULT_DIAMOND_PERCENT.put("超越", 0.0);
    }
    
    public static void main(String[] args) {
        Main main = new Main();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("请输入起始时间,格式为yyyy/MM/dd");
            START_END[0] = proTime(scanner.nextLine());
            System.out.println("请输入结束时间,格式为yyyy/MM/dd");
            START_END[1] = proTime(scanner.nextLine());
            
            if(START_END[0] == null || START_END[1] == null){
                System.out.println("输入的时间格式不正确,请重新输入");
                continue;
            }
            
            int diff = 0;
            for (int i = 0; i < 3; i++) {
                diff += (Integer.parseInt(START_END[1][i]) - Integer.parseInt(START_END[0][i])) * (i == 0 ? 365 : i == 1 ? 30 : 1);
            }
            if (diff < 0) {
                System.out.println("结束时间不能早于起始时间");
                continue;
            }
            System.out.println("查询时间范围:" + START_END[0][0] + "年" + START_END[0][1] + "月" + START_END[0][2] + "日至" + START_END[1][0] + "年" + START_END[1][1] + "月" + START_END[1][2] + "日");
            break;
        }
        
        String finalMp;
        String finalGroup;
        String finalMore = "";
        String finalSeasonId = "";
        while (true) {
            System.out.println("请输入查询段位(B,A,AA,Master.对于任何段位,都会同时查询其之后的段位(例如输入A会同时查询AA与Master段的数据,这与程序设计者无关,是网站本身的设计),不输入默认同时查询后三者)");
            String mp = scanner.nextLine();
            mp = mp.isEmpty() ? "A" : mp;
            System.out.println("请输入查询分组(topaz(黄宝石),ruby(红宝石),sapphire(蓝宝石),diamond(钻石).对于任何分组,都会同时查询其之后的分组(例如输入ruby会同时查询sapphire与diamond组的数据,这与程序设计者无关,是网站本身的设计),不输入默认同时查询后二者)");
            String group = scanner.nextLine();
            group = group.isEmpty() ? "sapphire" : group;
            System.out.println("请输入查询连胜数(5,10.对于任何连胜数,后面忘了总之不是我的锅,不输入默认查询10连胜以上)");
            String more = scanner.nextLine();
            more = more.isEmpty() ? "5" : more;
            System.out.println("请输入查询卡包ID,传说揭幕包为61,无限进化为62,以此类推,不输入就画个圈圈诅咒你");
            String seasonId = scanner.nextLine();
            if (!hasItem(MPS, mp) || !hasItem(GROUPS, group) || !hasItem(MORE, more) || seasonId.isEmpty()) {
                System.out.println("输入的段位/分组/连胜数不存在或未指定卡包ID,请重新输入");
                continue;
            }
            finalMp = mp;
            finalGroup = group;
            System.out.println("确认查询条件:" + finalMp + "段|" + finalGroup + "组|" + "连胜数:" + more + "|卡包ID:" + seasonId);
            break;
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(16);
        
        
        
        
        for (String leaderType : LEADER_TYPES) {
            executor.submit(() -> {
                System.out.println("爬取" + leader(leaderType) + " " + finalMp + " " + finalGroup + "组" + "开始");
                try {
                    //我该把这一大串参数都放到static变量里的
                    main.get(finalGroup, finalMp, leaderType, accumulatePage(finalGroup, finalMp, leaderType,finalMore,finalSeasonId),finalMore,finalSeasonId);
                    if (!"diamond".equals(finalGroup)) {
                        System.out.println("准备查询钻石组数据");
                        main.get("diamond", finalMp, leaderType, accumulatePage("diamond", finalMp, leaderType, finalMore, finalSeasonId), finalMore, finalSeasonId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("爬取" + leader(leaderType) + " " + finalMp + " " + finalGroup + "组" + "结束");
            });
        }
        
        
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        int total = 0;
        int totalDiamond = 0;
        
        
        System.out.println(START_END[0][0] + "年" + START_END[0][1] + "月" + START_END[0][2] + "日至" + START_END[1][0] + "年" + START_END[1][1] + "月" + START_END[1][2] + "日");
        
        
        if (!finalGroup.equals("diamond")){
            for (String key : RESULT.keySet()) {
                total += RESULT.get(key);
            }
            System.out.println("总计:" + total + "条数据");
            for (String key : RESULT.keySet()) {
                System.out.println(key + ":" + RESULT.get(key));
            }
            for (String key : RESULT.keySet()) {
                RESULT_PERCENT.put(key, RESULT.get(key).doubleValue() / total);
            }
            for (String key : RESULT_PERCENT.keySet()) {
                System.out.println(key + ":" + RESULT_PERCENT.get(key));
            }
        }
        for (String key : RESULT_DIAMOND.keySet()) {
            totalDiamond += RESULT_DIAMOND.get(key);
        }
        System.out.println("钻石组数据,共" + totalDiamond + "条");
        for (String key : RESULT_DIAMOND.keySet()) {
            System.out.println(key + ":" + RESULT_DIAMOND.get(key));
        }
        for (String key : RESULT_DIAMOND.keySet()) {
            RESULT_DIAMOND_PERCENT.put(key, RESULT_DIAMOND.get(key).doubleValue() / totalDiamond);
        }
        for (String key : RESULT_DIAMOND_PERCENT.keySet()) {
            System.out.println(key + ":" + RESULT_DIAMOND_PERCENT.get(key));
        }
        System.out.println("输入回车退出程序");
        scanner.nextLine();
        scanner.close();
    }
    
    public void get(String group, String mp, String leader, Integer start,String more,String seasonId) {
        if (start == null) {
            return;
        }
        
        Response res;
        Document doc;
        System.out.println("开始爬取" + leader(leader) + mp + " " + group + "组" + "数据");
        for (int i = start; true; i++) {
            System.out.println("正在爬取第" + i + "页数据...");
            res = api.GET("/", "group=" + group, "leader=" + leader, "format=rotation", "mp=" + mp, "seasonId=" + seasonId, "more=" + more, "page=" + i);
            doc = Jsoup.parse(res.getData().toString());
            Elements sections = doc.select("section");
            if (sections.isEmpty()) {
                System.out.println("爬取至末尾页");
                break;
            }
            boolean isEnd = true;
            for (Element section : sections) {
                Elements timestamp = section.select(".timestamp");
                if (timestamp.isEmpty()) {
                    continue;
                }
                Elements as = timestamp.select("a");
                if (as.isEmpty()) {
                    continue;
                }
                String[] time = null;
                for (Element front : as) {
                    String frontText = front.text();
                    System.out.println("第" + i + "页第" + sections.indexOf(section) + "条数据,时间为:" + frontText);
                    if (frontText.isEmpty()) {
                        continue;
                    }
                    time = proTime(frontText.split(" ")[0]);
                }
                if (!timeLegal(time)) {
                    System.out.println("数据时间不在指定范围内,跳过");
                    continue;
                }
                isEnd = false;
                System.out.println("数据时间在指定范围内,有效");
                if (group.equals("diamond")) {
                    RESULT_DIAMOND.put(leader(leader), RESULT_DIAMOND.get(leader(leader)) + 1);
                    //若是钻石组,只统计钻石组结果即可
                    continue;
                }
                RESULT.put(leader(leader), RESULT.get(leader(leader)) + 1);
                
            }
            if (isEnd) {
                System.out.println("爬取至末尾页");
                break;
            }
        }
        
    }
    
    public static Integer accumulatePage(String group, String mp, String leader, String more, String seasonId) {
        System.out.println("正在估计起始页数...");
        int start = 0;
        int end = 0;
        Integer diff = calculateDelTime(1, group, mp, leader, 1, more, seasonId);
        if (diff == null) {
            System.out.println("无数据");
            return null;
        }
        if (diff < 0) {
            diff = 0;
        }
        
        
        //估计系数,递减以模拟估计越来越保守
        double base = 0.5;
        System.out.println("首次估计起始页为:" + ((int) (diff * base) + 1));
        //diff*base:首次估计,每一页的数据时间差为0.5天
        for (int i = (int) (diff * base) + 1; true; ) {
            
            if ((isPageLegal(mp, group, leader, i, more, seasonId) || diff == 0) && (!isPageLegal(mp, group, leader, i - 1,more,seasonId) || i == 1)) {
                start = i;
                System.out.println("起始页为:" + start);
            }
            
            if (
                    !isPageLegal(mp, group, leader, i, more, seasonId) &&
                            calculateDelTime(i, group, mp, leader, 1, more,seasonId) != null &&
                            calculateDelTime(i, group, mp, leader, 1, more,seasonId) > 0 &&
                            (calculateDelTime(i + 1, group, mp, leader, 1, more,seasonId) == null
                                    || (
                                    calculateDelTime(i + 1, group, mp, leader, 1, more,seasonId) != null
                                            && calculateDelTime(i + 1, group, mp, leader, 1, more,seasonId) < 0
                                            && !isPageLegal(mp, group, leader, i + 1,more,seasonId)
                            )
                            )
            ) {
                System.out.println("无数据");
                return null;
            }
            
            if (start != 0) {
                break;
            }
            
            
            if (base > 0.3) {
                Integer cal = calculateDelTime(i, group, mp, leader, 1, more,seasonId);
                System.out.println("第" + i + "页首条数据,与末尾时间差为:" + cal);
                if (cal == null || (cal == 0 && i > 1)) {
                    i--;
                    System.out.println("尝试调整起始页为:" + i);
                    base -= 0.05;
                    continue;
                }
                int temp = i;
                i += (int) (cal * base);
                if (i < 1) {
                    i = temp;
                    i--;
                }
                base -= 0.05;
                System.out.println("尝试调整起始页为:" + i);
            } else {
                //估计若干次后,不再优化,逐一递减/增
                System.out.println("估算次数达最大值,尝试逐一递减/增");
                Integer cal = calculateDelTime(i, group, mp, leader, 1, more,seasonId);
                System.out.println("第" + i + "页首条数据,与末尾时间差为:" + cal);
                if (cal == null) {
                    i--;
                    System.out.println("尝试调整起始页为:" + i);
                    continue;
                }
                if (cal > 0) {
                    i++;
                } else {
                    i--;
                }
                System.out.println("尝试调整起始页为:" + i);
            }
        }
        //只需要找出起始页就够了,该部分代码弃用
//        System.out.println("正在估计末尾页数...");
//        diff = calculateDelTime(start, group, mp, leader, 0);
//        if (diff == null || diff < 0) {
//            diff = 0;
//            System.out.println("无数据");
//            return null;
//        }
//        System.out.println("首次估计末尾页为:" + (start + diff));
//        for (int i = start + (int)(diff * base) + 1; true; ) {
//            if (isPageLegal(mp, group, leader, i) && !isPageLegal(mp, group, leader, i + 1)) {
//                end = i;
//                System.out.println("末尾页为:" + end);
//            }
//
//            if (end != 0) {
//                break;
//            }
//
//
//            if (base > 0.3) {
//                int temp = i;
//                Integer cal = calculateDelTime(i, group, mp, leader, 0);
//                if (cal == null) {
//                    i--;
//                    System.out.println("尝试调整末尾页为:" + i);
//                    continue;
//                }
//                i += (int) (cal * base);
//                base -= 0.1;
//                if (i < start) {
//                    i = temp;
//                    i--;
//                }
//                System.out.println("尝试调整末尾页为:" + i);
//            } else {
//                //估计若干次后,不再优化,逐一递减/增
//                Integer cal = calculateDelTime(i, group, mp, leader, 0);
//                System.out.println("估算次数达最大值,尝试逐一递减/增");
//                if (cal == null || cal < 0) {
//                    i--;
//                } else {
//                    i++;
//                }
//                System.out.println("尝试调整末尾页为:" + i);
//            }
//        }
        System.out.println("计算完成,起始页:" + start);
        return start;
    }
    
    public static Integer calculateDelTime(int page, String group, String mp, String leader,int mode,String more, String seasonId) {
        Response res = api.GET("/", "group=" + group, "leader=" + leader, "format=rotation", "mp=" + mp, "seasonId=" + seasonId, "more=" + more, "page=" + page);
        Document doc = Jsoup.parse(res.getData().toString());
        Elements sections = doc.select("section");
        if (sections.isEmpty()) {
            System.out.println("第" + page + "页无数据");
            return null;
        }
        for (Element section : sections) {
            Elements timestamp = section.select(".timestamp");
            if (timestamp.isEmpty()) {
                continue;
            }
            Elements a = timestamp.select("a");
            if (a.isEmpty()) {
                continue;
            }
            String[] time = null;
            for (Element front : a) {
                String frontText = front.text();
                if (frontText.isEmpty()) {
                    continue;
                }
                time = proTime(frontText.split(" ")[0]);
            }
            Integer diff = 0;
            for (int j = 0; j < 3; j++) {
                diff += (Integer.parseInt(time[j]) - Integer.parseInt(START_END[mode][j])) * (j == 0 ? 365 : j == 1 ? 30 : 1);
            }
            return diff;
        }
        return null;
    }
    
    public static boolean isPageLegal(String mp, String group, String leader, int page,String more, String seasonId) {
        Response res = api.GET("/", "group=" + group, "leader=" + leader, "format=rotation", "mp=" + mp, "seasonId=" + seasonId, "more=" + more, "page=" + page);
        Document doc = Jsoup.parse(res.getData().toString());
        Elements sections = doc.select("section");
        if (sections.isEmpty()) {
//            System.out.println(api.getLastUrl());
            return false;
        }
        for (Element section : sections) {
            Elements timestamp = section.select(".timestamp");
            if (timestamp.isEmpty()) {
                continue;
            }
            Elements as = timestamp.select("a");
            if (as.isEmpty()) {
                continue;
            }
            String[] time = null;
            for (Element front : as) {
                String frontText = front.text();
                if (frontText.isEmpty()) {
                    continue;
                }
                time = proTime(frontText.split(" ")[0]);
            }
            if (timeLegal(time)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean timeLegal(String[] time) {
        int diff1 = 0;
        for (int j = 0; j < 3; j++) {
            diff1 += (Integer.parseInt(START_END[1][j]) - Integer.parseInt(time[j])) * (j == 0 ? 365 : j == 1 ? 30 : 1);
        }
        int diff2 = 0;
        for (int j = 0; j < 3; j++) {
            diff2 += (Integer.parseInt(time[j]) - Integer.parseInt(START_END[0][j])) * (j == 0 ? 365 : j == 1 ? 30 : 1);
        }
        
        
        return diff1 >= 0 && diff2 >= 0;
    }
    
    public static String[] proTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            System.out.println("时间为空");
            return null;
        }
        if (!timestamp.matches("\\d{4}/\\d{2}/\\d{2}")) {
            System.out.println("时间格式错误");
            return null;
        }
        return timestamp.split("/");
    }
    
    public static String leader(String leaderType) {
        return switch (leaderType) {
            case "E" -> "妖精";
            case "R" -> "皇家";
            case "W" -> "法师";
            case "D" -> "龙族";
            case "B" -> "主教";
            case "Nmr" -> "梦魇";
            case "Nm" -> "超越";
            default -> "";
        };
    }
    
    public static boolean hasItem(String[] arr, String item) {
        for (String s : arr) {
            if (s.equals(item)) {
                return true;
            }
        }
        return false;
    }
}