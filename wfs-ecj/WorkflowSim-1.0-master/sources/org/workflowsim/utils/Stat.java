package org.workflowsim.utils;

import javafx.util.Pair;

import java.util.Map;

public class Stat {
    private String name;
    private long time;
    private double mean;
    private double sd;
    private Map<String, Pair<Integer, Integer>> worflowStats;
    private String bestRule;
    private String fitness;

    public Stat(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBestRule() {
        return bestRule;
    }

    public void setBestRule(String bestRule) {
        this.bestRule = bestRule;
    }

    public String getFitness() {
        return fitness;
    }

    public void setFitness(String fitness) {
        this.fitness = fitness;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getSd() {
        return sd;
    }

    public void setSd(double sd) {
        this.sd = sd;
    }

    public Map<String, Pair<Integer, Integer>> getWorflowStats() {
        return worflowStats;
    }

    public void setWorflowStats(Map<String, Pair<Integer, Integer>> worflowStats) {
        this.worflowStats = worflowStats;
    }
}
