package org.workflowsim.scheduling;

import ec.EvolutionState;
import ec.Individual;
import ec.app.gphh.DoubleData;
import ec.app.gphh.GPHHUtility;
import ec.app.gphh.MultiValuedRegression;
import ec.gp.ADFStack;
import ec.gp.GPIndividual;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.utils.Parameters;
import org.workflowsim.vmranking.*;

import java.util.*;

public class GPHHSchedulingAlgorithm extends BaseSchedulingAlgorithm {


    public GPHHSchedulingAlgorithm() {
        super();
    }


    @Override
    public void run() throws Exception{
        int size = getCloudletList().size();

        System.out.println("In GPHH with: "+size+" tasks");

        if (size <= 0) {
            return;
        }
        //System.out.println("In GPHH run at: "+CloudSim.clock()+" with: "+size+" jobs.");

        GPHHUtility utility = Parameters.getGphhUtility();

        Individual ind = utility.getInd();
        EvolutionState state = utility.getState();
        int threadnum = utility.getThreadnum();
        DoubleData input = utility.getInput();
        ADFStack stack = utility.getStack();
        MultiValuedRegression problem = (MultiValuedRegression) utility.getProblem();

        ArrayList <PairingItem> pairs = new ArrayList<>();

        //
        MinMin minMin = new MinMin();
        MaxMin maxMin = new MaxMin();
        Fcfs fcfs = new Fcfs();
        RndRbn rndRbn = new RndRbn();
        GrpHeft grpHeft = new GrpHeft();
        Mct mct = new Mct();

        double currentTime = CloudSim.clock();  // arrival/allocation time


        // setup all ready task to available vms
        for(Object cloudletObj : getCloudletList()) {
            Cloudlet c = (Cloudlet) cloudletObj;

            //set allocation time, if not already set
            if (c.getAllocationTime() == null){
                c.setAllocationTime(currentTime);
                //System.out.println("Set allocation time of cloudlet: "+c.getCloudletId()+" to: "+c.getAllocationTime());
            }

            for (Object vmObj : getVmList()) {
                CondorVM vm = (CondorVM) vmObj;
                //check if vm is idle
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE){
                    PairingItem p = new PairingItem(c, vm);
                    //System.out.println("PairingItem p: "+p.toString());
                    if (!pairs.contains(p)){
                        //System.out.println("Adding PairingItem p to list: "+p.toString());
                        pairs.add(p);
                    }

                }
            }
        }

        //System.out.println("There are "+pairs.size()+" pairs in the list.");


        // rank all pairs in map according to gp tree
        for (PairingItem p : pairs){
            Cloudlet cloudlet =  p.getCloudlet();
            CondorVM vm =  p.getVm();

            problem.taskSize = cloudlet.getCloudletTotalLength();
            problem.executionTime = problem.taskSize / vm.getMips();
            problem.vmSpeed = vm.getMips();
            problem.unitCost = vm.getCost();
            problem.vmBw = vm.getBw();
            problem.waitingTime = currentTime - cloudlet.getAllocationTime();
            problem.responseTime = problem.waitingTime + problem.executionTime;
            problem.expectedCompletionTime = utility.getExpectedCompletionTime(cloudlet, getVmList());
            problem.MINMIN = minMin.getRanking(vm, cloudlet, getCloudletList(), getVmList());
            problem.MAXMIN = maxMin.getRanking(vm, cloudlet, getCloudletList(), getVmList());
            problem.FCFS = fcfs.getRanking(vm, cloudlet, getCloudletList(), getVmList());
            problem.RR = rndRbn.getRanking(vm, cloudlet, getCloudletList(), getVmList());
            problem.GRPHEFT = grpHeft.getRanking(vm, cloudlet, getCloudletList(), getVmList());
            problem.MCT = mct.getRanking(vm, cloudlet, getCloudletList(), getVmList());

            ((GPIndividual) ind).trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), problem);

            p.setRank(input.x);
        }

        // once all pairs are ranked, start allocating tasks to vms
        Collections.sort(pairs, new Comparator<PairingItem>() {
            @Override
            public int compare(PairingItem p1, PairingItem p2) {
                return Double.compare(p1.getRank(), p2.getRank());
            }
        });


        for (PairingItem p : pairs){
            System.out.println("p: "+p.toString());
        }

        for (PairingItem p : pairs){
            Cloudlet cloudlet =  p.getCloudlet();
            CondorVM vm =  p.getVm();

            if (!cloudlet.isScheduled()){
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    vm.setState(WorkflowSimTags.VM_STATUS_BUSY);
                    cloudlet.setVmId(vm.getId());
                    getScheduledList().add(cloudlet);
                    cloudlet.setScheduled(true);
                    System.out.println("CLOUDLET "+p.getCloudlet().getCloudletId()+" SCHEDULED TO VM "+p.getVm().getId());
                }
            }

        }

    }
}
