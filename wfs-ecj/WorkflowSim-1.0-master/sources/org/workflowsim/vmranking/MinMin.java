package org.workflowsim.vmranking;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;
import org.workflowsim.WorkflowSimTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MinMin extends VMRanking {

    private static final List<Boolean> hasChecked = new ArrayList<>();

    public double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList){

        //System.out.println("Start of mnmn ranking with vm: "+targetVm.getId()+" and cloudlet: "+targetCloudlet.getCloudletId());
        double rank = Double.MAX_VALUE;

        ArrayList <PairingItem> rankedPairs = new ArrayList<>();

        //calculate the rank according to the MINMIN heuristic
        //return the rank of the targetVm
        int size = cloudletList.size();
        hasChecked.clear();
        for (int t = 0; t < size; t++) {
            hasChecked.add(false);
        }
        for (int i = 0; i < size; i++) {
            int minIndex = 0;
            Cloudlet minCloudlet = null;
            for (int j = 0; j < size; j++) {
                Cloudlet cloudlet = (Cloudlet) cloudletList.get(j);
                if (!hasChecked.get(j)) {
                    minCloudlet = cloudlet;
                    minIndex = j;
                    break;
                }
            }
            if (minCloudlet == null) {
                break;
            }


            for (int j = 0; j < size; j++) {
                Cloudlet cloudlet = (Cloudlet) cloudletList.get(j);
                if (hasChecked.get(j)) {
                    continue;
                }
                long length = cloudlet.getCloudletLength();
                if (length < minCloudlet.getCloudletLength()) {
                    minCloudlet = cloudlet;
                    minIndex = j;
                }
            }
            hasChecked.set(minIndex, true);

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
                    PairingItem pairingItem = new PairingItem(minCloudlet, vm);
                    rankedPairs.add(pairingItem);
                }
            }

        }

        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            //System.out.println("Mnmn ranked pairs: "+p.toString());
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    //System.out.println(" MnMn Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rank);
                    return rankedPairs.indexOf(p);
                }
            }
        }

        System.out.println("Shouldn't get here");
        return rank;
    }
}
