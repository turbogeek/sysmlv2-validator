# Medical Device Stability - Parametric SysML v2 Model

## Overview

This is a comprehensive parametric model demonstrating advanced SysML v2 capabilities for analyzing the stability of a medical device on a rolling pole. The model addresses center of gravity (CG) calculations, wheel locking scenarios, and tipping force analysis.

**File:** `16_medical_device_stability_v2.sysml`

## Model Purpose

The model analyzes stability for a medical IV pole device with:
- Variable IV fluid levels (affects CG over time)
- Configurable wheel locking states
- Applied external forces (pushing/moving)
- Dynamic CG calculations
- Tipping force thresholds

### Analogous Applications

This parametric approach applies equally to:
- **Tall rockets on the moon** with fuel slosh affecting CG
- **Launch vehicles** with payload shifting during transport
- **Mobile medical equipment** in hospitals
- **Construction equipment** on uneven terrain

## Model Structure

### 1. Requirements (`StabilityRequirement`, `SafetyRequirement`)

**Purpose:** Define top-level system requirements that constrain the design.

- **StabilityRequirement:** Device must remain stable under operating conditions
- **SafetyRequirement:** Define safety thresholds (tilt angle, forces)

**Key Concept:** Requirements link to the physical system through `subject` declarations.

### 2. Physical Types

Custom attribute definitions for type safety and clarity:
- `MassValue` - Masses in kilograms
- `LengthValue` - Distances in meters
- `ForceValue` - Forces in Newtons
- `AngleValue` - Angles in degrees
- `VolumeValue` - Volumes in liters
- `Position3D` - 3D position with x, y, z components

**SysML v2 Feature:** Custom attribute definitions enable domain-specific typing beyond primitive types.

### 3. Component Definitions

#### Wheel (`part def Wheel`)
- Individual wheel with locking capability
- Attributes: `isLocked`, `diameter`, `frictionCoefficient`

#### WheelBase (`part def WheelBase`)
- Base structure containing 4 wheels
- Attributes: `numWheels`, `baseRadius`
- Contains: `wheels : Wheel[4]` (array of 4 wheels)

#### Pole (`part def Pole`)
- Vertical pole structure
- Attributes: `height`, `mass`, `crossSectionArea`

#### IVBag (`part def IVBag`)
- IV fluid bag with variable volume
- Attributes: `fluidVolume`, `fluidDensity`, `bagMass`, `heightOnPole`
- **Time-varying:** Fluid volume decreases over time as IV drains

#### EquipmentTray (`part def EquipmentTray`)
- Tray holding medical equipment
- Attributes: `mass`, `heightOnPole`

### 4. Main Medical Device (`part def MedicalDevice`)

**Central System:** Integrates all components and provides parametric calculations.

#### Component Composition
- `wheelBase : WheelBase` - 4-wheel base with 30cm radius
- `pole : Pole` - 2m tall, 5kg mass
- `ivBag : IVBag` - 1L capacity, positioned at 1.8m
- `equipmentTray : EquipmentTray` - 2kg, positioned at 1.0m

#### Configuration Parameters
- `poleHeight` - Overall height
- `numLockableWheels` - Number of wheels that can lock
- `maxTiltAngle` - Maximum safe tilt (15 degrees)
- `minStabilityForce` - Minimum force to prevent tipping (50 N)

#### State Variables
- `tiltAngle` - Current tilt angle
- `appliedForce` - External force being applied
- `timeStep` - Simulation time for dynamic analysis

#### Parametric Calculations

**calc totalMass**
- Sums mass of pole, IV bag, and equipment tray
- **Input:** Component masses
- **Output:** Total system mass

**calc centerOfGravityHeight**
- Calculates weighted average CG height
- **Formula:** `(mass1 * height1 + mass2 * height2 + ...) / totalMass`
- **Critical for:** Stability analysis - higher CG reduces stability

**calc tippingForce**
- Calculates minimum force to tip the device
- **Inputs:** CG height, base radius, total mass, wheel locking state
- **Physics:** Moment equilibrium around pivot point
- **Formula simplified:** `F_tip = Weight * (base_radius / CG_height) * locking_factor`
- **Locking factor:** Each locked wheel increases resistance by 25%

**calc stabilityMargin**
- Safety ratio: `tipping_force / applied_force`
- **Interpretation:**
  - Margin > 2.0: Safe (2x safety factor)
  - Margin = 1.0: At tipping threshold
  - Margin < 1.0: Unstable!

#### Constraints

**stabilityConstraint**
- Ensures `stabilityMargin >= 2.0` (2x safety factor)

**tiltConstraint**
- Ensures `tiltAngle <= maxTiltAngle`

**cgHeightConstraint**
- Ensures CG height is within safe operating range

### 5. Analysis Cases

#### StabilityAnalysis (`analysis def`)
- Performs stability analysis on the device
- **Objective:** Maximize stability margin
- **Returns:** Stability margin value

### 6. Use Cases

#### NormalOperation
- **Scenario:** Normal use with full IV bag, no external forces
- **Objective:** Maintain stability during routine operation

#### MovingDevice
- **Scenario:** Applying force to move device (30 N)
- **Configuration:** Wheels unlocked to allow movement
- **Verification:** Tipping force ≥ 2x applied force

#### EmergencyStop
- **Scenario:** Sudden deceleration creates inertial force (40 N)
- **Configuration:** All 4 wheels locked for emergency
- **Verification:** Stability margin ≥ 1.5

### 7. Trade Study

**Purpose:** Compare different wheel configurations to optimize stability.

#### Configurations Analyzed:
1. **wheelConfig1:** 3 wheels, 25cm radius
2. **wheelConfig2:** 4 wheels, 30cm radius (baseline)
3. **wheelConfig3:** 5 wheels, 35cm radius

**Objective:** Maximize base radius (larger radius = better stability)

**Expected Result:** Configuration 3 (5 wheels, 35cm) provides best stability.

**SysML v2 Feature:** Trade studies enable automated design space exploration.

### 8. Verification Cases

**StabilityVerification (`verification def`)**
- Formal verification of stability requirements
- Tests multiple scenarios:
  - Full IV bag scenario
  - Half-empty IV bag scenario
  - Empty IV bag scenario

### 9. Test Instances

#### testDevice1
- Configuration: Full IV bag, moderate external force (20 N)
- Must satisfy StabilityRequirement

#### testDevice2
- Configuration: Half-empty IV bag, higher external force (35 N)
- Must satisfy StabilityRequirement

**Purpose:** Concrete instances for validation and testing.

### 10. Viewpoint and View

**StabilityViewpoint (`viewpoint def`)**
- Engineering perspective focused on stability analysis

**StabilityView (`view def`)**
- Visual representation exposing test devices
- Renders both test configurations for comparison

### 11. Allocation

**PhysicalAllocation (`allocation def`)**
- Maps logical design to physical implementation
- **Ends:** Logical and physical MedicalDevice instances
- **Mapping:** `testDevice1 allocate to testDevice2`

## Parametric Analysis Workflow

### Step 1: Define Physical Model
Define components, attributes, and composition hierarchy.

### Step 2: Add Calculations
Create `calc` definitions for:
- Total mass
- Center of gravity
- Tipping force
- Stability margin

### Step 3: Add Constraints
Define `constraint` blocks that must be satisfied:
- Stability constraints
- Safety limits
- Operating envelope

### Step 4: Create Use Cases
Define operational scenarios:
- Normal operation
- Movement
- Emergency situations

### Step 5: Run Trade Studies
Explore design space:
- Different wheel configurations
- Varied pole heights
- Alternative mass distributions

### Step 6: Verify Requirements
Formal verification:
- Test against requirements
- Multiple operating scenarios
- Edge cases

## Key SysML v2 Features Demonstrated

### 1. **Parametric Modeling**
- `calc` definitions with inputs/outputs
- Expressions referencing component attributes
- Calculated properties propagate through model

### 2. **Requirement Linking**
- Requirements with `subject` declarations
- `satisfy` relationships from design to requirements
- Constraint blocks enforcing requirements

### 3. **Part Decomposition**
- Hierarchical part structure
- Part usages with multiplicities: `Wheel[4]`
- Redefinition with `:>>` operator

### 4. **Analysis Integration**
- `analysis` definitions for parametric studies
- Trade studies with multiple alternatives
- Objective functions for optimization

### 5. **Verification Integration**
- `verification` definitions
- Test cases linked to requirements
- Formal verification workflows

### 6. **Views and Viewpoints**
- `viewpoint` for stakeholder perspectives
- `view` exposing relevant model elements
- Render statements for visualization

## Future Enhancements

### Phase 1: Enhanced Physics (Current)
- ✅ Basic CG calculations
- ✅ Tipping force analysis
- ✅ Wheel locking effects

### Phase 2: Dynamic Simulation
- ⏳ Time-varying fluid volume (IV draining)
- ⏳ Fluid slosh dynamics
- ⏳ Dynamic CG shifts over time

### Phase 3: Environmental Factors
- ⏳ Floor slope/inclination
- ⏳ Wind forces (outdoor use)
- ⏳ Vibration during transport

### Phase 4: Parametric Solver Integration
- ⏳ Constraint satisfaction solver
- ⏳ Optimization algorithms
- ⏳ Monte Carlo uncertainty analysis
- ⏳ Open-source math library integration (e.g., Apache Commons Math, JBlas)

## Testing in Cameo Systems Modeler

### Prerequisites
1. Cameo Systems Modeler 2024x or later
2. SysML v2 plugin enabled
3. Parametric diagram capability

### Import Steps
1. Open Cameo Systems Modeler
2. Create new SysML v2 project
3. Import `16_medical_device_stability_v2.sysml`
4. Resolve any library dependencies (ScalarValues, ISQ, SI)

### Validation Steps
1. **Syntax Validation:** Verify model parses without errors
2. **Semantic Validation:** Check constraint consistency
3. **Parametric Diagram:** Visualize calculation dependencies
4. **Requirement Diagram:** View requirement satisfaction
5. **Trade Study Execution:** Run wheel configuration trade study

### Expected Cameo Behavior
- All calculations should appear in parametric diagram
- Constraints should be evaluable (may need value assignments)
- Trade study should rank alternatives by objective function
- Requirements should trace to design elements

## Validator Testing

### Current Status
✅ **PASSED** - Model validates with 0 errors using SysML v2 Validator v0.1.0

### Command
```bash
java -jar sysml-validator.jar 16_medical_device_stability_v2.sysml
```

### Validation Coverage
- ✅ All syntax correct
- ✅ Part definitions and usages
- ✅ Calc definitions (structure)
- ✅ Constraint definitions
- ✅ Requirement definitions
- ✅ Analysis definitions
- ✅ Use case definitions
- ✅ Verification definitions
- ✅ Viewpoint and view definitions
- ✅ Allocation definitions
- ✅ Attribute redefinitions (`:>>`)
- ✅ Satisfy relationships
- ✅ Trade study structure

### Known Limitations (Validator Phase 1)
- ⚠️ Expression evaluation not yet implemented
- ⚠️ Constraint solving not yet implemented
- ⚠️ Trade study execution not yet implemented

**Note:** These are planned for future phases with math solver integration.

## Educational Value

This model serves as a comprehensive example for:
- **Students:** Learning SysML v2 parametric modeling
- **Engineers:** Template for stability analysis
- **Tool Vendors:** Reference for implementing parametric features
- **Researchers:** Basis for formal methods research

## Conclusion

This parametric model demonstrates the power of SysML v2 for systems engineering:
- **Integrated:** Requirements, design, analysis, and verification
- **Parametric:** Calculations propagate through model
- **Traceable:** Design decisions traced to requirements
- **Analyzable:** Trade studies enable design optimization
- **Verifiable:** Formal verification of requirements satisfaction

The medical device stability problem showcases real-world complexity while remaining accessible for educational purposes.

---

**Model Version:** 2.0
**Validator:** SysML v2 Validator v0.1.0-SNAPSHOT
**Validation Status:** ✅ PASSED (0 errors)
**Last Updated:** 2025-01-25
**Author:** Claude Code + Human Engineer
**License:** MIT
