package com.shadowverse;

import com.shadowverse.utils.API;
import com.shadowverse.utils.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String[] MORE = {"5", "10"};
    private static final String[] MPS = {"B", "A", "AA", "Master","GrandMaster"};
    private static final String[] GROUPS = {"topaz","ruby", "sapphire", "diamond"};
    //依次是:妖精,皇家,法师,龙族,主教,梦魇,超越
    private static final String[] LEADER_TYPES = {"E", "R", "W", "D", "B", "Nmr", "Nm"};
    private static final API api = new API("https://shadowverse-wins.com");
    
    private String group;
    private String mp;
    private String more;
    private String leaderType;
    private String seasonId;
    private final String[][] startEnd = {{null, null, null}, {null, null, null}};
    
    private int total;
    private int totalDiamond;
    
    private final Map<String, Integer> result = new HashMap<>();
    private final Map<String, Double> resultPercent = new HashMap<>();
    private final Map<String, Integer> resultDiamond = new HashMap<>();
    private final Map<String, Double> resultDiamondPercent = new HashMap<>();
    
    private boolean isSave = false;
    
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        Main main = new Main();
        main.initMaps();
        main.getQueryParams();
        main.execute();
        main.reportResult();
    }
    
    private void initMaps(){
        result.put("妖精", 0);
        result.put("皇家", 0);
        result.put("法师", 0);
        result.put("龙族", 0);
        result.put("主教", 0);
        result.put("梦魇", 0);
        result.put("超越", 0);
        resultPercent.put("妖精", 0.0);
        resultPercent.put("皇家", 0.0);
        resultPercent.put("法师", 0.0);
        resultPercent.put("龙族", 0.0);
        resultPercent.put("主教", 0.0);
        resultPercent.put("梦魇", 0.0);
        resultPercent.put("超越", 0.0);
        resultDiamond.put("妖精", 0);
        resultDiamond.put("皇家", 0);
        resultDiamond.put("法师", 0);
        resultDiamond.put("龙族", 0);
        resultDiamond.put("主教", 0);
        resultDiamond.put("梦魇", 0);
        resultDiamond.put("超越", 0);
        resultDiamondPercent.put("妖精", 0.0);
        resultDiamondPercent.put("皇家", 0.0);
        resultDiamondPercent.put("法师", 0.0);
        resultDiamondPercent.put("龙族", 0.0);
        resultDiamondPercent.put("主教", 0.0);
        resultDiamondPercent.put("梦魇", 0.0);
        resultDiamondPercent.put("超越", 0.0);
    }
    
    private void getQueryParams(){
        while (true) {
            System.out.println("请输入起始时间,格式为yyyy/MM/dd");
            startEnd[0] = proTime(scanner.nextLine());
            System.out.println("请输入结束时间,格式为yyyy/MM/dd");
            startEnd[1] = proTime(scanner.nextLine());
            
            if(startEnd[0] == null || startEnd[1] == null){
                System.out.println("输入的时间格式不正确,请重新输入");
                continue;
            }
            
            int diff = 0;
            for (int i = 0; i < 3; i++) {
                diff += (Integer.parseInt(startEnd[1][i]) - Integer.parseInt(startEnd[0][i])) * (i == 0 ? 365 : i == 1 ? 30 : 1);
            }
            if (diff < 0) {
                System.out.println("结束时间不能早于起始时间");
                continue;
            }
            System.out.println("查询时间范围:" + startEnd[0][0] + "年" + startEnd[0][1] + "月" + startEnd[0][2] + "日至" + startEnd[1][0] + "年" + startEnd[1][1] + "月" + startEnd[1][2] + "日");
            break;
        }
        while (true) {
            System.out.println("请输入查询段位(B,A,AA,Master,GrandMaster.对于任何段位,都会同时查询其之后的段位(例如输入A会同时查询AA与Master段的数据,这与程序设计者无关,是网站本身的设计),不输入默认查询Master)");
            mp = scanner.nextLine();
            mp = mp.isEmpty() ? "Master" : mp;
            System.out.println("请输入查询分组(topaz(黄宝石),ruby(红宝石),sapphire(蓝宝石),diamond(钻石).对于任何分组,都会同时查询其之后的分组(例如输入ruby会同时查询sapphire与diamond组的数据,这与程序设计者无关,是网站本身的设计),不输入默认同时查询钻石组)");
            group = scanner.nextLine();
            group = group.isEmpty() ? "diamond" : group;
            System.out.println("请输入查询连胜数(5,10.对于任何连胜数,后面忘了总之不是我的锅,不输入默认查询10连胜以上)");
            more = scanner.nextLine();
            more = more.isEmpty() ? "10" : more;
            System.out.println("请输入查询卡包ID,传说揭幕包为61,无限进化为62,以此类推,不输入就画个圈圈诅咒你");
            seasonId = scanner.nextLine();
            if (!hasItem(MPS, mp) || !hasItem(GROUPS, group) || !hasItem(MORE, more) || seasonId.isEmpty()) {
                System.out.println("输入的段位/分组/连胜数不存在或未指定卡包ID,请重新输入");
                continue;
            }
            System.out.println("确认查询条件:" + mp + "段|" + group + "组|" + "连胜数:" + more + "|卡包ID:" + seasonId);
            break;
        }
        System.out.println("是否保存卡组图片到本地?(留空则不保存,输入任意字符则保存,请提前设置环境变量,代理端口设为7897)");
        isSave = !scanner.nextLine().isEmpty();
        if(isSave){
            api.setProxy();
        }
    }
    
    private void execute(){
        ExecutorService executor = Executors.newFixedThreadPool(16);
        
        for (String leaderType : LEADER_TYPES) {
            executor.submit(() -> {
                System.out.println("爬取" + leader(leaderType) + " " + mp + " " + group + "组" + "开始");
                try {
                    get(leaderType, estimatePage(leaderType));
                    if (!"diamond".equals(group) || !"GrandMaster".equals(mp)) {
                        System.out.println("准备查询钻石组数据");
                        get("diamond", estimatePage("diamond"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("爬取" + leader(leaderType) + " " + mp + " " + group + "组" + "结束");
            });
        }
        
        
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void reportResult(){
        System.out.println(startEnd[0][0] + "年" + startEnd[0][1] + "月" + startEnd[0][2] + "日至" + startEnd[1][0] + "年" + startEnd[1][1] + "月" + startEnd[1][2] + "日");
        
        if (!group.equals("diamond") || "GrandMaster".equals(mp)){
            for (String key : result.keySet()) {
                total += result.get(key);
            }
            System.out.println("总计:" + total + "条数据");
            for (String key : result.keySet()) {
                System.out.println(key + ":" + result.get(key));
            }
            for (String key : result.keySet()) {
                resultPercent.put(key, result.get(key).doubleValue() / total);
            }
            for (String key : resultPercent.keySet()) {
                System.out.println(key + ":" + resultPercent.get(key));
            }
        }
        if(!"GrandMaster".equals(mp)) {
            for (String key : resultDiamond.keySet()) {
                totalDiamond += resultDiamond.get(key);
            }
            System.out.println("钻石组数据,共" + totalDiamond + "条");
            for (String key : resultDiamond.keySet()) {
                System.out.println(key + ":" + resultDiamond.get(key));
            }
            for (String key : resultDiamond.keySet()) {
                resultDiamondPercent.put(key, resultDiamond.get(key).doubleValue() / totalDiamond);
            }
            for (String key : resultDiamondPercent.keySet()) {
                System.out.println(key + ":" + resultDiamondPercent.get(key));
            }
        }
        System.out.println("输入回车退出程序");
        scanner.nextLine();
        scanner.close();
    }
    
    public void get(String leader, Integer start) {
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
                
                if(isSave){
                    Element img = section.select(".image").first()
                            .select("img").last();
                    String url = img.attr("src");
                    String output = System.getenv("SV_OUTPUT_PATH");
                    boolean success = api.downloadImage(url, output);
                    if(success){
                        System.out.println("图片保存成功");
                    }else{
                        System.out.println("图片保存失败");
                    }
                }
                
                if (group.equals("diamond") && !"GrandMaster".equals(mp)) {
                    resultDiamond.put(leader(leader), resultDiamond.get(leader(leader)) + 1);
                    //若是钻石组,只统计钻石组结果即可
                    continue;
                }
                result.put(leader(leader), result.get(leader(leader)) + 1);
                
            }
            if (isEnd) {
                System.out.println("爬取至末尾页");
                break;
            }
        }
        
    }
    
    public Integer estimatePage(String leader) {
        System.out.println("正在估计起始页数...");
        int start = 0;
        int end = 0;
        Integer diff = calculateDelTime(1,leader, 1);
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
            
            if ((isPageLegal(leader, i) || diff == 0) && (!isPageLegal( leader, i - 1) || i == 1)) {
                start = i;
                System.out.println("起始页为:" + start);
            }
            
            if (
                    !isPageLegal(leader, i) &&
                            calculateDelTime(i,  leader, 1) != null &&
                            calculateDelTime(i, leader, 1) > 0 &&
                            (calculateDelTime(i + 1,leader, 1) == null
                                    || (
                                    calculateDelTime(i + 1, leader, 1) != null
                                            && calculateDelTime(i + 1, leader, 1) < 0
                                            && !isPageLegal(leader, i + 1)
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
                Integer cal = calculateDelTime(i, leader, 1);
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
                Integer cal = calculateDelTime(i, leader, 1);
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
    
    public Integer calculateDelTime(int page, String leader,int mode) {
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
                diff += (Integer.parseInt(time[j]) - Integer.parseInt(startEnd[mode][j])) * (j == 0 ? 365 : j == 1 ? 30 : 1);
            }
            
            return diff;
        }
        return null;
    }
    
    public boolean isPageLegal(String leader, int page) {
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
    
    public boolean timeLegal(String[] time) {
        int diff1 = 0;
        for (int j = 0; j < 3; j++) {
            diff1 += (Integer.parseInt(startEnd[1][j]) - Integer.parseInt(time[j])) * (j == 0 ? 365 : j == 1 ? 30 : 1);
        }
        int diff2 = 0;
        for (int j = 0; j < 3; j++) {
            diff2 += (Integer.parseInt(time[j]) - Integer.parseInt(startEnd[0][j])) * (j == 0 ? 365 : j == 1 ? 30 : 1);
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