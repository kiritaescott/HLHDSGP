package org.workflowsim.vmranking;

import javafx.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.utils.Parameters;

import java.util.*;

public class GrpHeft extends VMRanking {
    private Map<Cloudlet, Map<CondorVM, Double>> computationCosts;
    private Map<Cloudlet, Map<Cloudlet, Double>> transferCosts;
    private Map<Cloudlet, Double> rank;
    private Map<CondorVM, List<Event>> schedules;
    private Map<Cloudlet, Double> earliestFinishTimes;
    private double averageBandwidth;
    private List<CloudletRank> cloudletRank;
    private List <Cloudlet> allCloudlets;
    private int HOUR = 3600;
    private boolean addingChangesCost = true;
    ArrayList<PairingItem> rankedPairs;

    private class Event {

        public double start;
        public double finish;

        public Event(double start, double finish) {
            this.start = start;
            this.finish = finish;
        }
    }

    private class CloudletRank implements Comparable<CloudletRank> {

        public Cloudlet cloudlet;
        public Double rank;

        public CloudletRank(Cloudlet cloudlet, Double rank) {
            this.cloudlet = cloudlet;
            this.rank = rank;
        }

        @Override
        public int compareTo(CloudletRank o) {
            return o.rank.compareTo(rank);
        }
    }

    public void getAllCloudletsChildren(List <Task> tasks, double allocationTime){
        //System.out.println("Adding children to allCloudlets");
        for (Task t : tasks){
            if (!allCloudlets.contains(t)){
                allCloudlets.add(t);
                t.setAllocationTime(allocationTime);
                if (!t.getChildList().isEmpty()){
                    getAllCloudletsChildren(t.getChildList(), allocationTime);
                }
            }
        }
    }

    private double calculateAverageBandwidth(List vmList) {
        double avg = 0.0;
        for (Object vmObject : vmList) {
            CondorVM vm = (CondorVM) vmObject;
            avg += vm.getBw();
        }
        return avg / vmList.size();
    }

    private void calculateComputationCosts(List vmList) {
        for (Object cloudletObject : allCloudlets) {
            Cloudlet cloudlet = (Cloudlet) cloudletObject;
            Map<CondorVM, Double> costsVm = new HashMap<>();
            for (Object vmObject : vmList) {
                CondorVM vm = (CondorVM) vmObject;
                if (vm.getNumberOfPes() < cloudlet.getNumberOfPes()) {
                    costsVm.put(vm, Double.MAX_VALUE);
                } else {
                    costsVm.put(vm,
                            cloudlet.getCloudletTotalLength() / vm.getMips());
                }
            }
            computationCosts.put(cloudlet, costsVm);
        }

//        System.out.println("size of computationcosts: "+computationCosts.size());
//        for (Map.Entry<Cloudlet, Map<CondorVM, Double>> entry : computationCosts.entrySet()){
//            System.out.println("cloudlet key: "+entry.getKey().getCloudletId());
//            Map<CondorVM, Double> map = entry.getValue();
//            for (Map.Entry<CondorVM, Double> entry1 : map.entrySet()){
//                System.out.println("vm key: "+entry1.getKey().getId()+" cost val: "+entry1.getValue());
//            }
//        }
    }

    private void calculateTransferCosts() {
        // Initializing the matrix

        for (Object cloudletObj1 : allCloudlets){
            Cloudlet cloudlet1 = (Cloudlet) cloudletObj1;
            // System.out.println("cloudlet 1 id: "+cloudlet1.getCloudletId());
            Map<Cloudlet, Double> cloudletTransferCosts = new HashMap<>();
            for (Object cloudletObj2 : allCloudlets) {
                Cloudlet cloudlet2 = (Cloudlet) cloudletObj2;
                // System.out.println("cloudlet 2 id: "+cloudlet2.getCloudletId());
                cloudletTransferCosts.put(cloudlet2, 0.0);
            }
            transferCosts.put(cloudlet1, cloudletTransferCosts);
        }

        // Calculating the actual values
        for (Object parentObj : allCloudlets) {
            Cloudlet parent = (Cloudlet) parentObj;
            Job parentJob = (Job) parentObj;
            for (Task task : parentJob.getChildList()){
                Cloudlet child = null;
                for (Object childObj: allCloudlets){
                    Cloudlet childCloudlet = (Cloudlet) childObj;
                    if (task.getCloudletId()==childCloudlet.getCloudletId()){
                        //   System.out.println("found the child: "+childCloudlet.getCloudletId());
                        child = childCloudlet;
                        break;
                    }
                }
                if (child == null){
                    break;
                }
                transferCosts.get(parent).put(child, calculateTransferCost(parent, child));
            }
        }

//        System.out.println("size of transfercosts: "+transferCosts.size());
//        for (Map.Entry<Cloudlet, Map<Cloudlet, Double>> entry : transferCosts.entrySet()){
//            System.out.println("cloudlet key: "+entry.getKey().getCloudletId());
//            Map<Cloudlet, Double> map = entry.getValue();
//            for (Map.Entry<Cloudlet, Double> entry1 : map.entrySet()){
//                System.out.println("c key: "+entry1.getKey().getCloudletId()+" cost val: "+entry1.getValue());
//            }
//        }
    }

    private double calculateTransferCost(Cloudlet parent, Cloudlet child) {
        //System.out.println("parent: "+parent.getCloudletId()+" child: "+child.getCloudletId());

        Job parentJob = (Job) parent;
        Job childJob = (Job) child;

        /**
         * In our experiments a Job object's task list will only ever have one Task
         * This is due to NOT making use of clustering in the Clustering Engine
         * Should that change we will need to modify our implementation of HEFT
         * */
        List<FileItem> parentFiles = parentJob.getFileList();
        List<FileItem> childFiles = childJob.getFileList();

        // System.out.println("parentFiles: "+parentFiles.size()+" chilFiles: "+childFiles.size());


        double acc = 0.0;

        for (FileItem parentFile : parentFiles) {
            //System.out.println("parentFile: "+parentFile.getName()+" "+parentFile.getType());
            if (parentFile.getType() != Parameters.FileType.OUTPUT) {
                continue;
            }

            for (FileItem childFile : childFiles) {
                // System.out.println("childFIels: "+childFile.getName()+" "+childFile.getType());
                if (childFile.getType() == Parameters.FileType.INPUT
                        && childFile.getName().equals(parentFile.getName())) {
                    // System.out.println("calculating child/parent transfer");
                    acc += childFile.getSize();
                    //  System.out.println("acc: "+acc);
                    break;
                }
            }
        }
        //file Size is in Bytes, acc in MB
        acc = acc / Consts.MILLION;
        // acc in MB, averageBandwidth in Mb/s
        return acc * 8 / averageBandwidth;
    }

    private void calculateRanks() {
        // only rank all the cloudlets
        for (Object cloudletObj : allCloudlets) {
            Cloudlet cloudlet = (Cloudlet) cloudletObj;
            calculateRank(cloudlet);
        }
    }

    private double calculateRank(Cloudlet cloudlet) {
        if (rank.containsKey(cloudlet)) {
            return rank.get(cloudlet);
        }

        double averageComputationCost = 0.0;

        for (Double cost : computationCosts.get(cloudlet).values()) {
            averageComputationCost += cost;
        }

        averageComputationCost /= computationCosts.get(cloudlet).size();

        double max = 0.0;

        Job parent = (Job) cloudlet;
        for (Task taskChild : parent.getChildList()){
            for (Object cloudletChildObj: allCloudlets){
                Cloudlet child = (Cloudlet) cloudletChildObj;
                if (child.getCloudletId()==taskChild.getCloudletId()){
                    double childCost = transferCosts.get(cloudlet).get(child)
                            + calculateRank(child);
                    max = Math.max(max, childCost);
                }
            }

        }

        rank.put(cloudlet, averageComputationCost + max);

        return rank.get(cloudlet);
    }

    private void allocateCloudlets(List vmList, List cloudletList) {
        for (Cloudlet cloudlet : rank.keySet()) {
            cloudletRank.add(new CloudletRank(cloudlet, rank.get(cloudlet)));
        }

        // Sorting in descending order of rank
        Collections.sort(cloudletRank);
        for (CloudletRank rank : cloudletRank) {
            allocateCloudlet(rank.cloudlet, vmList, cloudletList);
        }

    }

    private void allocateCloudlet(Cloudlet cloudlet, List vmList, List cloudletList){
        /**
         * A cloudlet has a list of vms and their finish times, ranked according
         * to the minFinishTime of this allocation, after allocation we add them to the
         * rankedPairs list
         * */
        Map <CondorVM, List<Double>> finishTimesOnVM = new HashMap<>();

        Map <CondorVM, Double> allFins = new HashMap<>();
        Map <CondorVM, Double> iminFins = new HashMap<>();
        Map <CondorVM, Double> imin2Fins = new HashMap<>();

        List <PairingItem> pairs = new ArrayList<>();

        CondorVM chosenVM = null;
        CondorVM Imin = null;
        CondorVM Imin2 = null;

        double finishTime;
        double finishTimeMin = Double.MAX_VALUE;
        double finishTimeMin2 = Double.MAX_VALUE;
        double earliestFinishTime = Double.MAX_VALUE;

        double minReadyTime = 0.0;
        double bestReadyTime = 0.0;

        double currentTime = CloudSim.clock();
        minReadyTime = currentTime;

        Job job = (Job) cloudlet;
        List <Job> parents = job.getParentList();

        for (Object vObj : vmList){
            CondorVM vm = (CondorVM) vObj;

            if (!parents.isEmpty()){
                //System.out.println("This cloudlet has number parents: "+parents.size());
                for (Task parent : parents) {
                    Cloudlet p = parent;
                    //System.out.println("parent id: "+p.getCloudletId()+" of me: "+cloudlet.getCloudletId());
                    double readyTime = 0.0;

                    if (earliestFinishTimes.containsKey(p) && transferCosts.containsKey(p)){
                        readyTime = earliestFinishTimes.get(p);

                        //System.out.println("calculating transfer costs - Id of p: "+p.getCloudletId()+" child: "+cloudlet.getCloudletId());
                        if (p.getVmId() != vm.getId()) {
                            readyTime += transferCosts.get(p).get(cloudlet);
                        }
                        //ystem.out.println("minReadyTime "+minReadyTime+" and readyTime "+readyTime);
                        minReadyTime = Math.max(minReadyTime, readyTime);
                    }

                }
            }

            finishTime = findFinishTime(cloudlet, vm, minReadyTime, false);
            doesAddingChangeCost(minReadyTime, finishTime);
            // add finish time to the list of times
            allFins.put(vm, finishTime);

            //System.out.println("finish time to check of vm: "+vm.getId()+" is: "+finishTime);
            if (finishTime < finishTimeMin && vm.getState()==WorkflowSimTags.VM_STATUS_IDLE) {
                //System.out.println("updating Imin to vm: "+vm.getId());
                finishTimeMin = finishTime;
                bestReadyTime = minReadyTime;
                Imin = vm;
                chosenVM = Imin;
                cloudlet.setVmId(chosenVM.getId());
                iminFins.put(vm, finishTime);
            }

            if (!addingChangesCost && finishTime < finishTimeMin2
                    && vm.getState()==WorkflowSimTags.VM_STATUS_IDLE ){
                //System.out.println("Adding cloudlet doesn't change cost, updating Imin2 to: "+vm.getId());
                finishTimeMin2 = finishTime;
                bestReadyTime = minReadyTime;
                Imin2 = vm;
                chosenVM = Imin2;
                cloudlet.setVmId(chosenVM.getId());
                imin2Fins.put(vm, finishTime);
            }

        }

        if (chosenVM == null){return;}

        if (!Imin.equals(Imin2) ){
            //System.out.println("Imin different to Imin2");
            if (Imin2 != null && Imin2.getState()==WorkflowSimTags.VM_STATUS_IDLE){
                //System.out.println("Imin2 not null, assigning c "+cloudlet.getCloudletId()+" to Imin2 id: "+Imin2.getId());
                // assign to this one
                chosenVM = Imin2;
                cloudlet.setVmId(chosenVM.getId());
                cloudlet.setScheduled(true);
                earliestFinishTime = finishTimeMin2;
                PairingItem pair = new PairingItem(cloudlet, Imin2);
                pairs.add(pair);
            }
        }

        //if (cloudlet.isScheduled()){ System.out.println("c "+cloudlet.getCloudletId()+" has been scheduled to: "+cloudlet.getVmId());}
        if (!cloudlet.isScheduled() && Imin.getState()==WorkflowSimTags.VM_STATUS_IDLE) {
            //System.out.println("c "+cloudlet.getCloudletId()+" not scheduled, assinging c to Imin");
            //asign to this one
            chosenVM = Imin;
            cloudlet.setVmId(chosenVM.getId());
            cloudlet.setScheduled(true);
            earliestFinishTime = finishTimeMin;
            // get minimum value in
            PairingItem pair = new PairingItem(cloudlet, Imin);
            pairs.add(pair);
        }

        for (Map.Entry<CondorVM, Double> entry : allFins.entrySet()){
            PairingItem pair = new PairingItem(cloudlet, entry.getKey());
            if (!pairs.contains(pair)){
                pairs.add(pair);
            }
        }

        double fin = findFinishTime(cloudlet, chosenVM, bestReadyTime, true);
        earliestFinishTimes.put(cloudlet, earliestFinishTime);

        if (cloudletList.contains(cloudlet)){
            for (PairingItem p : pairs){
                //System.out.println("Adding pair: "+p.toString());
                rankedPairs.add(p);
            }
        }

    }

    public void doesAddingChangeCost(double readyTime, double finishTime){

//        System.out.println("in doesAddingChangeCost with readyTime: "+readyTime+
//                " and finishTime: "+finishTime);

        if (readyTime < HOUR){
            // System.out.println("new execution time : "+finishTime);
            if (finishTime <= HOUR){
                // System.out.println("time less than 1 hour, set false");
                addingChangesCost = false;
            } else {
                addingChangesCost = true;
                //System.out.println("COST GOES OVER 1 HOUR");
            }
        } else {
            double nearestHour = readyTime / HOUR;
            //System.out.println("nearestHour: "+nearestHour);

            double nearestHourAfter = finishTime / HOUR;

            //System.out.println("nearestHourAfter: "+nearestHourAfter);

            double nearestHourDiff = nearestHourAfter - nearestHour;

            //System.out.println("nearestHourDiff: "+nearestHourDiff);

            if (nearestHourDiff <= 1){
                //System.out.println("didn't go over the nearest hour, set false");
                addingChangesCost = false;
            } else {
                addingChangesCost = true;
                //System.out.println("NEAREST COST GOES OVER 1 HOUR");
            }
        }


    }

    private double findFinishTime(Cloudlet cloudlet, CondorVM vm, double readyTime,
                                  boolean occupySlot) {
        //System.out.println("vm: "+vm.getId());
        List<Event> sched = schedules.get(vm);
        double computationCost = computationCosts.get(cloudlet).get(vm);
        double start, finish;
        int pos;


        if (sched.isEmpty()) {
            if (occupySlot) {
                sched.add(new Event(readyTime, readyTime + computationCost));
            }
            return readyTime + computationCost;
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return start + computationCost;
        }

        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }
            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }
            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }

            return start + computationCost;
        }
        if (occupySlot) {
            sched.add(pos, new Event(start, finish));
        }
        return finish;
    }

    public double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList){
        double pairRank = Double.MAX_VALUE;
        rankedPairs = new ArrayList<>();

        // reset all collections
        computationCosts = new HashMap<>();
        transferCosts = new HashMap<>();
        rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        schedules = new HashMap<>();
        cloudletRank = new ArrayList<>();
        allCloudlets = new ArrayList<>();

        //setup allCloudlets
        int cloudletSize = cloudletList.size();
        int vmSize = vmList.size();

        averageBandwidth = calculateAverageBandwidth(vmList);

        for (Object vmObject : vmList) {
            CondorVM vm = (CondorVM) vmObject;
            schedules.put(vm, new ArrayList<>());
        }

        double currentTime = CloudSim.clock();
        for(Object cloudObj : cloudletList){
            Cloudlet cloudlet = (Cloudlet) cloudObj;
            //System.out.print("cloudlet: "+cloudlet.getCloudletId());
            double allocationTime = currentTime; // for the children
            if (cloudlet.getAllocationTime()==null){
                cloudlet.setAllocationTime(allocationTime);
            }

            Job job = (Job) cloudlet;
            //System.out.println(" is job: "+job.getCloudletId());
            allCloudlets.add(cloudlet);
            //getAllCloudletsChildren(job.getChildList(), allocationTime);
        }

        // Sort cloudlets according to rank
        calculateComputationCosts(vmList);
        calculateTransferCosts();
        calculateRanks();

        // Should mirror allocateCloudlets() in GRPHEFT Scheduling Algorithm
        allocateCloudlets(vmList, cloudletList);

        // De-allocate/remove allocation times for tasks that weren't ready
        deallocateNonReadyCloudlets(cloudletList);


        //System.out.println("There are : "+ rankedPairs.size()+" pairs in rankedPairs");
        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            //System.out.println("GRPHEFT ranked pairs: "+p.toString());
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    System.out.println(" grpheft Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rankedPairs.indexOf(p));
                    return rankedPairs.indexOf(p);
                }
            }
        }

        System.out.println("Shouldn't get here");
        return pairRank;
    }

    private void deallocateNonReadyCloudlets(List cloudletList) {
        for (Cloudlet c : allCloudlets){
            c.setScheduled(false);
            if (!cloudletList.contains(c)){
            //    System.out.println("de-allocating cloudlet "+c.getCloudletId());
                c.setAllocationTime(null);
                c.setVmId(-1);
            } //else {System.out.println("cloudlet "+c.getCloudletId()+" was a ready task: "+c.getAllocationTime());}
        }
    }

}