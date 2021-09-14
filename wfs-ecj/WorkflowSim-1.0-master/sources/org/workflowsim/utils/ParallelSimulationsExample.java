package org.workflowsim.utils;

import ec.app.gphh.GPHHUtility;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.workflowsim.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static org.workflowsim.examples.scheduling.GPHHSchedulingAlgorithmExample.createDatacenter;
import static org.workflowsim.examples.scheduling.GPHHSchedulingAlgorithmExample.createVM;

public class ParallelSimulationsExample  {
    private final String title;
    private final GPHHUtility utility;
    private List <Job> cloudletList;

    private List<Job> finishedCloudletList;
    private CloudSim simulation;

    private DatacenterBroker broker;
    private List<CondorVM> vmList;
    private int hostsToCreate;
    private int vmsToCreate;
    private int cloudletsToCreate;

    private Parameters.SchedulingAlgorithm sch_method;
    private Parameters.PlanningAlgorithm pln_method;
    private ReplicaCatalog.FileSystem file_system;
    private OverheadParameters op;
    private ClusteringParameters.ClusteringMethod method;
    private ClusteringParameters cp;

    private WorkflowEngine wfEngine;
    private WorkflowPlanner wfPlanner;
    private WorkflowDatacenter datacenter0;

    private Calendar calendar;

    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example This example has only one datacenter
     * and one storage
     */
    public static void main(String[] args) {
        /*IT IS MANDATORY TO DISABLE THE LOG WHEN EXECUTING PARALLEL SIMULATIONS TO AVOID RUNTIME EXCEPTIONS.*/
       // Log.setLevel(Level.OFF);
        GPHHUtility gphhUtility1 = new GPHHUtility();
        gphhUtility1.setDaxPath("/Users/koe/Documents/Gitlab-Thesis/thesis/WFS-ECJ/WFSP_Grid_Files/config/workflows/training/CyberShake_50.xml");
        gphhUtility1.setVmDaxPath("/Users/koe/Documents/Gitlab-Thesis/thesis/WFS-ECJ/WFSP_Grid_Files/config/vms/vm_16.json");
        gphhUtility1.setAlgorithmParameters(Parameters.SchedulingAlgorithm.FCFS);
        gphhUtility1.setVmRatiosList();
        gphhUtility1.setSceNum(1);

        GPHHUtility gphhUtility2 = new GPHHUtility();
        gphhUtility2.setDaxPath("/Users/koe/Documents/Gitlab-Thesis/thesis/WFS-ECJ/WFSP_Grid_Files/config/workflows/training/Inspiral_50.xml");
        gphhUtility2.setVmDaxPath("/Users/koe/Documents/Gitlab-Thesis/thesis/WFS-ECJ/WFSP_Grid_Files/config/vms/vm_16.json");
        gphhUtility2.setAlgorithmParameters(Parameters.SchedulingAlgorithm.MCT);
        gphhUtility2.setVmRatiosList();
        gphhUtility2.setSceNum(2);

        List<ParallelSimulationsExample> simulationList = new ArrayList<>(2);

        //Creates the first simulation scenario
        simulationList.add(
                new ParallelSimulationsExample("Simulation1", gphhUtility1)
        );

        //Creates the second simulation scenario
        simulationList.add(
                new ParallelSimulationsExample("Simulation2", gphhUtility2)

        );


        final long startTimeMilliSec = System.currentTimeMillis();
        //Uses Java 8 Streams to execute the simulation scenarios in parallel.
        // tag::parallelExecution[]
        simulationList.parallelStream().forEach(ParallelSimulationsExample::run);
        // end::parallelExecution[]

        final long finishTimeMilliSec = System.currentTimeMillis() - startTimeMilliSec;

        System.out.printf("Time to run %d simulations: %d milliseconds%n", simulationList.size(), finishTimeMilliSec);

        //Prints the cloudlet list of all executed simulations
        simulationList.forEach(ParallelSimulationsExample::printResults);
    }

    public ParallelSimulationsExample(String title, GPHHUtility utility){
        this.title = title;
        this.utility = utility;
        this.cloudletList = new ArrayList<>();
        this.finishedCloudletList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.simulation = new CloudSim();
        setupParameters();
    }

    public void setupVMs(String daxPath){

    }

    public void setupWorkflow(String daxPath){}

    public ParallelSimulationsExample setupParameters(){
        /**
         * Since we are using HEFT planning algorithm, the scheduling
         * algorithm should be static such that the scheduler would not
         * override the result of the planner
         */
        this.sch_method = this.utility.getAlgorithmParameters();
        this.pln_method = Parameters.PlanningAlgorithm.INVALID;
        this.file_system = ReplicaCatalog.FileSystem.LOCAL;

        /**
         * Set the cost model to be VM (the default is Datacenter
         */
        //Parameters.setCostModel(Parameters.CostModel.VM);

        /**
         * No overheads
         */
        this.op = new OverheadParameters(0, null, null, null, null, 0);

        /**
         * No Clustering
         */
        this.method = ClusteringParameters.ClusteringMethod.NONE;
        this.cp = new ClusteringParameters(0, 0, method, null);

        return this;
    }

    public void run() {
        System.out.println("this sim: "+this.title+" has daxPath: "+this.utility.getDaxPath());

        try {
            // First step: Initialize the WorkflowSim package.

            int vmNum = 10;//number of vms; ... likely to be passed in as a parameter in future
            /**
             * Should change this based on real physical path
             */
            String daxPath = this.utility.getDaxPath();

            File daxFile = new File(daxPath);
            if (!daxFile.exists()) {
                Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");

            }

            /**
             * Initialize static parameters
             */
            Parameters.init(vmNum, daxPath, null,
                    null, op, cp, sch_method, pln_method,
                    null, 0, this.utility);
            ReplicaCatalog.init(file_system);

            // before creating any entities.
            int num_user = 1;   // number of grid users
            this.calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            this.simulation.init(num_user, calendar, trace_flag);

            this.datacenter0 = createDatacenter("datacenter_0");

            /**
             * Create a WorkflowPlanner with one schedulers.
             */
            this.wfPlanner = new WorkflowPlanner("planner_0", 1);
            /**
             * Create a WorkflowEngine.
             */
            this.wfEngine = wfPlanner.getWorkflowEngine();
            /**
             * Create a list of VMs.The userId of a vm is basically the id of
             * the scheduler that controls this vm.
             */
            this.vmList = createVM(wfEngine.getSchedulerId(0), this.utility.getVmRatios());

            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmList, 0);


            /**
             * Binds the data centers with the scheduler.
             */
            wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);

            this.simulation.startSimulation();
            this.cloudletList = wfEngine.getJobsReceivedList();
            this.simulation.stopSimulation();

        } catch (Exception e) {
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }

    }

    public void printResults(){
        System.out.println("results are good for: "+this.title);
        System.out.println("jobs returned: "+this.cloudletList.size());
    }

}
