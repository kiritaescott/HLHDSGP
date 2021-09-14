package org.workflowsim.vmranking;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;
import org.workflowsim.WorkflowSimTags;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Fcfs extends VMRanking {

    public double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList){
        double rank = Double.MAX_VALUE;

        ArrayList<PairingItem> rankedPairs = new ArrayList<>();

        for (Iterator it = cloudletList.iterator(); it.hasNext();) {
            Cloudlet cloudlet = (Cloudlet) it.next();
            for (Iterator itc = vmList.iterator(); itc.hasNext();) {
                CondorVM vm = (CondorVM) itc.next();
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    PairingItem pairingItem = new PairingItem(cloudlet, vm);
                    rankedPairs.add(pairingItem);
                }
            }
        }

        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            //System.out.println("Fcfs ranked pairs: "+p.toString());
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    //System.out.println(" Fcfs Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rankedPairs.indexOf(p));
                    return rankedPairs.indexOf(p);
                }
            }
        }

        System.out.println("Shouldn't get here");
        return rank;
    }

}
