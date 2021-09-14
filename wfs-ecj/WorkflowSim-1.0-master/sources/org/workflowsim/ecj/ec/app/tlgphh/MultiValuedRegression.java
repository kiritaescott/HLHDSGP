///*
//  Copyright 2006 by Sean Luke
//  Licensed under the Academic Free License version 3.0
//  See the file "LICENSE" for more information
//*/
//
//
//package ec.app.gphh;
//import ec.util.*;
//import ec.*;
//import ec.gp.*;
//import ec.simple.*;
//import org.cloudbus.cloudsim.Cloudlet;
//import org.cloudbus.cloudsim.core.CloudSim;
//import org.workflowsim.Job;
//import org.workflowsim.examples.scheduling.GPHHSchedulingAlgorithmExample;
//
//import java.io.File;
//import java.util.*;
//
//public class MultiValuedRegression extends GPProblem implements SimpleProblemForm {
//    private static final long serialVersionUID = 1;
//
//    public double currentX;
//    public double currentY;
//    public double taskSize;
//    public double executionTime;
//    public double vmSpeed;
//    public double waitingTime;
//    public double responseTime;
//    public double unitCost;
//    public double vmBw;
//    public double expectedCompletionTime;
//    public double MINMIN;
//    public double MAXMIN;
//    public double FCFS;
//    public double RR;
//    public double GRPHEFT;
//    public double MCT;
//
//
//    private static final int TRAINING_SET = 0;
//    private static final int TESTING_SET = 1;
//
//    private static Map<String, String> workflowStats;
//
//
//
//    public void setup(final EvolutionState state, final Parameter base) {
//        super.setup(state, base);
//
//        // verify our input is the right class (or subclasses from it)
//        if (!(input instanceof DoubleData))
//            state.output.fatal("GPData class must subclass from " + DoubleData.class, base.push(P_DATA), null);
//    }
//
//    public void evaluate(final EvolutionState state,
//                         final Individual ind,
//                         final int subpopulation,
//                         final int threadnum)
//    {
//        if (!ind.evaluated) // don't bother reevaluating
//        {
//            DoubleData input = (DoubleData) (this.input);
//            GPHHUtility utility = new GPHHUtility();
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
//                File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
//                utility.setTaskFiles(taskFiles);
//
//                File[] vmFiles = utility.getVMFileList();
//
//                ArrayList<Double> wfResponseTimes = new ArrayList<>();
//                ArrayList<Double> allRespTasks = new ArrayList<>();
//
//                workflowStats = new HashMap<>();
//
//                GPIndividual gpInd = (GPIndividual) ind;
//
//                final long startTimeAll= System.currentTimeMillis();
//
//
//                    for (File vmf : vmFiles) {
//                        utility.setVmDaxPath(vmf.getPath());
//                        utility.setVmRatiosList();
//
//                        List<Job> jobs = new ArrayList<>();
//
//                        //jobs = GPHHSchedulingAlgorithmExample.runSimulation(utility);
//
//                        GPHHSchedulingAlgorithmExample gphhSchedulingAlgorithmExample = new GPHHSchedulingAlgorithmExample(utility);
//                        final long startTimeMilliSec = System.currentTimeMillis();
//
//                        double startTime = CloudSim.clock();
//                        System.out.println("Starting at: "+startTime);
//                        gphhSchedulingAlgorithmExample.run();
//
//                        double wfResponseTime = 0;
//                        ArrayList<Double> respTimes = new ArrayList<>();
//
//
//                        // get the response time of the workflow
//                        jobs = gphhSchedulingAlgorithmExample.getFinishedCloudletList();
//
//                        final long finishTimeMilliSec = System.currentTimeMillis() - startTimeMilliSec;
//
//                        System.out.printf("Time to run %d jobs: %d milliseconds%n", jobs.size(), finishTimeMilliSec);
//
//                        // calculate the average
//                        System.out.println("Jobs returned: "+jobs.size());
//
//                        Collections.sort(jobs, new Comparator<Job>() {
//                            @Override
//                            public int compare(Job v1, Job v2) {
//                                return Double.compare(v1.getCloudletId(), v2.getCloudletId());
//                            }
//                        });
//
//                        for (Job j : jobs){
//                            Cloudlet c = (Cloudlet) j;
//                            double taskResp = c.getFinishTime() - c.getAllocationTime();
//                            //System.out.println(c.getCloudletId()+" resp time of task: "+taskResp);
//                            respTimes.add(taskResp);
//                            allRespTasks.add(taskResp);
//                            wfResponseTime += taskResp;
//                        }
//                        //System.out.println(tf.getName() + "_" + vmf.getName()+" total wfResponseTime: "+ wfResponseTime);
//
//                        double mean = utility.getAverageResponseTime(respTimes);
//                        //System.out.println(tf.getName() + "_" + vmf.getName()+" average wfResponseTime: "+ mean);
//
//                        double sd = utility.getStandardDeviation(respTimes);
//                        //System.out.println("Standard Deviation of : "+tf.getName() + "_" + vmf.getName()+" is "+sd);
//
//                        wfResponseTimes.add(mean);
//
//                        workflowStats.put(vmf.getName(), "avg resp: "+mean+" sd: "+sd);
//                    }
//
//
//                final long finishTimeAll = System.currentTimeMillis() - startTimeAll;
//                System.out.printf("Time to run all scenarios: %d milliseconds%n", finishTimeAll);
//
//                double mean = utility.getAverageResponseTime(wfResponseTimes);
//                System.out.println("Number of scenarios : "+wfResponseTimes.size());
//                System.out.println("Mean of overall respsonse times : "+mean);
//                double sd = utility.getStandardDeviation(wfResponseTimes);
//                System.out.println("Standard Deviation: "+sd);
//
//                List sortedWFKeys=new ArrayList(workflowStats.keySet());
//                Collections.sort(sortedWFKeys);
//
//                for (Object objKey : sortedWFKeys) {
//                    String key = (String) objKey;
//                    String value = workflowStats.get(key);
//                    System.out.println("For file : " + key + " "+ value);
//                }
//
//                /**
//                 * set the average average total time for the GP individual
//                 * */
//                gpInd.setTotalTime(mean);
//                gpInd.setWorkflowResps(workflowStats);
//
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
//        GPHHUtility utility = new GPHHUtility();
//
//        utility.setState(state);
//        utility.setInd(ind);
//        utility.setSubpopulation(subpopulation);
//        utility.setThreadnum(threadnum);
//        utility.setInput(input);
//        utility.setStack(stack);
//        utility.setProblem(this);
//
//        try {
//
//            System.out.print("BEGINNING OF TESTING");
//            state.output.println("BEGINNING OF TESTING", log);
//
//            File[] taskFiles = utility.getTaskFileList(TESTING_SET); // training set
//            File[] vmFiles = utility.getVMFileList();
//
//            ArrayList<Double> wfResponseTimes = new ArrayList<>();
//            ArrayList<Double> allRespTasks = new ArrayList<>();
//
//            workflowStats = new HashMap<>();
//
//            final long startTimeAll= System.currentTimeMillis();
//
//
//            for (File vmf : vmFiles) {
//                utility.setVmDaxPath(vmf.getPath());
//                utility.setVmRatiosList();
//
//                List<Job> jobs = new ArrayList<>();
//
//                //jobs = GPHHSchedulingAlgorithmExample.runSimulation(utility);
//
//                GPHHSchedulingAlgorithmExample gphhSchedulingAlgorithmExample = new GPHHSchedulingAlgorithmExample(utility);
//                final long startTimeMilliSec = System.currentTimeMillis();
//
//                double startTime = CloudSim.clock();
//                System.out.println("Starting at: "+startTime);
//                gphhSchedulingAlgorithmExample.run();
//
//                double wfResponseTime = 0;
//                ArrayList<Double> respTimes = new ArrayList<>();
//
//
//                // get the response time of the workflow
//                jobs = gphhSchedulingAlgorithmExample.getFinishedCloudletList();
//
//                final long finishTimeMilliSec = System.currentTimeMillis() - startTimeMilliSec;
//
//                System.out.printf("Time to run %d jobs: %d milliseconds%n", jobs.size(), finishTimeMilliSec);
//
//                // calculate the average
//                System.out.println("Jobs returned: "+jobs.size());
//
//                Collections.sort(jobs, new Comparator<Job>() {
//                    @Override
//                    public int compare(Job v1, Job v2) {
//                        return Double.compare(v1.getCloudletId(), v2.getCloudletId());
//                    }
//                });
//
//                for (Job j : jobs){
//                    Cloudlet c = (Cloudlet) j;
//                    double taskResp = c.getFinishTime() - c.getAllocationTime();
//                    //System.out.println(c.getCloudletId()+" resp time of task: "+taskResp);
//                    respTimes.add(taskResp);
//                    allRespTasks.add(taskResp);
//                    wfResponseTime += taskResp;
//                }
//                //System.out.println(tf.getName() + "_" + vmf.getName()+" total wfResponseTime: "+ wfResponseTime);
//
//                double mean = utility.getAverageResponseTime(respTimes);
//                //System.out.println(tf.getName() + "_" + vmf.getName()+" average wfResponseTime: "+ mean);
//
//                double sd = utility.getStandardDeviation(respTimes);
//                //System.out.println("Standard Deviation of : "+tf.getName() + "_" + vmf.getName()+" is "+sd);
//
//                wfResponseTimes.add(mean);
//
//                workflowStats.put(vmf.getName(), "avg resp: "+mean+" sd: "+sd);
//            }
//
//            final long finishTimeAll = System.currentTimeMillis() - startTimeAll;
//            System.out.printf("Time to TEST all scenarios: %d milliseconds%n", finishTimeAll);
//            state.output.println("Time to TEST all scenarios: "+ finishTimeAll +" milliseconds", log);
//            double mean = utility.getAverageResponseTime(wfResponseTimes);
//            System.out.println("Number of scenarios : "+wfResponseTimes.size());
//            System.out.println("Mean of overall respsonse times : "+mean);
//            state.output.println("TESTING Number of scenarios : "+wfResponseTimes.size(), log);
//            state.output.println("TESTING Mean of overall respsonse times : "+mean, log);
//            double sd = utility.getStandardDeviation(wfResponseTimes);
//            System.out.println("Standard Deviation: "+sd);
//            state.output.println("TESTING Standard Deviation: "+sd, log);
//
//            List sortedWFKeys=new ArrayList(workflowStats.keySet());
//            Collections.sort(sortedWFKeys);
//
//            for (Object objKey : sortedWFKeys) {
//                String key = (String) objKey;
//                String value = workflowStats.get(key);
//                System.out.println("For file : " + key + " "+ value);
//                state.output.println("TESTING For file : " + key + " "+ value, log);
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
//
//}



/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.app.tlgphh;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPProblem;
import ec.gp.GPTree;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.Job;
import org.workflowsim.examples.scheduling.GPHHSchedulingAlgorithmExample;

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
    public double responseTime;
    public double unitCost;
    public double vmBw;
    public double expectedCompletionTime;
    public double MINMIN;
    public double MAXMIN;
    public double FCFS;
    public double RR;
    public double GRPHEFT;
    public double MCT;


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
            Utility utility = new Utility();

            utility.setState(state);
            utility.setInd(ind);
            utility.setSubpopulation(subpopulation);
            utility.setThreadnum(threadnum);
            utility.setInput(input);
            utility.setStack(stack);
            utility.setProblem(this);

            try {

                File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
                File[] vmFiles = utility.getVMFileList();

                GPIndividual gpInd = (GPIndividual) ind;

                /**
                 * Randomly generate our scenario:
                 *  size of workflow(s)
                 *  pattern of workflow(s)
                 *  # of different patterns
                 *  arrival of workflows
                 * */




                /**
                 * set the XXX fitness measurement for the GP individual
                 * */
                //gpInd.setSomething(value);


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

        Utility utility = new Utility();

        utility.setState(state);
        utility.setInd(ind);
        utility.setSubpopulation(subpopulation);
        utility.setThreadnum(threadnum);
        utility.setInput(input);
        utility.setStack(stack);
        utility.setProblem(this);

        try {

            System.out.print("BEGINNING OF TESTING");
            state.output.println("BEGINNING OF TESTING", log);

            File[] taskFiles = utility.getTaskFileList(TESTING_SET); // training set
            File[] vmFiles = utility.getVMFileList();

            /**
             * Do stuff for testing here
             * */


            state.output.println("The best rule is: " + rule, log);
            state.output.println("The fitness of the best gpInd is: "+gpInd.fitness.fitnessToStringForHumans(), log);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}



