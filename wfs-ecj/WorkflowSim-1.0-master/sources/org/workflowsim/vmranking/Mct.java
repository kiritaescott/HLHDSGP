package org.workflowsim.vmranking;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;
import org.workflowsim.WorkflowSimTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Mct extends VMRanking {
    private static final List<Boolean> hasChecked = new ArrayList<>();

    public double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList){

        //System.out.println("Start of mct ranking with vm: "+targetVm.getId()+" and cloudlet: "+targetCloudlet.getCloudletId());
        double rank = Double.MAX_VALUE;

        ArrayList <PairingItem> rankedPairs = new ArrayList<>();

        int size = cloudletList.size();

        for (int i = 0; i < size; i++) {
            Cloudlet cloudlet = (Cloudlet) cloudletList.get(i);

            // sort vms by current requested total mips (in order of the most to the least)
            Collections.sort(vmList, new Comparator<CondorVM>() {
                @Override
                public int compare(CondorVM v1, CondorVM v2) {
                    return Double.compare(v2.getCurrentRequestedTotalMips(), v1.getCurrentRequestedTotalMips());
                }
            });

            for (Object vmObj : vmList){
                CondorVM vm = (CondorVM) vmObj;
                //machine is idle, create a pair and add it to the list
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE){
                    PairingItem pairingItem = new PairingItem(cloudlet, vm);
                    rankedPairs.add(pairingItem);
                }
            }
        }


        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            //System.out.println("Mnmn ranked pairs: "+p.toString());
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    //System.out.println(" Mct Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rank);
                    return rankedPairs.indexOf(p);
                }
            }
        }

        System.out.println("Shouldn't get here");
        return rank;
    }
}
