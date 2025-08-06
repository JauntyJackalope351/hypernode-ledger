package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerParameters
{
    private int maxMessageLength;
    private Set<DistributedLedgerAccount> distributedLedgerAccountReassignProposals = new HashSet<>();
    private int messageUpdateFrequencyPerHour;
    private int frameProcessingTimeMilliseconds;
    private int maxConnections;
    private int groupSize;
    private int groupInstances;
    private int transmitRedundancy;
    private BigDecimal amountRequestedToBeValidator;
    private BigDecimal transactionCost;
    private int maxTransactionsPerBlock;


   /**
    * Distributes a given number of participants into groups, optimizing the group size
    * and the number of group instances based on a maximum connection limit.
    * The primary goal of this method is to determine a {@code groupSize} (base)
    * and {@code groupInstances} (exponent) such that {@code groupSize^groupInstances}
    * is greater than or equal to the total {@code participants}, while considering
    * {@code maxConnections} as a factor in limiting or influencing the base group size.
    *
    * <p>The distribution logic handles several cases:
    * <ul>
    * <li>If {@code participants} is 0 or less, both {@code groupSize} and
    * {@code groupInstances} are set to 0.</li>
    * <li>If a single group is sufficient (e.g., participants are fewer than maxConnections
    * or maxConnections is zero/negative), a single group is used.</li>
    * <li>A sanity check is performed to ensure {@code participants} does not
    * exceed 1,000,000,000.</li>
    * <li>Otherwise, an iterative process determines the optimal {@code groupInstances}
    * (exponent) and a corresponding {@code groupSize} (base) that can accommodate
    * all participants, considering {@code maxConnections} as an influencing factor.</li>
    * <li>The final {@code groupSize} is calculated as the ceiling of the root of
    * {@code participants} by {@code groupInstances}.</li>
    * </ul>
    *
    * @param participants The total number of individuals or items to be distributed.
    * var maxConnections: The maximum number of connections or capacity influencing
    * the base size of a group at a given level. A value of 0 or less indicates
    * no practical limit, leading to all participants being in one group.
    * @throws IllegalArgumentException If {@code participants} exceeds 1,000,000,000,
    * indicating an excessively large input that the system cannot handle.
    * @throws IllegalStateException If a suitable group configuration cannot be found
    * within reasonable computational bounds (e.g., if {@code participants} is
    * too large to be represented by {@code long} for the calculated exponent,
    * or if the algorithm cannot converge).
    */

    public void setGroupParameters(int participants)
    {
        // Handle trivial cases first
        if (participants <= 0) {
            this.groupSize = 0;
            this.groupInstances = 0;
            return;
        }

        if (participants < maxConnections || maxConnections <= 0) { // maxConnections <= 0 implies no practical limit or invalid limit
            this.groupSize = participants;
            this.groupInstances = 1;
            return;
        }

        // Validate participants within a reasonable range
        if (participants > 1_000_000_000) {
            throw new IllegalArgumentException("Number of participants exceeds the supported limit (1,000,000,000).");
        }

        // Find the optimal exponent (number of instances/levels)
        // We're looking for the smallest 'exponent' such that we can find a 'groupSize'
        // that satisfies the conditions.
        int currentExponent = 1;
        int connections = (participants-1)*3;

        // The loop continues as long as the current grouping capacity isn't enough
        // and we haven't exceeded a reasonable exponent limit to prevent infinite loops
        // or extremely large calculations. A typical max exponent for practical
        // group distributions wouldn't be astronomically high.
        while (maxConnections < connections)
        {
            currentExponent++;
            int newConnections = (currentExponent * ((int) Math.ceil(Math.pow(participants, (1.0/currentExponent))-1)));

            if(newConnections >= connections) //if going up on one exponent does not lower the connections we reached the asymptotic area of the function
            {
                currentExponent--;
                break;
            }
            connections = newConnections;

            // Safety break for extremely large exponents or if participants is unreachable
            if (currentExponent > 60) { // ~60 is log2(Long.MAX_VALUE)
                throw new IllegalStateException("Could not find suitable group configuration within reasonable bounds.");
            }

        }

        // After the loop, currentExponent is the first exponent that works.
        // Now, calculate the precise groupSize for this exponent.
        // We calculate the exponent-th root of participants and round up.
        int finalGroupSize = (int) Math.ceil(Math.pow(participants, 1.0 / currentExponent));

        // Assign the results
        this.groupSize = finalGroupSize;
        this.groupInstances = currentExponent;
    }

    public static String calculateAddressString(Map<Integer,Integer> id)
    {
        String ret =
        id.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Sort by key
                .map(entry -> entry.getValue().toString()) // Map to string of values
                .collect(Collectors.joining(","));

        return ret;
    }
    /**
     * calculates the address based off your id and the configuration of the network
     *
     * do an integer division, and every time push the remainder in the address value
     * example: id 5, groupSize 3
     * remainder of 7/3 = 1
     * push 1 in the map
     * 7/3 = 2
     * repeat the process with 2
     * remainder of 2/3 = 2
     * push 2 into map in position 2
     * 2/3 = 0
     * finish, return the map (1,2)
     *
     * @param id the integer to investigate
     *
     * @return map of address
     */
    public Map<Integer,Integer> calculateAddress(int id)
    {
        Map<Integer,Integer> address = new HashMap<Integer,Integer>();
        int divisionValue;
        int divisionReminder;
        divisionValue = id-1;
        if(this.groupSize == 1)//fix edge case when id = 1
        {
            address.put(1,0);
            return address;
        }
        while(divisionValue > 0)
        {
            divisionReminder = Math.floorMod(divisionValue,this.groupSize);//divisionValue - (Math.floorDiv(divisionValue,this.groupSize) * this.groupSize);
            divisionValue = Math.floorDiv(divisionValue,this.groupSize);
            //pop divisionReminder in the array
            address.put(address.size()+1,divisionReminder);
        }
        while(address.size()<this.groupInstances)
        {
            address.put(address.size()+1,0);
        }
        return address;
    }

    /**
     * Based off your own tuple address determine which tuples identify
     * the servers you are supposed to communicate with
     *
     * @param _list the current list of validatorNodes that are currently authenticated
     * @param _address your own address
     * @return the list of peers you should communicate with
     */
    public List<ValidatorNode> calculatePeers(List<ValidatorNode> _list, Map<Integer,Integer> _address)
    {
        int groupInstance = 0;
        int groupElement = 0;
        Map <Integer,Integer> address = new HashMap<>();
        Set<String> addressesString = new HashSet<>();

        while(_address.size()>groupInstance)
        {
            groupInstance++;
            groupElement = 0;
            address = new HashMap<>(_address);
            while (groupElement < this.groupSize)
            {
                address.put(groupInstance,groupElement);
                if(!_address.equals( address))
                {
                    addressesString.add(LedgerParameters.calculateAddressString(address));
                }
                groupElement++;
            }
        }
        return ValidatorNode.filterByAddress(_list, addressesString);
    }
    public BigDecimal getAmountRequestedToBeValidator()
    {
        return amountRequestedToBeValidator == null? BigDecimal.ZERO : amountRequestedToBeValidator;
    }
    @JsonIgnore
    public String getStringToSign()
    {
        return
                ":" + this.getAmountRequestedToBeValidator() +
                ":" + this.getTransactionCost() +
                ":" + this.getMessageUpdateFrequencyPerHour() +
                ":" + this.getMaxTransactionsPerBlock() +
                ":" + this.getMaxConnections() +
                ":" + this.getFrameProcessingTimeMilliseconds() +
                ":" + this.getMaxMessageLength() +
                ":" + this.getTransmitRedundancy() +
                ":" + this.distributedLedgerAccountReassignProposals.stream()
                        .sorted(Comparator.comparing(DistributedLedgerAccount::uniqueString))
                        .map(DistributedLedgerAccount::uniqueString).collect(Collectors.joining("|"))
                ;
    }
    //getter and setter
    public int getMessageUpdateFrequencyPerHour() {return messageUpdateFrequencyPerHour;}
    public void setMessageUpdateFrequencyPerHour(int messageUpdateFrequencyPerHour) {this.messageUpdateFrequencyPerHour = messageUpdateFrequencyPerHour;}
    public int getFrameProcessingTimeMilliseconds() {return frameProcessingTimeMilliseconds;}
    public void setFrameProcessingTimeMilliseconds(int frameProcessingTimeMilliseconds) {this.frameProcessingTimeMilliseconds = frameProcessingTimeMilliseconds;}
    public int getGroupInstances() {return groupInstances;}
    public int getMessageTransmissionsPerRevision() {return groupInstances + transmitRedundancy;}
    public int getMessageTransmissionsPerBlock() {return 3;}
    public void setAmountRequestedToBeValidator(BigDecimal amountRequestedToBeValidator) {this.amountRequestedToBeValidator = amountRequestedToBeValidator;}
    public BigDecimal getTransactionCost() {return transactionCost;}
    public void setTransactionCost(BigDecimal transactionCost) {this.transactionCost = transactionCost;}
    public int getMaxTransactionsPerBlock() {return maxTransactionsPerBlock;}
    public void setMaxTransactionsPerBlock(int maxTransactionsPerBlock) {this.maxTransactionsPerBlock = maxTransactionsPerBlock;}
    public Set<DistributedLedgerAccount> getDistributedLedgerAccountReassignProposals() {return distributedLedgerAccountReassignProposals;}
    public void setDistributedLedgerAccountReassignProposals(Set<DistributedLedgerAccount> distributedLedgerAccountReassignProposals) {this.distributedLedgerAccountReassignProposals = distributedLedgerAccountReassignProposals;}
    public int getMaxConnections() {return maxConnections;}
    public void setMaxConnections(int maxConnections) {this.maxConnections = maxConnections;}
    public int getTransmitRedundancy() {return transmitRedundancy;}
    public void setTransmitRedundancy(int transmitRedundancy) {this.transmitRedundancy = transmitRedundancy;}
    public int getMaxMessageLength() {return maxMessageLength;}
    public void setMaxMessageLength(int maxMessageLength) {this.maxMessageLength = maxMessageLength;}

}
