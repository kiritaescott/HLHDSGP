package ec.app.bcdsgp;

public class WorkflowStatTuple {
    private double averageResponseTime;
    private double standardDeviation;

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public double getCostOfWorkflow() {
        return costOfWorkflow;
    }

    public void setCostOfWorkflow(double costOfWorkflow) {
        this.costOfWorkflow = costOfWorkflow;
    }

    private double costOfWorkflow;


    public WorkflowStatTuple(double averageResponseTime, double standardDeviation, double costOfWorkflow){
        this.averageResponseTime = averageResponseTime;
        this.standardDeviation = standardDeviation;
        this.costOfWorkflow = costOfWorkflow;
    }


}
