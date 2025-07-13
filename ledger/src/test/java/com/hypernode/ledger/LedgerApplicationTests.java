package com.hypernode.ledger;

import com.hypernode.ledger.contracts.LedgerParameters;
import com.hypernode.ledger.contracts.Signature;
import com.hypernode.ledger.contracts.ValidatorNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@SpringBootTest
class LedgerApplicationTests {
/*
	@Test
	void contextLoads() {

	}*/
    int groupSize;
    int groupInstances;

    @Test
    public void test124()
    {
        this.distributeParticipants(26,12);
        ErrorHandling.logEvent("",false,null);
    }
    public static ValidatorNode getvalidatorNode(int i, LedgerParameters ledgerParameters)
    {
        ValidatorNode validatorNode = new ValidatorNode();
        validatorNode.setPublicKey("a"+i);
        validatorNode.setSignature("");
        validatorNode.setConnectionString("");
        validatorNode.setAddress(ledgerParameters.calculateAddress(i));
        return validatorNode;
    };
    @Test
    public void test25()
    {

        LedgerParameters ledgerParameters = new LedgerParameters();
        ledgerParameters.setMaxConnections(6);
        ledgerParameters.setGroupParameters(8);

        List<ValidatorNode> validatorNodeList = new ArrayList<>();
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(1,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(2,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(3,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(4,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(5,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(6,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(7,ledgerParameters));
        validatorNodeList.add(LedgerApplicationTests.getvalidatorNode(8,ledgerParameters));


        List result;
        result = ledgerParameters.calculatePeers(validatorNodeList,ledgerParameters.calculateAddress(4));
        result = ledgerParameters.calculatePeers(validatorNodeList,ledgerParameters.calculateAddress(4));
        result.size();
    }

    public void distributeParticipants(int participants, int maxConnections) {
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
        long currentProduct = maxConnections; // Use long to prevent overflow during intermediate calculations

        // The loop continues as long as the current grouping capacity isn't enough
        // and we haven't exceeded a reasonable exponent limit to prevent infinite loops
        // or extremely large calculations. A typical max exponent for practical
        // group distributions wouldn't be astronomically high.
        while (currentProduct < participants) {
            currentExponent++;

            // Calculate the theoretical base needed for the current exponent
            // We use long for intermediate division to preserve precision before ceiling
            long theoreticalBase = (long) Math.ceil((double) maxConnections / currentExponent);

            // Ensure the base is at least the exponent itself, to prevent base from becoming 1 prematurely
            // which would cause an infinite loop if participants is large.
            // This also implies a minimum size for a group in a hierarchical structure.
            currentProduct = (long) Math.pow(Math.max(theoreticalBase, currentExponent), currentExponent);

            // Safety break for extremely large exponents or if participants is unreachable
            if (currentExponent > 60 && participants > currentProduct) { // ~60 is log2(Long.MAX_VALUE)
                // This means participants is so large that even with a large exponent,
                // we can't reach it, or the calculation is becoming impractical.
                // Could throw an exception or set to max possible values.
                throw new IllegalStateException("Could not find suitable group configuration within reasonable bounds.");
            }
        }

        // After the loop, currentExponent is the first exponent that might work.
        // Now, calculate the precise groupSize for this exponent.
        // We calculate the exponent-th root of participants and round up.
        int finalGroupSize = (int) Math.ceil(Math.pow(participants, 1.0 / currentExponent));

        // Assign the results
        this.groupSize = finalGroupSize;
        this.groupInstances = currentExponent;
    }
}
