package konamiman.z80;

import java.util.EventObject;

import dotnet4j.util.compat.EventHandler;


/**
 * Complements {@link Z80Processor} by adding events related to interrupts servicing.
 */
public interface Z80ProcessorInterruptEvents {

    /**
     * Triggered when a maskable interrupt is about to be serviced.
     * <ul>
     *  <li>For IM 0: The opcode has been already fetched from the data bus and is about to be executed.</li>
     *  <li>For IM 1: PC is already set to 0x0038 and the return address has been pushed to the stack.</li>
     *  <li>For IM 2: PC is already set to the address of the routine to execute and the return address has been pushed to the stack.</li>
     * </ul>
     */
    EventHandler<EventObject> maskableInterruptServicingStart();

    /**
     * Triggered when a non-maskable interrupt is about to be serviced.
     * PC is already set to 0x0066 and the return address has been pushed to the stack
     * when this event is invoked.
     */
    EventHandler<EventObject> nonMaskableInterruptServicingStart();

    /**
     * Triggered before a RETI instruction is about to be executed,
     * right after the corresponding BeforeInstructionExecution event
     */
    EventHandler<EventObject> beforeRetiInstructionExecution();

    /**
     * Triggered after a RETI instruction has been executed,
     * right after the corresponding AfterInstructionExecution event
     */
    EventHandler<EventObject> afterRetiInstructionExecution();

    /**
     * Triggered before a RETN instruction is about to be executed,
     * right after the corresponding BeforeInstructionExecution event
     */
    EventHandler<EventObject> beforeRetnInstructionExecution();

    /**
     * Triggered after a RETN instruction has been executed,
     * right after the corresponding AfterInstructionExecution event
     */
    EventHandler<EventObject> afterRetnInstructionExecution();
}
