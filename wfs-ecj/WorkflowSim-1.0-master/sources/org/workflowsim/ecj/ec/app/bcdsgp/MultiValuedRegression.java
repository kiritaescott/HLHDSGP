/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.app.bcdsgp;
import ec.util.*;
import ec.*;
import ec.gp.*;
import ec.simple.*;
import javafx.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.Job;
import org.workflowsim.examples.scheduling.BCDSGPSchedulingAlgorithmExample;

import java.io.File;
import java.util.*;

public class MultiValuedRegression extends GPProblem implements SimpleProblemForm {
    private static final long serialVersionUID = 1;

    public double currentX;
    public double currentY;
    public double taskSize;
    public double executionTime;
    public double vmSpeed;
    public double waitingTime;
    public double relativeFinishTime;
    public double unitCost;
    public double expectedCompletionTime;
    public double MINMIN;
    public double MAXMIN;
    public double FCFS;

    private static final int TRAINING_SET = 0;
    private static final int TESTING_SET = 1;


    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);

        // verify our input is the right class (or subclasses from it)
        if (!(input instanceof DoubleData))
            state.output.fatal("GPData class must subclass from " + DoubleData.class, base.push(P_DATA), null);
    }

    public void evaluate(final EvolutionState state,
                         final Individual ind,
                         final int subpopulation,
                         final int threadnum)
    {
        if (!ind.evaluated) // don't bother reevaluating
        {
            DoubleData input = (DoubleData) (this.input);
            BCDSGPUtility utility = new BCDSGPUtility();

            utility.setState(state);
            utility.setInd(ind);
            utility.setSubpopulation(subpopulation);
            utility.setThreadnum(threadnum);
            utility.setInput(input);
            utility.setStack(stack);
            utility.setProblem(this);

            try {

                Map <String, Double> workflowStats = new HashMap<>();

                File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
                File[] vmFiles = utility.getVMFileList();

                ArrayList<Double> wfResponseTimes = new ArrayList<>();
                ArrayList<Double> taskResponseTimes = new ArrayList<>();
                ArrayList<Double> wfCosts = new ArrayList<>();

                double totalResponseTime = 0;
                double totalJobs = 0;
                double totalCost = 0;

                GPIndividual gpInd = (GPIndividual) ind;

                for (File tf : taskFiles) {// test cases
                    String filePath = tf.getPath();
                    utility.setDaxPath(filePath);

                    for (File vmf : vmFiles) {
                        utility.setVmDaxPath(vmf.getPath());
                        //utility.setVmRatiosList();

                        List<Job> jobs = new ArrayList<>();

                        jobs = BCDSGPSchedulingAlgorithmExample.runSimulation(utility);

                        double wfResponseTime = 0;
                        double wfCost = 0;

                        // get the response time of the workflow
                        // calculate the average
                        for (Job j : jobs){
                            Cloudlet c = (Cloudlet) j;
                            double taskResp = c.getFinishTime() - c.getArrivalTime();
                            double processingCost = j.getProcessingCost();
                            System.out.println("resp time of task: "+taskResp+" cost of task: "+processingCost);
                            wfResponseTime += taskResp;
                            wfCost += processingCost;
                            taskResponseTimes.add(taskResp);
                        }
                        System.out.println("wfResponseTime: "+ wfResponseTime);
                        System.out.println("wfCost: "+ wfCost);

                        double mean = utility.getAverageResponseTime(taskResponseTimes);
                        double avgWfResponseTime =  wfResponseTime / jobs.size();
                        System.out.println("Average wfResponseTime: "+ avgWfResponseTime+"Mean wfResponseTime: "+ mean);

                        double sd = utility.getStandardDeviation(taskResponseTimes);

                        System.out.println("Standard Deviation: "+sd);

                        totalResponseTime += wfResponseTime;
                        totalJobs += jobs.size();

                        totalCost += wfCost;

                        wfResponseTimes.add(wfResponseTime);
                        wfCosts.add(wfCost);

                        workflowStats.put(tf.getName() + "_" + vmf.getName(), avgWfResponseTime);
                    }
                }

//                double avgRespTime = totalResponseTime / workflowStats.size();
//
//                System.out.println("avgRespTime: "+ avgRespTime);

                System.out.println("totalCost: "+ totalCost);

                double mean = utility.getAverageResponseTime(wfResponseTimes);

                System.out.println("Number of scenarios : "+wfResponseTimes.size());
                System.out.println("Mean of overall makespans : "+mean);

                double sd = utility.getStandardDeviation(wfResponseTimes);

                System.out.println("Standard Deviation: "+sd);

                /**
                 * set the average average total time for the GP individual
                 * */
                gpInd.setTotalTime(mean);
                //gpInd.setWorkflowResps(workflowStats);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //    @SuppressWarnings("Duplicates")
    public void describe(EvolutionState state, Individual ind, int subpopulation, int threadnum, int log) {
        DoubleData input = (DoubleData) (this.input);
        // print out rules
        GPIndividual gpInd = (GPIndividual) ind;
        GPTree gpt = gpInd.trees[0];
        GPNode child = gpt.child;

        String rule = child.makeCTree(true, true, true);

        // perform testing here
        BCDSGPUtility utility = new BCDSGPUtility();

        utility.setState(state);
        utility.setInd(ind);
        utility.setSubpopulation(subpopulation);
        utility.setThreadnum(threadnum);
        utility.setInput(input);
        utility.setStack(stack);
        utility.setProblem(this);


        try {


            Map <String, Double> workflowStats = new HashMap<>();

            File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
            File[] vmFiles = utility.getVMFileList();

            ArrayList<Double> wfResponseTimes = new ArrayList<>();
            ArrayList<Double> wfCosts = new ArrayList<>();

            double totalResponseTime = 0;
            double totalJobs = 0;
            double totalCost = 0;

            for (File tf : taskFiles) {// test cases
                String filePath = tf.getPath();
                utility.setDaxPath(filePath);

                for (File vmf : vmFiles) {
                    utility.setVmDaxPath(vmf.getPath());

                    List<Job> jobs = new ArrayList<>();

                    jobs = BCDSGPSchedulingAlgorithmExample.runSimulation(utility);

                    double wfResponseTime = 0;
                    double wfCost = 0;

                    // get the response time of the workflow
                    // calculate the average
                    for (Job j : jobs){
                        Cloudlet c = (Cloudlet) j;
                        double taskResp = c.getFinishTime() - c.getArrivalTime();
                        double processingCost = j.getProcessingCost();
                        System.out.println("resp time of task: "+taskResp+" cost of task: "+processingCost);
                        wfResponseTime += taskResp;
                        wfCost += processingCost;
                    }
                    System.out.println("wfResponseTime: "+ wfResponseTime);
                    System.out.println("wfCost: "+ wfCost);

                    double avgWfResponseTime = wfResponseTime/jobs.size();

                    System.out.println("Average wfResponseTime: "+ avgWfResponseTime);

                    totalResponseTime += wfResponseTime;
                    totalJobs += jobs.size();

                    totalCost += wfCost;

                    wfResponseTimes.add(wfResponseTime);
                    wfCosts.add(wfCost);

                    workflowStats.put(tf.getName() + "_" + vmf.getName(), avgWfResponseTime);
                }
            }


            double avgRespTime = totalResponseTime / workflowStats.size();

            state.output.println("avgRespTime: "+ avgRespTime, log);
            System.out.println("avgRespTime: "+ avgRespTime);

            double avgCost = totalCost / workflowStats.size();

            state.output.println("avgCost: "+ avgCost, log);
            System.out.println("avgCost: "+ avgCost);

            double mean = utility.getAverageResponseTime(wfResponseTimes);

            state.output.println("Number of scenarios : "+wfResponseTimes.size(), log);
            System.out.println("Number of scenarios : "+wfResponseTimes.size());
            state.output.println("Mean of overall makespans : "+mean, log);
            System.out.println("Mean of overall makespans : "+mean);

            double sd = utility.getStandardDeviation(wfResponseTimes);

            state.output.println("Standard Deviation: "+sd, log);
            System.out.println("Standard Deviation: "+sd);

            state.output.println("The best rule is: " + rule, log);
            state.output.println("The fitness of the best gpInd is: "+gpInd.fitness.fitnessToStringForHumans(), log);

            List sortedKeys=new ArrayList(workflowStats.keySet());
            Collections.sort(sortedKeys);
            Collections.reverse(sortedKeys);

            for (Object objKey : sortedKeys) {
                String key = (String) objKey;
                Double value = workflowStats.get(key);

                System.out.println("The average responsetime for for : " + key + " is: "+ value);
                state.output.println("The average responsetime for for : " + key + " is: "+ value, log);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

