package money.fluid.ilp.ledgerclient;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import money.fluid.ilp.connector.model.ids.IlpTransactionId;
import money.fluid.ilp.ledger.inmemory.InMemoryLedger;
import money.fluid.ilp.ledger.model.ConnectionInfo;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledgerx.ilp.core.LedgerInfo;
import org.interledgerx.ilp.core.LedgerTransfer;
import org.interledgerx.ilp.core.LedgerTransferRejectedReason;
import org.interledgerx.ilp.core.events.LedgerEventHandler;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An implementation of {@link LedgerClient} that communicates with an in-memory ledger.  Normally, a {@link
 * LedgerClient} would make remote calls to a ledger running in a different runtime, but this variant simulates that
 * setup by merely keeping the ledger in memory, and communicating to it directly.
 */
@ToString
@EqualsAndHashCode
public class InMemoryLedgerClient implements LedgerClient {
    @NonNull
    private final ConnectionInfo connectionInfo;

    // Strongly-typed to require an InMemoryLedger because this implementation requires functionality for RPC simulation
    // that doesn't (and shouldn't) exist in the Ledger interface.
    @NonNull
    private final InMemoryLedger inMemoryLedger;

    // Usually there's only a single Handler, but if there are multiple then they'll always be for the same Connector
    // because a client only ever talks to a single ledger on behalf of a single connector.
    @NonNull
    private Set<LedgerEventHandler> ledgerEventHandlers;

    /**
     * Default Constructor.  Initializes an empty {@link Set} of {@link LedgerEventHandler}.
     *
     * @param connectionInfo
     * @param ledger
     */
    public InMemoryLedgerClient(
            final ConnectionInfo connectionInfo, final InMemoryLedger ledger
    ) {
        this.connectionInfo = Objects.requireNonNull(connectionInfo);
        this.inMemoryLedger = Objects.requireNonNull(ledger);
        this.ledgerEventHandlers = new HashSet<>();
    }
    ///////////////

    @Override
    public LedgerInfo getLedgerInfo() {
        // Normally, this might be populated after connecting to a designated Ledger.
        return this.inMemoryLedger.getLedgerInfo();
    }

    @Override
    public void connect() {
        // This merely establishes a connection to the Ledger.  Managers for this client (e.g., a Connector) may register
        // and unregister handlers at will.
        this.inMemoryLedger.getLedgerConnectionManager().connect(this.connectionInfo);
    }

    @Override
    public void disconnect() {
        this.inMemoryLedger.getLedgerConnectionManager().disconnect(this.connectionInfo.getConnectorId());
    }

    /**
     * Initiate an ILP transfer.
     */
    @Override
    public void send(final LedgerTransfer transfer) {
        Preconditions.checkNotNull(transfer);

        // Simulate an RPC call to the ledger by merely calling the method directly on the inMemoryLedger.
        this.inMemoryLedger.send(transfer);
    }

    @Override
    public void rejectTransfer(LedgerTransfer transfer, LedgerTransferRejectedReason reason) {
        this.inMemoryLedger.rejectTransfer(transfer, reason);
    }

    @Override
    public void fulfillCondition(Fulfillment fulfillment) {
        this.inMemoryLedger.fulfillCondition(fulfillment);
    }

    @Override
    public void fulfillCondition(final IlpTransactionId ilpTransactionId) {
        this.inMemoryLedger.fulfillCondition(ilpTransactionId);
    }

    @Override
    public void registerEventHandler(LedgerEventHandler<?> handler) {
        Preconditions.checkNotNull(handler);

        // For each connection to the Ledger, the Ledger requires a new Listener in order to associate a group of
        // LedgerEventHandlers to a single Connector/Ledger combination.
        this.inMemoryLedger.getLedgerConnectionManager().registerEventHandler(
                handler.getListeningConnector().getConnectorInfo().getConnectorId(), handler
        );
    }
}