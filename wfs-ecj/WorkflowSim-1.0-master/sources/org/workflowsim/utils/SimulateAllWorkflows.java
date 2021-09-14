package org.workflowsim.utils;

import ec.app.bcdsgp.BCDSGPUtility;
import ec.app.gphh.GPHHUtility;
import ec.app.wfsgp.GPUtility;
import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.Job;
import org.workflowsim.examples.scheduling.GenericSchedulingAlgorithmExample;

import java.io.File;
import java.util.*;

public class SimulateAllWorkflows {
    private static final int TRAINING_SET = 0;
    private static final int TESTING_SET = 1;

    private static Map<String, String> workflowStats;

    public static void main(String [] args){
        try {
           GPHHUtility utility = new GPHHUtility();
            workflowStats = new HashMap<>();

            File[] taskFiles = utility.getTaskFileList(TRAINING_SET); // training set
            File[] vmFiles = utility.getVMFileList();

            ArrayList<Double> wfResponseTimes = new ArrayList<>();
            ArrayList<Double> allRespTasks = new ArrayList<>();

            double totalResponseTime = 0;

            for (File tf : taskFiles) {// test cases
                String filePath = tf.getPath();
                utility.setDaxPath(filePath);

                for (File vmf : vmFiles) {
                    utility.setVmDaxPath(vmf.getPath());
                    utility.setVmRatiosList();

                    Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.MINMIN;    // change according to which algorithm to run
                    utility.setAlgorithmParameters(sch_method);

                    List<Job> jobs = new ArrayList<>();

                    jobs = GenericSchedulingAlgorithmExample.runSimulation(utility);

                    double wfResponseTime = 0;
                    ArrayList<Double> respTimes = new ArrayList<>();

                    // get the response time of the workflow
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

                    double mean = utility.getAverageResponseTime(respTimes);
                    //System.out.println(tf.getName() + "_" + vmf.getName()+" average wfResponseTime: "+ mean);

                    double sd = utility.getStandardDeviation(respTimes);
                    //System.out.println("Standard Deviation of : "+tf.getName() + "_" + vmf.getName()+" is "+sd);

                    wfResponseTimes.add(mean);

                    workflowStats.put(tf.getName() + "_" + vmf.getName(), "avg resp: "+mean+" sd: "+sd);
                }

            }

//
//            double allRespMean = utility.getAverageResponseTime(allRespTasks);
//            System.out.println("Total number of respTasks : "+allRespTasks.size());
//            System.out.println("Mean of respTasks respsonse times : "+allRespMean);
//            double allRespSD = utility.getStandardDeviation(allRespTasks);
//            System.out.println("allRespTasks Standard Deviation: "+allRespSD);

            double mean = utility.getAverageResponseTime(wfResponseTimes);
            System.out.println("Number of scenarios : "+wfResponseTimes.size());
            System.out.println("Mean of overall respsonse times : "+mean);
            double sd = utility.getStandardDeviation(wfResponseTimes);
            System.out.println("Standard Deviation: "+sd);

            System.out.println("--------------------------------------------------------------");

            System.out.printf("Formatted Number of scenarios : %d\n", wfResponseTimes.size());
            System.out.printf("%.4f\n", mean);
            System.out.printf("%.4f\n", sd);


            List sortedKeys=new ArrayList(workflowStats.keySet());
            Collections.sort(sortedKeys);
            Collections.reverse(sortedKeys);

            for (Object objKey : sortedKeys) {
                String key = (String) objKey;
                String value = workflowStats.get(key);

               //System.out.println("For file : " + key + " "+ value);

                String [] values = value.split(" ");

                double resp = Double.parseDouble(values[2]);
                double std = Double.parseDouble(values[4]);

                System.out.printf("%s %.4f %.4f \n", key, resp, std);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
