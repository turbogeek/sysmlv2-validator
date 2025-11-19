package StockTicker_MagicGrid {
    doc /*
    Stock Ticker Device - MagicGrid Structured Model

    This is the root package organizing the Stock Ticker system model
    following the MagicGrid systems engineering methodology framework.

    MagicGrid Structure:
    - 3 Domains: Problem, Solution, Implementation
    - 5 Pillars: Requirements, Structure, Behavior, Parameters, Safety & Reliability

    Model Organization:
    1. Problem Domain
       - Black Box (Stakeholder perspective)
       - White Box (Functional decomposition)
    2. Solution Domain
       - System Level (Logical architecture)
       - Subsystem Level (Detailed design)
       - Configuration Level (Product variants)
    3. Implementation Domain
       - Physical realization

    Project: Personal Stock Ticker Device
    Purpose: Portable device for monitoring real-time stock prices
    Target Users: Individual investors, day traders

    Last Updated: 2025-11-10
    */

    private import SysML::*;
    private import ScalarValues::*;

    // Import all domain packages
    public import StockTicker_ProblemDomain_BlackBox::*;
    public import StockTicker_ProblemDomain_WhiteBox::*;
    public import StockTicker_SolutionDomain::*;
    public import StockTicker_ImplementationDomain::*;

    // Import architecture framework
    public import StockTicker_Viewpoints::*;
}
