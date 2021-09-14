package org.workflowsim.vmranking;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.scheduling.RoundRobinSchedulingAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RndRbn extends VMRanking {
    public double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList) {
        double rank = Double.MAX_VALUE;
        ArrayList<PairingItem> rankedPairs = new ArrayList<>();

        //rank pairs according to round robin heuristic
        int vmIndex = 0;
        int size = cloudletList.size();
        Collections.sort(cloudletList, new Comparator<Cloudlet>() {
            @Override
            public int compare(Cloudlet c1, Cloudlet c2) {
                return Double.compare(c2.getCloudletId(), c1.getCloudletId());
            }
        });

        Collections.sort(vmList, new Comparator<CondorVM>() {
            @Override
            public int compare(CondorVM v1, CondorVM v2) {
                return Double.compare(v2.getId(), v1.getId());
            }
        });

        for (int j = 0; j < size; j++) {
            Cloudlet cloudlet = (Cloudlet) cloudletList.get(j);
            int vmSize = vmList.size();

            for (int l = 0; l < vmSize; l++) {
                CondorVM vm = (CondorVM) vmList.get(l);
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    PairingItem pairingItem = new PairingItem(cloudlet, vm);
                    rankedPairs.add(pairingItem);
                }
            }

            vmIndex = (vmIndex + 1) % vmList.size();
        }

        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            //System.out.println("Mnmn ranked pairs: "+p.toString());
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    //System.out.println(" RndRbn Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rankedPairs.indexOf(p));
                    return rankedPairs.indexOf(p);
                }
            }
        }
        //shouldn't get here
        return rank;
    }
}
