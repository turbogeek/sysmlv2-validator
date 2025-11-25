package MedicalDeviceStability {
	doc /*
	 * Parametric model for medical device on rolling pole.
	 * Demonstrates: CG calculations, stability analysis, wheel locking,
	 * tipping force calculations, IV fluid changes.
	 */

	// ============================================================================
	// REQUIREMENTS
	// ============================================================================

	requirement def StabilityRequirement {
		doc /* Device shall remain stable under operating conditions */
		subject device;
	}

	requirement def SafetyRequirement {
		doc /* Safety requirements for patient protection */
		subject device;
	}

	// ============================================================================
	// PHYSICAL TYPES
	// ============================================================================

	attribute def MassValue :> Real;
	attribute def LengthValue :> Real;
	attribute def ForceValue :> Real;
	attribute def AngleValue :> Real;
	attribute def VolumeValue :> Real;

	attribute def Position3D {
		attribute x : LengthValue;
		attribute y : LengthValue;
		attribute z : LengthValue;
	}

	// ============================================================================
	// COMPONENT DEFINITIONS
	// ============================================================================

	part def Wheel {
		doc /* Individual wheel with locking capability */
		attribute isLocked : Boolean;
		attribute diameter : LengthValue;
		attribute frictionCoefficient : Real;
	}

	part def WheelBase {
		doc /* Base structure with multiple wheels */
		attribute numWheels : Integer;
		attribute baseRadius : LengthValue;
		part wheels : Wheel[4];
	}

	part def Pole {
		doc /* Vertical pole structure */
		attribute height : LengthValue;
		attribute mass : MassValue;
		attribute crossSectionArea : Real;
	}

	part def IVBag {
		doc /* IV fluid bag with variable volume */
		attribute fluidVolume : VolumeValue;
		attribute fluidDensity : Real;
		attribute bagMass : MassValue;
		attribute heightOnPole : LengthValue;
	}

	part def EquipmentTray {
		doc /* Tray holding medical equipment */
		attribute mass : MassValue;
		attribute heightOnPole : LengthValue;
	}

	// ============================================================================
	// MAIN MEDICAL DEVICE
	// ============================================================================

	part def MedicalDevice {
		doc /* Complete medical device on rolling pole */

		part wheelBase : WheelBase {
			attribute :>> numWheels = 4;
			attribute :>> baseRadius = 0.3;
		}

		part pole : Pole {
			attribute :>> height = 2.0;
			attribute :>> mass = 5.0;
		}

		part ivBag : IVBag {
			attribute :>> fluidVolume = 1.0;
			attribute :>> heightOnPole = 1.8;
			attribute :>> fluidDensity = 1.05;
			attribute :>> bagMass = 0.1;
		}

		part equipmentTray : EquipmentTray {
			attribute :>> mass = 2.0;
			attribute :>> heightOnPole = 1.0;
		}

		// Configuration parameters
		attribute poleHeight : LengthValue;
		attribute numLockableWheels : Integer;
		attribute maxTiltAngle : AngleValue;
		attribute minStabilityForce : ForceValue;

		// State variables
		attribute tiltAngle : AngleValue;
		attribute appliedForce : ForceValue;
		attribute timeStep : Real;

		// ============================================================================
		// CALCULATIONS
		// ============================================================================

		calc totalMass {
			in pMass : MassValue = pole.mass;
			in ivMass : MassValue = ivBag.bagMass;
			in trayMass : MassValue = equipmentTray.mass;
			return : MassValue;
		}

		calc centerOfGravityHeight {
			doc /* Calculate CG height */
			in pMass : MassValue = pole.mass;
			in pHeight : LengthValue = pole.height;
			in ivMass : MassValue = ivBag.bagMass;
			in ivHeight : LengthValue = ivBag.heightOnPole;
			in trayMass : MassValue = equipmentTray.mass;
			in trayHeight : LengthValue = equipmentTray.heightOnPole;
			return : LengthValue;
		}

		calc tippingForce {
			doc /* Calculate force required to tip device */
			in baseR : LengthValue = wheelBase.baseRadius;
			in totalM : MassValue;
			return : ForceValue;
		}

		calc stabilityMargin {
			doc /* Safety margin ratio */
			in tipForce : ForceValue;
			in appForce : ForceValue = appliedForce;
			return : Real;
		}

		// ============================================================================
		// CONSTRAINTS
		// ============================================================================

		constraint stabilityConstraint {
			doc /* Device must maintain stability */
		}

		constraint tiltConstraint {
			doc /* Tilt angle must not exceed maximum */
		}

		constraint cgHeightConstraint {
			doc /* CG must be within safe range */
		}
	}

	// ============================================================================
	// ANALYSIS CASES
	// ============================================================================

	analysis def StabilityAnalysis {
		doc /* Analyze device stability */
		subject device : MedicalDevice;
		objective : MaximizeObjective;
		return : Real;
	}

	// ============================================================================
	// USE CASES
	// ============================================================================

	use case def NormalOperation {
		doc /* Normal operation with full IV bag */
		subject device : MedicalDevice;
		objective normalStability;
	}

	use case def MovingDevice {
		doc /* Moving the device safely */
		subject device : MedicalDevice;
		objective safeMovement;
	}

	use case def EmergencyStop {
		doc /* Emergency braking scenario */
		subject device : MedicalDevice;
		objective preventTipping;
	}

	// ============================================================================
	// TRADE STUDY
	// ============================================================================

	part wheelConfig1 : WheelBase {
		:>> numWheels = 3;
		:>> baseRadius = 0.25;
	}

	part wheelConfig2 : WheelBase {
		:>> numWheels = 4;
		:>> baseRadius = 0.30;
	}

	part wheelConfig3 : WheelBase {
		:>> numWheels = 5;
		:>> baseRadius = 0.35;
	}

	analysis engineTradeStudy : TradeStudy {
		subject : WheelBase[1..*] = (wheelConfig1, wheelConfig2, wheelConfig3);
		objective : MaximizeObjective;
		return part : WheelBase;
	}

	// ============================================================================
	// VERIFICATION CASES
	// ============================================================================

	verification def StabilityVerification {
		doc /* Verification test for stability requirements */
		subject device : MedicalDevice;
	}

	// ============================================================================
	// TEST INSTANCES
	// ============================================================================

	part testDevice1 : MedicalDevice {
		doc /* Test: Full IV bag, 2 wheels locked */
		:>> appliedForce = 20.0;
		satisfy StabilityRequirement;
	}

	part testDevice2 : MedicalDevice {
		doc /* Test: Half-empty IV bag, all wheels locked */
		:>> appliedForce = 35.0;
		satisfy StabilityRequirement;
	}

	// ============================================================================
	// VIEWPOINT AND VIEW
	// ============================================================================

	viewpoint def StabilityViewpoint {
		doc /* Engineering viewpoint for stability */
	}

	view def StabilityView : StabilityViewpoint {
		doc /* Visual representation of stability */
		expose testDevice1;
		expose testDevice2;
		render testDevice1;
		render testDevice2;
	}

	// ============================================================================
	// ALLOCATION
	// ============================================================================

	allocation def PhysicalAllocation {
		doc /* Allocation of logical to physical */
		end logical : MedicalDevice;
		end physical : MedicalDevice;
	}

	allocation physicalMapping : PhysicalAllocation
		allocate testDevice1 to testDevice2;
}
