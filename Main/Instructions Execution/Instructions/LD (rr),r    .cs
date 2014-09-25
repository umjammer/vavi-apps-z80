﻿// AUTOGENERATED CODE
//
// Do not make changes directly to this (.cs) file.
// Change "LD (rr),r    .tt" instead.

namespace Konamiman.Z80dotNet
{
    public partial class Z80InstructionExecutor
    {
        /// <summary>
        /// The LD (BC),A instruction.
        /// </summary>
        byte LD_aBC_A()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.BC.ToUShort(), 
				Registers.A);
            return 7;
        }

        /// <summary>
        /// The LD (DE),A instruction.
        /// </summary>
        byte LD_aDE_A()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.DE.ToUShort(), 
				Registers.A);
            return 7;
        }

        /// <summary>
        /// The LD (HL),A instruction.
        /// </summary>
        byte LD_aHL_A()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.A);
            return 7;
        }

        /// <summary>
        /// The LD (HL),B instruction.
        /// </summary>
        byte LD_aHL_B()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.B);
            return 7;
        }

        /// <summary>
        /// The LD (HL),C instruction.
        /// </summary>
        byte LD_aHL_C()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.C);
            return 7;
        }

        /// <summary>
        /// The LD (HL),D instruction.
        /// </summary>
        byte LD_aHL_D()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.D);
            return 7;
        }

        /// <summary>
        /// The LD (HL),E instruction.
        /// </summary>
        byte LD_aHL_E()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.E);
            return 7;
        }

        /// <summary>
        /// The LD (HL),H instruction.
        /// </summary>
        byte LD_aHL_H()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.H);
            return 7;
        }

        /// <summary>
        /// The LD (HL),L instruction.
        /// </summary>
        byte LD_aHL_L()
        {
		    FetchFinished();
            ProcessorAgent.WriteToMemory(
				Registers.HL.ToUShort(), 
				Registers.L);
            return 7;
        }

    }
}
