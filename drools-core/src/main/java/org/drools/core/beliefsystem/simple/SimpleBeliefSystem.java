package org.drools.core.beliefsystem.simple;

import org.drools.core.beliefsystem.BeliefSet;
import org.drools.core.beliefsystem.BeliefSystem;
import org.drools.core.common.EqualityKey;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.LogicalDependency;
import org.drools.core.common.NamedEntryPoint;
import org.drools.core.common.ObjectTypeConfigurationRegistry;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.ObjectTypeConf;
import org.drools.core.spi.Activation;
import org.drools.core.spi.PropagationContext;

import static org.drools.core.reteoo.PropertySpecificUtil.allSetButTraitBitMask;

/**
 * Default implementation emulates classical Drools TMS behaviour.
 *
 */
public class SimpleBeliefSystem
        implements
        BeliefSystem<SimpleMode> {
    private NamedEntryPoint        ep;
    private TruthMaintenanceSystem tms;

    public SimpleBeliefSystem(NamedEntryPoint ep,
                              TruthMaintenanceSystem tms) {
        super();
        this.ep = ep;
        this.tms = tms;
    }

    public TruthMaintenanceSystem getTruthMaintenanceSystem() {
        return this.tms;
    }

    public void insert(LogicalDependency<SimpleMode> node,
                       BeliefSet<SimpleMode> beliefSet,
                       PropagationContext context,
                       ObjectTypeConf typeConf) {
        boolean empty = beliefSet.isEmpty();

        beliefSet.add( node.getMode() );

        InternalFactHandle bfh = beliefSet.getFactHandle();
        if ( empty && bfh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            ep.insert( bfh,
                       bfh.getObject(),
                       node.getJustifier().getRule(),
                       node.getJustifier(),
                       typeConf,
                       null );
        }
    }

    public void read(LogicalDependency<SimpleMode> node,
                     BeliefSet<SimpleMode> beliefSet,
                     PropagationContext context,
                     ObjectTypeConf typeConf) {
        //insert(node, beliefSet, context, typeConf );
        beliefSet.add( node.getMode() );
    }

    public void delete(LogicalDependency<SimpleMode> node,
                       BeliefSet<SimpleMode> beliefSet,
                       PropagationContext context) {
        SimpleBeliefSet sBeliefSet = (SimpleBeliefSet) beliefSet;
        beliefSet.remove( node.getMode() );

        InternalFactHandle bfh = beliefSet.getFactHandle();

        if ( beliefSet.isEmpty() && bfh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet), (RuleImpl) context.getRule(), (Activation) context.getLeftTupleOrigin() );
        } else if ( !beliefSet.isEmpty() && bfh.getObject() == node.getObject() && node.getObject() != bfh.getObject() ) {
            // prime has changed, to update new object
            // Equality might have changed on the object, so remove (which uses the handle id) and add back in
            ((NamedEntryPoint)bfh.getEntryPoint()).getObjectStore().updateHandle(bfh, ((SimpleMode) beliefSet.getFirst()).getObject().getObject());

            ((NamedEntryPoint) bfh.getEntryPoint() ).update( bfh, bfh.getObject(), allSetButTraitBitMask(), Object.class, null );
        }

        if ( beliefSet.isEmpty() ) {
            // if the beliefSet is empty, we must null the logical handle
            EqualityKey key = bfh.getEqualityKey();
            key.setLogicalFactHandle( null );
            key.setBeliefSet( null );

            if ( key.getStatus() == EqualityKey.JUSTIFIED ) {
                // if it's stated, there will be other handles, so leave it in the TMS
                tms.remove( key );
            }
        }
    }

    public void stage(PropagationContext context,
                      BeliefSet<SimpleMode> beliefSet) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        // Remove the FH from the network
        ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet),(RuleImpl) context.getRule(), null);

        bfh.getEqualityKey().setStatus( EqualityKey.STATED ); // revert to stated
    }

    public void unstage(PropagationContext context,
                        BeliefSet<SimpleMode> beliefSet) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        bfh.getEqualityKey().setStatus( EqualityKey.JUSTIFIED ); // revert to justified

        // Add the FH back into the network
        ep.insert(bfh, bfh.getObject(), (RuleImpl) context.getRule(), null, getObjectTypeConf(beliefSet), null );
    }

    private ObjectTypeConf getObjectTypeConf(BeliefSet beliefSet) {
        InternalFactHandle fh = beliefSet.getFactHandle();
        ObjectTypeConfigurationRegistry reg;
        ObjectTypeConf typeConf;
        reg = ep.getObjectTypeConfigurationRegistry();
        typeConf = reg.getObjectTypeConf( ep.getEntryPoint(), fh.getObject() );
        return typeConf;
    }

    public BeliefSet newBeliefSet(InternalFactHandle fh) {
        return new SimpleBeliefSet( this, fh );
    }

    public LogicalDependency newLogicalDependency(Activation activation,
                                                  BeliefSet beliefSet,
                                                  Object object,
                                                  Object value) {
        SimpleMode mode = new SimpleMode();
        SimpleLogicalDependency dep =  new SimpleLogicalDependency( activation, beliefSet, object, mode );
        mode.setObject( dep );
        return dep;
    }

}
