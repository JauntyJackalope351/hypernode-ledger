package com.hypernode.ledger.contracts;

import com.hypernode.ledger.ErrorHandling;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LedgerHistory
{
    private StatusDataContract originalContract;
    private Set<BlockRevisionResult> blockRevisionResultsProcessed;

    public void addNewBlockRevisionResult(BlockRevisionResult blockRevisionResult) {this.blockRevisionResultsProcessed.add(blockRevisionResult);}

    public static LedgerHistory create(StatusDataContract originalContract)
    {
        LedgerHistory ledgerHistory = new LedgerHistory();
        ledgerHistory.originalContract = originalContract;
        ledgerHistory.blockRevisionResultsProcessed = new HashSet<>();
        return ledgerHistory;
    }
    public LedgerHistory getInterval(int startingContractId, int endBlockId)
    {
        LedgerHistory ledgerHistory = new LedgerHistory();
        if(this.originalContract.getId() > startingContractId)
        {
            ledgerHistory = LedgerHistory.create(this.originalContract);
        }
        else
        {
            try
            {
                BlockRevisionResult blockRevisionResult = blockRevisionResultsProcessed.stream().filter(b -> b.statusDataContractStarting.getId() == startingContractId).findFirst().get();
                ledgerHistory = LedgerHistory.create(blockRevisionResult.statusDataContractStarting);

            }
            catch (Exception e)
            {
                ErrorHandling.logEvent("error",false,e);
                return null;
            }

        }
        ledgerHistory.blockRevisionResultsProcessed.addAll(
        blockRevisionResultsProcessed.stream().filter(b -> b.statusDataContractStarting.getId() >= startingContractId && b.newStatusDataContract.getId() < endBlockId).collect(Collectors.toSet())
        );
        return ledgerHistory;
    }

    public void changeOriginalContract(int startingContractId)
    {
        try
        {
            BlockRevisionResult blockRevisionResult = blockRevisionResultsProcessed.stream().filter(b -> b.statusDataContractStarting.getId() == startingContractId).findFirst().get();

            this.originalContract = blockRevisionResult.statusDataContractStarting;
            this.blockRevisionResultsProcessed = blockRevisionResultsProcessed.stream().filter(b -> b.statusDataContractStarting.getId() == startingContractId).collect(Collectors.toSet());
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
        }
    }

    public StatusDataContract getOriginalContract() {return originalContract;}
    public Set<BlockRevisionResult> getBlockRevisionResultsProcessed() {return blockRevisionResultsProcessed;}

}
