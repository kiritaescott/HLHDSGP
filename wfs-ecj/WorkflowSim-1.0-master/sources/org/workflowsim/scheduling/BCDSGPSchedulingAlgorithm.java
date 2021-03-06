package org.workflowsim.scheduling;

import ec.EvolutionState;
import ec.Individual;
import ec.app.bcdsgp.DoubleData;
import ec.app.bcdsgp.BCDSGPUtility;
import ec.app.bcdsgp.MultiValuedRegression;
import ec.gp.ADFStack;
import ec.gp.GPIndividual;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.CondorVM;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.utils.Parameters;
import org.workflowsim.vmranking.MinMin;


public class BCDSGPSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    public BCDSGPSchedulingAlgorithm() {
        super();
    }


    @Override
    public void run() throws Exception {
        int size = getCloudletList().size();

        if (size <= 0) {
            return;
        }

        BCDSGPUtility utility = Parameters.getBCDSGPUtility();

        Individual ind = utility.getInd();
        EvolutionState state = utility.getState();
        int threadnum = utility.getThreadnum();
        DoubleData input = utility.getInput();
        ADFStack stack = utility.getStack();
        MultiValuedRegression problem = (MultiValuedRegression) utility.getProblem();

        //utility.orderCloudletsByNumOfChildren((ArrayList<Cloudlet>) getCloudletList());

        for (Object cloudletObj : getCloudletList()) {
            Cloudlet c = (Cloudlet) cloudletObj;
            double currentTime = CloudSim.clock();
            problem.waitingTime = currentTime - c.getArrivalTime();
            for (Object vmObj : getVmList()) {
                CondorVM vm = (CondorVM) vmObj;
                problem.taskSize = c.getCloudletTotalLength();
                problem.executionTime = problem.taskSize / vm.getMips();
                problem.vmSpeed = vm.getMips();
                problem.waitingTime = currentTime - c.getArrivalTime();
                problem.relativeFinishTime = problem.waitingTime + problem.executionTime;
                problem.expectedCompletionTime = utility.getExpectedCompletionTime(c, getVmList());
                problem.unitCost = vm.getCost();
                //problem.MINMIN = MinMin.getRanking(vm, c, getCloudletList(), getVmList());
                //problem.MAXMIN = utility.getMAXMIN(vm, getCloudletList());

                ((GPIndividual) ind).trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), problem);
                vm.setFitnessValue(input.x);
            }

            CondorVM vmSelected = utility.getVMWithMinFitness(getVmList());
            if (vmSelected == null){ break; }   //case where all vms are busy
            vmSelected.setState(WorkflowSimTags.VM_STATUS_BUSY);
            c.setVmId(vmSelected.getId());
            getScheduledList().add(c);
        }

    }
}
