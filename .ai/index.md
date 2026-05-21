# AI Knowledge Orchestrator

## 🎯 Objective
This file acts as the central registry and routing engine for all rules within the `.ai` directory. It defines how an agent must discover, load, and prioritize instructions based on the current task context.

##  Namespace Registry (Static)
The following namespaces are available. An agent must check these namespaces to determine which rules are relevant to the current file or task.

### 1. Standard (`standard:[name]`)
*The Supreme Authority. These rules are immutable and override all others.*
- `standard:core` (Core project values and universal constraints)
- `standard:security` (Universal security protocols)
- `standard:testing` (Universal testing requirements)
- `standard:documentation` (Universal documentation standards)

### 2. Technology (`tech:[name]`)
*The Technological Foundation. Rules related to specific languages and tools.*
- `tech:kotlin`
- `tech:sql`
- `tech:docker`
- [Add more as discovered...]

### 3. Feature (`feature:[name]`)
*The Functional Layer. Rules specific to business logic and modules.*
- `feature:api`
- `feature:cache`
- `feature:auth`
- [Add more as discovered...]

## ⚙️ Operational Protocols

### 1. Contextual Loading Procedure
Before performing any task (coding, auditing, or documentation), the agent **MUST**:
1.  **Analyze the context:** Identify the files, languages, and business modules currently being handled.
2.  **Identify relevant namespaces:** Determine which `standard:`, `tech:`, and `feature:` namespaces are applicable.
    *   *Example:* If working on a Kotlin-based API, the agent must load `standard:core`, `standard:security`, `tech:kotlin`, and `feature:api`.
3.  **Verify Existence:** Check if the identified rules exist in the `.ai` directory before proceeding.

### 2. Conflict & Authority Protocol
The hierarchy of authority is: **Standard > Technology > Feature**.

If the agent detects a logical contradiction (e.g., a `feature:api` rule contradicts a `standard:security` rule):
1.  **IMMEDIATE HALT:** The agent must stop all code generation or task execution immediately.
2.  **Conflict Alert:** The agent must issue a clear error message in the chat using the following format:
    > ⚠️ **[CONFLICT DETECTED]** ⚠️
    > **Conflict between:** `[namespace:rule_A]` and `[namespace:rule_B]`
    > **Description: [Brief explanation of the contradiction]**
3.  **Await Human Resolution:** Do not attempt to resolve or bypass the conflict. Wait for the human to provide a new instruction or update the rules.

### 3. Rule Verification Requirement
Before generating any implementation or response, the agent must perform a "Compliance Check" to ensure the proposed solution adheres to all loaded namespaces.
