package com.kineticdata.bridgehub.adapter.generic.rest;

import com.kineticdata.bridgehub.adapter.Record;
import java.util.Comparator;
import java.util.Map;

public class GenericRestComparator implements Comparator<Record>{
    private final Map<String, String> sortOrderItems;

    public GenericRestComparator(Map<String, String> sortOrderItems) {
      this.sortOrderItems = sortOrderItems;
    }

    @Override
    public int compare(Record r1, Record r2) {
        // Initialize result to represent both records being equal
        int result = 0;

        // Looping the sortOrderItems supports multi-tiered sorting
        for (Map.Entry<String,String> sortOrderItem : sortOrderItems.entrySet()) {
            String fieldName = sortOrderItem.getKey();
            boolean isAscending = "asc".equals(sortOrderItem.getValue().toLowerCase());

            // Get the values of the record to compare
            String r1Value = normalize(r1.getValue(fieldName));
            String r2Value = normalize(r2.getValue(fieldName));

            // Order based on field direction specified
            int fieldComparison = (isAscending)
                ? r1Value.compareTo(r2Value)
                : r2Value.compareTo(r1Value);
            if (fieldComparison != 0) {
                result = fieldComparison;
                break;
            }
        }

        return result;
    }

    protected String normalize(Object string) {
        String result;
        if (String.valueOf(string) == null) {
            result = "";
        } else {
            result = String.valueOf(string);
        }
        return result;
    }
}
