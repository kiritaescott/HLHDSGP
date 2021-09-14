package org.workflowsim.utils;

import javafx.util.Pair;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ReadGPHHDatafiles {

    private static final int TOTAL_SCENARIOS = 36;
    private static List <Stat> stats;
    private static List<Double> allMeans;
    private static List<Double> allSDs;
    private static List<String> allRules;
    private static List<Map<String, Pair<Integer, Integer>>> allWorkflowStats;


    private static double overallMean;
    private static double overallSD;

    private static final String SIPHT = "Sipht";
    private static final String MONTAGE = "Montage";
    private static final String CYBER = "CyberShake";
    private static final String INSPIRAL = "Inspiral";

    private static final int XSML = 25;
    private static final int SML = 30;
    private static final int MED = 50;
    private static final int XMED = 60;
    private static final int LGE = 100;
    private static final int XLGE = 1000;

    private static final int V_SML = 4;
    private static final int V_MED = 16;
    private static final int V_LGE = 32;

    private static Map <String, List<Double>> workflowStats;

    private static Map <String, Double> calculatedWorkflowStats;

    private static Map <String, Integer> terminalFrequency;
    private static Map <String, Integer> functionFrequency;

    private static List <Stat> resultStats;
    private static Map <String, Map<String, Double>> resultStatsMap;
    private static Map <String, Map<String, Double>> resultAvgsStatsMap;


    public static void main(String [] args){
        //read in stats information
        //readInStats();
        //setup all the collections of stat information
        //setupStats();
        //calculate and print out all the stats information
        //calculateStats();
        //printOutStats();

        //perform analysis on rules
        //performRulesAnalysis();
        //print out anaylsis of rules
        //printOutRulesAnalysis();

        //read in result stats
        readInResultStats();
//        setupResultsStats();
//        calculateResultsStats();
        setupResultAverageStatsMap();
        calculateResultsAverages();
    }

    private static void calculateResultsStats(){
        for (Map.Entry entry : resultStatsMap.entrySet()){
            System.out.println("-----------------------------");
            System.out.println(entry.getKey());
            System.out.println("-----------------------------");
            Map<String, Double> map = (Map<String, Double>) entry.getValue();

            Map<String, Double> result = map.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            int count = 1;

            for (Map.Entry m : result.entrySet()){
                System.out.println(count+" "+m.getKey()+" "+m.getValue());
                count++;
            }
        }

    }



    private static void calculateResultsAverages(){
        for (Map.Entry entry : resultAvgsStatsMap.entrySet()){
            System.out.println("-----------------------------");
            System.out.println(entry.getKey());
            System.out.println("-----------------------------");
            Map<String, Double> map = (Map<String, Double>) entry.getValue();

            Map<String, Double> result = map.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            double smallAvg=0;
            double medAvg=0;
            double lgeAvg=0;

            double small_4=0;
            double small_16=0;
            double small_32=0;

            double medium_4=0;
            double medium_16=0;
            double medium_32=0;

            double large_4=0;
            double large_16=0;
            double large_32=0;


            for (Map.Entry m : result.entrySet()){
                //System.out.println(" "+m.getKey()+" "+m.getValue());
                String file = (String)m.getKey();
                double mean = (double) m.getValue();

                if ((file.contains("25") || file.contains("30")) && file.contains("vm_4")){
                    small_4 += mean;
                } else if ((file.contains("25") || file.contains("30")) && file.contains("vm_16")){
                    small_16 += mean;
                } else if ((file.contains("25") || file.contains("30")) && file.contains("vm_32")){
                    small_32 += mean;
                } else if ((file.contains("50") || file.contains("60")) && file.contains("vm_4")){
                    medium_4 += mean;
                } else if ((file.contains("50") || file.contains("60")) && file.contains("vm_16")){
                    medium_16 += mean;
                } else if ((file.contains("50") || file.contains("60")) && file.contains("vm_32")){
                    medium_32 += mean;
                } else if (file.contains("100.") && file.contains("vm_4")){
                    large_4 += mean;
                } else if (file.contains("100.") && file.contains("vm_16")){
                    large_16 += mean;
                } else if (file.contains("100.")&& file.contains("vm_32")){
                    large_32 += mean;
                }


            }

            small_4 /= 4;
            small_16 /= 4;
            small_32 /= 4;

            medium_4 /= 4;
            medium_16 /= 4;
            medium_32 /= 4;

            large_4 /= 4;
            large_16 /= 4;
            large_32 /= 4;

            System.out.printf("Small Tasks Averages - 4: %.4f 16: %.4f 32: %.4f\n",small_4, small_16, small_32);
            System.out.printf("Medium Tasks Averages - 4: %.4f 16: %.4f 32: %.4f\n",medium_4, medium_16, medium_32);
            System.out.printf("Large Tasks Averages - 4: %.4f 16: %.4f 32: %.4f\n",+large_4, large_16, large_32);
        }
    }

    private static void setupResultAverageStatsMap(){
        resultAvgsStatsMap = new HashMap<>();

        for (Stat stat : resultStats){
            String alg = stat.getName();
            // pair is mean and sd
            Map<String, Pair<Integer, Integer>> statStats = stat.getWorflowStats();

            for (Map.Entry entry : statStats.entrySet()){
                String file = (String) entry.getKey();
                Pair pair = (Pair) entry.getValue();
                double mean = (Double) pair.getKey();

                //System.out.println("alg: "+alg+" file: "+file+" mean: "+pair.getKey());

                if (resultAvgsStatsMap.containsKey(alg)){
                    resultAvgsStatsMap.get(alg).put(file, mean);
                } else {
                    Map <String, Double> map = new HashMap<>();
                    map.put(file, mean);
                    resultAvgsStatsMap.put(alg, map);
                }
            }

        }
    }


    private static void setupResultsStats() {
        resultStatsMap = new HashMap<>();

        for (Stat stat : resultStats){
            String alg = stat.getName();
            // pair is mean and sd
            Map<String, Pair<Integer, Integer>> statStats = stat.getWorflowStats();

            for (Map.Entry entry : statStats.entrySet()){
                String file = (String) entry.getKey();
                Pair pair = (Pair) entry.getValue();
                double mean = (Double) pair.getKey();

                //System.out.println("alg: "+alg+" file: "+file+" mean: "+pair.getKey());

                if (resultStatsMap.containsKey(file)){
                    resultStatsMap.get(file).put(alg, mean);
                } else {
                    Map <String, Double> map = new HashMap<>();
                    map.put(alg, mean);
                    resultStatsMap.put(file, map);
                }
            }

        }

    }

    public static boolean isOperator(String s){
        if (s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/")
            || s.equals("MAX") || s.equals("MIN")){
            return true;
        }
        return false;
    }

    public static String removeBraces(String s){
        s = s.replace("(", "");
        s = s.replace(")","");

        return s;
    }

    public static void performRulesAnalysis(){
        System.out.println("There are "+allRules.size()+" rules.");
        for (String rule : allRules){
            System.out.println(rule);
//            String i = rule.replace("(", "");
//            String j = i.replace(")","");
//            System.out.println(j);

            String [] combos = rule.split(" ");

            for (String s : combos){
                s = removeBraces(s);
                //System.out.println("s: "+s);

                if (!isOperator(s)){
                    if (terminalFrequency.containsKey(s)){
                        terminalFrequency.put(s, terminalFrequency.get(s)+1);
                    } else {
                        terminalFrequency.put(s, 1);
                    }
                } else {
                    if (functionFrequency.containsKey(s)){
                        functionFrequency.put(s, functionFrequency.get(s)+1);
                    } else {
                        functionFrequency.put(s, 1);
                    }
                }

            }
        }
    }

    public static void printOutRulesAnalysis(){
        for (Map.Entry termMap : terminalFrequency.entrySet()){
            String terminal = (String) termMap.getKey();
            int count = (Integer) termMap.getValue();
            System.out.println(terminal+" : "+count);
        }

        for (Map.Entry termMap : functionFrequency.entrySet()){
            String function = (String) termMap.getKey();
            int count = (Integer) termMap.getValue();
            System.out.println(function+" : "+count);
        }

    }

    public static double getMean(List<Double> means){
        double mean = 0;
        double sum = 0;

        int length = means.size();

        for (double num : means){
            sum += num;
        }

        mean = sum / length;

        return mean;
    }


    public static double getStandardDeviation(List<Double> means){
        double mean = getMean(means);
        double standardDeviation = 0;

        int length = means.size();

        for (double num : means){
            standardDeviation += Math.pow(num - mean, 2);
        }

        double sd = Math.sqrt(standardDeviation/length);

        return sd;
    }

    public static void printOutStats(){
        System.out.printf("overallMean: %.4f\n", overallMean);
        System.out.printf("overallSD: %.4f\n", overallSD);

        Map<String, Double> result = calculatedWorkflowStats.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        List <Double> means = new ArrayList<>();

        for (Map.Entry entry : result.entrySet()){
            String fName = (String) entry.getKey();
            Double mean = (Double) entry.getValue();
            means.add(mean);
            System.out.printf("%s : %.4f\n", fName, mean);
        }

        double overallMean = getMean(means);
        double sd = getStandardDeviation(means);

        System.out.printf("overallMean: %.4f\n", overallMean);
        System.out.printf("overallSD: %.4f\n", sd);
    }

    public static void calculateStats(){
        // calc overall mean
        overallMean = getMean(allMeans);
        overallSD = getMean(allSDs);

        // calc overall mean for individual workflow stats
        for (Map.Entry entry : workflowStats.entrySet()){
            String key = (String) entry.getKey();
            List <Double> vals = (List<Double>)entry.getValue();
            double mean = getMean(vals);
            calculatedWorkflowStats.put(key, mean);
        }
    }

    public static void setupStats(){
        allMeans = new ArrayList<>();
        allSDs = new ArrayList<>();
        allRules = new ArrayList<>();
        allWorkflowStats = new ArrayList<>();
        workflowStats = new HashMap<>();
        calculatedWorkflowStats = new HashMap<>();
        terminalFrequency = new HashMap<>();
        functionFrequency = new HashMap<>();

        stats.sort((Stat s1, Stat s2) -> Double.compare(s1.getMean(), s2.getMean()));

        for (int i=0; i < 5; i++){
            Stat stat = stats.get(i);
            allMeans.add(stat.getMean());
            allSDs.add(stat.getSd());
            allRules.add(stat.getBestRule());
            allWorkflowStats.add(stat.getWorflowStats());
        }

        for (Map<String, Pair<Integer, Integer>> map : allWorkflowStats){
            for (Map.Entry entry : map.entrySet()){
                String key = (String) entry.getKey();
                Pair val = (Pair) entry.getValue();
                double mean = (Double)val.getKey();
                if (workflowStats.containsKey(key)){
                    workflowStats.get(key).add(mean);
                } else {
                    List <Double> vals = new ArrayList<>();
                    vals.add(mean);
                    workflowStats.put(key, vals);
                }
            }
        }

    }

    public static void readInStats(){
        stats = new ArrayList<>();
        String path = "/Users/koe/Desktop/gphh/dsgp-datafiles";
        File fd = new File(path);
        File[] dataFiles = fd.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".stat");
            }
        });

        for (File data : dataFiles){
            Stat stat = new Stat(data.getName());
            try {

                Scanner sc = new Scanner(data);

                while (sc.hasNext()){
                    long time = Long.parseLong(sc.next());
                    stat.setTime(time);
                    double mean = sc.nextDouble();
                    stat.setMean(mean);
                    double sd = sc.nextDouble();
                    stat.setSd(sd);

                    int count = 0;

                    Map<String, Pair<Integer, Integer>> wfStats = new HashMap<>();

                    while (count < TOTAL_SCENARIOS){
                        String wfFileName = sc.next();

                        double wfMean = sc.nextDouble();
                        double wfSd = sc.nextDouble();

                        //ignore big files
                        if (!wfFileName.contains("1000")) {
                            wfStats.put(wfFileName, new Pair(wfMean, wfSd));
                        }
                        count ++;
                    }

                    stat.setWorflowStats(wfStats);

                    sc.nextLine(); //phantom line in the files

                    String bestRule = sc.nextLine();
                    stat.setBestRule(bestRule);

                    String fitness = sc.nextLine();
                    stat.setFitness(fitness);

                    stats.add(stat);
                }

            } catch (IOException e){
                System.out.println("Problem reading data file.");
            }
        }
    }

    public static void readInResultStats(){
        resultStats = new ArrayList<>();
        String path = "/Users/koe/Desktop/gphh/competing-algorithms-results";
        File fd = new File(path);
        File[] dataFiles = fd.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".stat");
            }
        });

        for (File data : dataFiles){
            Stat stat = new Stat(data.getName());
            try {

                Scanner sc = new Scanner(data);

                while (sc.hasNext()){
                    long time = Long.parseLong(sc.next());
                    stat.setTime(time);
                    double mean = sc.nextDouble();
                    stat.setMean(mean);
                    double sd = sc.nextDouble();
                    stat.setSd(sd);

                    int count = 0;

                    Map<String, Pair<Integer, Integer>> wfStats = new HashMap<>();

                    while (count < TOTAL_SCENARIOS){
                        String wfFileName = sc.next();
                        double wfMean = sc.nextDouble();
                        double wfSd = sc.nextDouble();

                        wfStats.put(wfFileName, new Pair(wfMean, wfSd));

                        count ++;
                    }

                    stat.setWorflowStats(wfStats);

                    resultStats.add(stat);
                }



            } catch (IOException e){
                System.out.println("Problem reading data file.");
            }

        }

        System.out.println("size of result stats: "+resultStats.size());
    }


}
