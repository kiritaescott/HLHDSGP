/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.app.wfsgp;
import ec.util.*;
import ec.*;
import ec.gp.*;
import ec.simple.*;
import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.Job;
import org.workflowsim.examples.scheduling.GPSchedulingAlgorithmExample;

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
    public double expectedCompletionTime;

    private static final int TRAINING_SET = 0;
    private static final int TESTING_SET = 1;

    private static Map<String, String> workflowStats;



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
            GPUtility utility = new GPUtility();

            utility.setState(state);
            utility.setInd(ind);
            utility.setSubpopulation(subpopulation);
            utility.setThreadnum(threadnum);
            utility.setInput(input);
            utility.setStack(stack);
            utility.setProblem(this);

            try {

                Map <String, Double> workflowMkspns = new HashMap<>();

                File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
                File[] vmFiles = utility.getVMFileList();

                ArrayList<Double> overallMakeSpans = new ArrayList<>();

                ArrayList<Double> wfResponseTimes = new ArrayList<>();
                ArrayList<Double> allRespTasks = new ArrayList<>();

                workflowStats = new HashMap<>();

                double overallMakespans = 0;
                double overallJobs = 0;

                GPIndividual gpInd = (GPIndividual) ind;

                for (File tf : taskFiles) {// test cases
                    String filePath = tf.getPath();
                    utility.setDaxPath(filePath);

                    for (File vmf : vmFiles) {
                        utility.setVmDaxPath(vmf.getPath());
                        utility.setVmRatiosList();

                        double vmMkspn = 0;
                        double vmJobs = 0;

                        double wfResponseTime = 0;
                        List <Job> jobs = new ArrayList<>();
                        ArrayList<Double> respTimes = new ArrayList<>();

                        jobs = GPSchedulingAlgorithmExample.runSimulation(utility);

                        // calculate the average
                        System.out.println("Jobs returned: "+jobs.size());

                        Collections.sort(jobs, new Comparator<Job>() {
                            @Override
                            public int compare(Job v1, Job v2) {
                                return Double.compare(v1.getCloudletId(), v2.getCloudletId());
                            }
                        });

                        for (Job j : jobs){
                            Cloudlet c = (Cloudlet) j;
                            double taskResp = c.getFinishTime() - c.getAllocationTime();
                            //System.out.println(c.getCloudletId()+" resp time of task: "+taskResp);
                            respTimes.add(taskResp);
                            allRespTasks.add(taskResp);
                            wfResponseTime += taskResp;
                        }
                        //System.out.println(tf.getName() + "_" + vmf.getName()+" total wfResponseTime: "+ wfResponseTime);

                        double mean = utility.getAverageMakespan(respTimes);
                        //System.out.println(tf.getName() + "_" + vmf.getName()+" average wfResponseTime: "+ mean);

                        double sd = utility.getStandardDeviation(respTimes);
                        //System.out.println("Standard Deviation of : "+tf.getName() + "_" + vmf.getName()+" is "+sd);

                        wfResponseTimes.add(mean);

                        workflowStats.put(tf.getName() + "_" + vmf.getName(), " "+mean+" "+sd);
                    }
                }


                double mean = utility.getAverageMakespan(wfResponseTimes);
                System.out.println("Number of scenarios : "+wfResponseTimes.size());
                System.out.println("Mean of overall respsonse times : "+mean);
                double sd = utility.getStandardDeviation(wfResponseTimes);
                System.out.println("Standard Deviation: "+sd);

                List sortedWFKeys=new ArrayList(workflowStats.keySet());
                Collections.sort(sortedWFKeys);

                for (Object objKey : sortedWFKeys) {
                    String key = (String) objKey;
                    String value = workflowStats.get(key);
                    System.out.println("For file : " + key + " "+ value);
                }

                /**
                 * set the average average total time for the GP individual
                 * */
                gpInd.setTotalTime(mean);
                gpInd.setWorkflowResps(workflowStats);

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

        ArrayList<Double> wfResponseTimes = new ArrayList<>();
        ArrayList<Double> allRespTasks = new ArrayList<>();

        workflowStats = new HashMap<>();


        // perform testing here
        GPUtility utility = new GPUtility();

        utility.setState(state);
        utility.setInd(ind);
        utility.setSubpopulation(subpopulation);
        utility.setThreadnum(threadnum);
        utility.setInput(input);
        utility.setStack(stack);
        utility.setProblem(this);

        List sortedKeys=new ArrayList(gpInd.getWorkflowMkspns().keySet());
        Collections.sort(sortedKeys);


        try {


            File[] taskFiles = utility.getTaskFileList(TESTING_SET); // testing set
            File[] vmFiles = utility.getVMFileList();

            ArrayList<Double> overallMakeSpans = new ArrayList<>();

            double overallMakespans = 0;
            double overallJobs = 0;

            ArrayList<Double> makeSpans = new ArrayList<>();

            for (File tf : taskFiles) {// test cases
                String filePath = tf.getPath();
                utility.setDaxPath(filePath);


                ArrayList <Double> tfMkspans = new ArrayList<>();

                for (File vmf : vmFiles){
                    utility.setVmDaxPath(vmf.getPath());
                    utility.setVmRatiosList();

                    double vmMkspn = 0;
                    double vmJobs = 0;

                    double wfResponseTime = 0;
                    List <Job> jobs = new ArrayList<>();
                    ArrayList<Double> respTimes = new ArrayList<>();

                    jobs = GPSchedulingAlgorithmExample.runSimulation(utility);

                    // calculate the average
                    System.out.println("Jobs returned: "+jobs.size());

                    Collections.sort(jobs, new Comparator<Job>() {
                        @Override
                        public int compare(Job v1, Job v2) {
                            return Double.compare(v1.getCloudletId(), v2.getCloudletId());
                        }
                    });

                    for (Job j : jobs){
                        Cloudlet c = (Cloudlet) j;
                        double taskResp = c.getFinishTime() - c.getAllocationTime();
                        //System.out.println(c.getCloudletId()+" resp time of task: "+taskResp);
                        respTimes.add(taskResp);
                        allRespTasks.add(taskResp);
                        wfResponseTime += taskResp;
                    }
                    //System.out.println(tf.getName() + "_" + vmf.getName()+" total wfResponseTime: "+ wfResponseTime);

                    double mean = utility.getAverageMakespan(respTimes);
                    //System.out.println(tf.getName() + "_" + vmf.getName()+" average wfResponseTime: "+ mean);

                    double sd = utility.getStandardDeviation(respTimes);
                    //System.out.println("Standard Deviation of : "+tf.getName() + "_" + vmf.getName()+" is "+sd);

                    wfResponseTimes.add(mean);

                    workflowStats.put(tf.getName() + "_" + vmf.getName(), " "+mean+" "+sd);

                }

            }

            double mean = utility.getAverageMakespan(wfResponseTimes);
            System.out.println("Number of scenarios : "+wfResponseTimes.size());
            System.out.println("Mean of overall respsonse times : "+mean);
            state.output.println("TESTING Number of scenarios : "+wfResponseTimes.size(), log);
            state.output.println("TESTING Mean of overall respsonse times : "+mean, log);
            double sd = utility.getStandardDeviation(wfResponseTimes);
            System.out.println("Standard Deviation: "+sd);
            state.output.println("TESTING Standard Deviation: "+sd, log);

            List sortedWFKeys=new ArrayList(workflowStats.keySet());
            Collections.sort(sortedWFKeys);

            for (Object objKey : sortedWFKeys) {
                String key = (String) objKey;
                String value = workflowStats.get(key);
                System.out.println("For file : " + key + " "+ value);
                state.output.println(" " + key + " "+ value, log);
            }

            state.output.println("The best rule is: " + rule, log);
            state.output.println("The fitness of the best gpInd is: "+gpInd.fitness.fitnessToStringForHumans(), log);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    public void evaluate(final EvolutionState state,
//                         final Individual ind,
//                         final int subpopulation,
//                         final int threadnum)
//    {
//        if (!ind.evaluated) // don't bother reevaluating
//        {
//            DoubleData input = (DoubleData) (this.input);
//            GPUtility utility = new GPUtility();
//
//            utility.setState(state);
//            utility.setInd(ind);
//            utility.setSubpopulation(subpopulation);
//            utility.setThreadnum(threadnum);
//            utility.setInput(input);
//            utility.setStack(stack);
//            utility.setProblem(this);
//
//            try {
//
//                Map <String, Double> workflowMkspns = new HashMap<>();
//
//                File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
//                File[] vmFiles = utility.getVMFileList();
//
//                ArrayList<Double> overallMakeSpans = new ArrayList<>();
//
//                double overallMakespans = 0;
//                double overallJobs = 0;
//
//                GPIndividual gpInd = (GPIndividual) ind;
//
//                for (File tf : taskFiles) {// test cases
//                    String filePath = tf.getPath();
//                    utility.setDaxPath(filePath);
//
//                    for (File vmf : vmFiles) {
//                        utility.setVmDaxPath(vmf.getPath());
//                        utility.setVmRatiosList();
//
//                        double vmMkspn = 0;
//                        double vmJobs = 0;
//
//                        List<Job> jobs = new ArrayList<>();
//
//                        jobs = GPSchedulingAlgorithmExample.runSimulation(utility);
//
//                        vmJobs = jobs.size();
//                        vmMkspn = utility.getCloudletsMakeSpan(jobs);
//
//                        overallJobs += vmJobs;
//                        overallMakespans += vmMkspn;
//
//                        double avgVmMkspn = vmMkspn / vmJobs;
//
//                        overallMakeSpans.add(avgVmMkspn);
//
//                        workflowMkspns.put(tf.getName() + "_" + vmf.getName(), avgVmMkspn);
//                    }
//                }
//
//                System.out.println("overallMkspn: " + overallMakespans);
//
//                double overallAvgMkspan = overallMakespans / overallJobs;
//
//                System.out.println("overallAvgMkspn: " + overallAvgMkspan);
//
//                double mean = utility.getAverageMakespan(overallMakeSpans);
//
//                System.out.println("Number of scenarios : "+overallMakeSpans.size());
//                System.out.println("Mean of overall makespans : "+mean);
//
//                double sd = utility.getStandardDeviation(overallMakeSpans);
//
//                System.out.println("Standard Deviation: "+sd);
//
//                /**
//                 * set the average average total time for the GP individual
//                 * */
//                gpInd.setTotalTime(overallAvgMkspan);
//                gpInd.setWorkflowMkspns(workflowMkspns);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//
//    //    @SuppressWarnings("Duplicates")
//    public void describe(EvolutionState state, Individual ind, int subpopulation, int threadnum, int log) {
//        DoubleData input = (DoubleData) (this.input);
//        // print out rules
//        GPIndividual gpInd = (GPIndividual) ind;
//        GPTree gpt = gpInd.trees[0];
//        GPNode child = gpt.child;
//
//        String rule = child.makeCTree(true, true, true);
//
//        // perform testing here
//        GPUtility utility = new GPUtility();
//
//        utility.setState(state);
//        utility.setInd(ind);
//        utility.setSubpopulation(subpopulation);
//        utility.setThreadnum(threadnum);
//        utility.setInput(input);
//        utility.setStack(stack);
//        utility.setProblem(this);
//
//        List sortedKeys=new ArrayList(gpInd.getWorkflowMkspns().keySet());
//        Collections.sort(sortedKeys);
//
//
//        try {
//            Map <String, Double> workflowStats = new HashMap<>();
//
//
//            File[] taskFiles = utility.getTaskFileList(TESTING_SET); // testing set
//            File[] vmFiles = utility.getVMFileList();
//
//            ArrayList<Double> overallMakeSpans = new ArrayList<>();
//
//            double overallMakespans = 0;
//            double overallJobs = 0;
//
//            ArrayList<Double> makeSpans = new ArrayList<>();
//
//            for (File tf : taskFiles) {// test cases
//                String filePath = tf.getPath();
//                utility.setDaxPath(filePath);
//
//                double tfMakespan = 0;
//                double tfJobs = 0;
//
//                ArrayList <Double> tfMkspans = new ArrayList<>();
//
//                for (File vmf : vmFiles){
//                    utility.setVmDaxPath(vmf.getPath());
//                    utility.setVmRatiosList();
//
//                    double vmMkspn = 0;
//                    double vmJobs = 0;
//
//                    List <Job> jobs = new ArrayList<>();
//
//                    jobs = GPSchedulingAlgorithmExample.runSimulation(utility);
//
//                    vmJobs = jobs.size();
//                    vmMkspn = utility.getCloudletsMakeSpan(jobs);
//
//                    tfJobs += vmJobs;
//                    tfMakespan += vmMkspn;
//
//                    overallJobs += vmJobs;
//                    overallMakespans += vmMkspn;
//
//                    double avgVmMkspn = vmMkspn / vmJobs;
//
//                    System.out.println("avgMkspn for " + tf.getName() + "_" + vmf.getName() + " is: " + avgVmMkspn);
//
//                    overallMakeSpans.add(avgVmMkspn);
//
//                    workflowStats.put(tf.getName() + "_" + vmf.getName(), avgVmMkspn);
//
//                }
//
//            }
//
//            state.output.println("TESTING overallMkspn: " + overallMakespans, log);
//
//            double overallAvgMkspan = overallMakespans / overallJobs;
//
//            state.output.println("TESTING overallAvgMkspn: " + overallAvgMkspan, log);
//
//            double mean = utility.getAverageMakespan(overallMakeSpans);
//
//            state.output.println("TESTING Number of scenarios : "+overallMakeSpans.size(), log);
//            state.output.println("TESTING Mean of overall makespans : "+mean, log);
//
//            double sd = utility.getStandardDeviation(overallMakeSpans);
//
//            state.output.println("TESTING Standard Deviation: "+sd, log);
//
//            List sortedWFKeys=new ArrayList(workflowStats.keySet());
//            Collections.sort(sortedWFKeys);
//
//            for (Object objKey : sortedWFKeys) {
//                String key = (String) objKey;
//                Double value = workflowStats.get(key);
//
//                state.output.println("TESTING The average makespan : " + key + " is: "+ value, log);
//            }
//
//            state.output.println("The best rule is: " + rule, log);
//            state.output.println("The fitness of the best gpInd is: "+gpInd.fitness.fitnessToStringForHumans(), log);
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

}

