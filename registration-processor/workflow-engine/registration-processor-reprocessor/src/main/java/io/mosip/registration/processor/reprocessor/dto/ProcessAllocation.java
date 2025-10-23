package io.mosip.registration.processor.reprocessor.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class ProcessAllocation {
    private List<String> processes;
    private List<String> statuses;
    private int percentageAllocation;

    @Override
    public String toString() {
        return "ProcessAllocation{" +
                "processes=" + processes +
                ", statuses=" + statuses +
                ", percentageAllocation=" + percentageAllocation +
                '}';
    }
}

