package org.workflowsim.examples.scheduling;

import ec.app.gphh.GPHHUtility;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.utils.ClusteringParameters;
import org.workflowsim.utils.OverheadParameters;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.ReplicaCatalog;

import java.io.File;
import java.util.*;

import static org.workflowsim.examples.scheduling.GPHHSchedulingAlgorithmExample.createDatacenter;
import static org.workflowsim.examples.scheduling.GPHHSchedulingAlgorithmExample.createVM;

public class GenericSchedulingAlgorithmExample {

        ////////////////////////// STATIC METHODS ///////////////////////
        /**
         * Creates main() to run this example This example has only one datacenter
         * and one storage
         */
        public static List<Job> runSimulation(GPHHUtility gpUtility) {

            try {
                // First step: Initialize the WorkflowSim package.

                int vmNum = 10;//number of vms; ... likely to be passed in as a parameter in future
                /**
                 * Should change this based on real physical path
                 */
                String daxPath = gpUtility.getDaxPath();

                File daxFile = new File(daxPath);
                if (!daxFile.exists()) {
                    Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");
                    return null;
                }

                /**
                 * Since we are using HEFT planning algorithm, the scheduling
                 * algorithm should be static such that the scheduler would not
                 * override the result of the planner
                 */
                Parameters.SchedulingAlgorithm sch_method = gpUtility.getAlgorithmParameters();
                Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.INVALID;
                ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.LOCAL;

                /**
                 * Set the cost model to be VM (the default is Datacenter
                 */
                Parameters.setCostModel(Parameters.CostModel.VM);

                /**
                 * No overheads
                 */
                OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);

                /**
                 * No Clustering
                 */
                ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
                ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);

                /**
                 * Initialize static parameters
                 */
                Parameters.init(vmNum, daxPath, null,
                        null, op, cp, sch_method, pln_method,
                        null, 0, gpUtility);
                ReplicaCatalog.init(file_system);

                // before creating any entities.
                int num_user = 1;   // number of grid users
                Calendar calendar = Calendar.getInstance();
                boolean trace_flag = false;  // mean trace events

                // Initialize the CloudSim library
                CloudSim.init(num_user, calendar, trace_flag);

               WorkflowDatacenter datacenter0 = createDatacenter("Datacenter_0");

                /**
                 * Create a WorkflowPlanner with one schedulers.
                 */
                WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);
                /**
                 * Create a WorkflowEngine.
                 */
                WorkflowEngine wfEngine = wfPlanner.getWorkflowEngine();
                /**
                 * Create a list of VMs.The userId of a vm is basically the id of
                 * the scheduler that controls this vm.
                 */
                List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), gpUtility.getVmRatios());

                /**
                 * Submits this list of vms to this WorkflowEngine.
                 */
                wfEngine.submitVmList(vmlist0, 0);


                /**
                 * Binds the data centers with the scheduler.
                 */
                wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);

                CloudSim.startSimulation();
                List<Job> outputList0 = wfEngine.getJobsReceivedList();
                CloudSim.stopSimulation();

                /**return the output list back to GP*/
                return outputList0;
            } catch (Exception e) {
                Log.printLine("The simulation has been terminated due to an unexpected error");
            }
            return null;    // should never get here
        }



}
