# 🚀 Copilot Migration Guide: Spring Framework to Spring Boot Migration Process

This guide is the sole context for the AI agent (Copilot) to autonomously perform the Spring Framework to Spring Boot migration, including iterations, document updates, and developer guidance.

---

> **Specification**  
> **app to migrate:** logsender  
> **app to use as inspiration:** private-pracitioner-service

---

## 📋 Migration Process Overview

The migration follows a structured, iterative approach with phases: Document Creation, Implementation, and Iteration/Review.

### Key Principles

- **Incremental Migration**: Each step leaves the application functional.
- **Developer Priority**: Requirements document takes precedence.
- **AI Autonomy**: Agent handles analysis, code changes, and documentation.
- **Human Oversight**: Flag uncertainties with `OBSERVE`.

---

## 📚 Documents and Artifacts

The agent creates and maintains these documents:

### 1. Requirements Document (`[YOUR-APP]-requirements.md`)
- **Purpose**: Team thoughts, requirements, design choices.
- **Guidelines**: Use Spring Boot BOM; include architecture, security, etc.
- **Priority**: Highest; resolve conflicts in favor of requirements.

### 2. Inspiration Document (`[YOUR-APP]-spring-design-choices.md`)
- **Purpose**: Analyzes existing Spring Boot patterns from a reference application.
- **Creation**: Prompt 1.1.

### 3. General Analysis Instructions (`spring-framework-analysis.instructions.md`)
- **Purpose**: Step-by-step repository analysis guide for Spring Framework applications.
- **Creation**: Prompt 2.
- **Note**: Requirements override when creating application guide.

### 4. Application-Specific Guide (`[YOUR-APP]-spring-framework-to-spring-boot-guide.md`)
- **Purpose**: Tailored migration guide for Spring Framework to Spring Boot.
- **Creation**: Prompt 3 using requirements and analysis.
- **Priority**: Requirements used here, not general analysis.

### 5. Progress Document (`[YOUR-APP]-spring-framework-to-spring-boot-progress.md`)
- **Purpose**: Tracks progress and iterations.
- **Creation**: Prompt 4.
- **Updates**: Agent updates after each iteration, marking `OBSERVE`.

---

## 🔄 Agent Workflow

The agent follows this sequence autonomously, with details on inputs, outputs, and iteration for each step.
Each phase is divided into steps. For each step the agent should:
1. If the first step of the phase, describe the phase. If the step is iterative, describe the iteration process for the first iteration.
2. Describe the step. If the step is iterative, describe the current iteration. 
3. Start with describing what you are planning on doing to complete the step, or what it needs from the developer.
Use format:
    🤖 Agent plan:
    👩‍💻 Developer input needed:
4. Wait for developer confirmation to proceed with whole step OR iteration. THIS CANNOT BE SKIPPED.
5. Summarize what has done and ask for developer confirmation to move onto first point (describe) for next step OR next iteration. 

### Phase 1: Initialization
For this phase we are setting up the necessary documents to enable the migration. The agent should first check if document exists in `.github/spring-framework-to-spring-boot-migration/instructions` folder, otherwise ask developer for it or generate it based on the instructions.
1. **Request Initial Requirements**
   - **Needed**: Developer provides `[YOUR-APP]-requirements-instruction` (developer requirements document).  
   - **Input**: None in chat.  
   - **Output**: `[YOUR-APP]-requirements-instruction.md`.  
   - **Iterative**: Optional.

2. **Request Inspiration Document**
   - **Needed**: Developer provides `[YOUR-APP]-spring-boot-design-choices.md` (Spring Boot patterns analysis from another repo).  
   - **Input**: None (from developer).  
   - **Output**: `[YOUR-APP]-spring-boot-design-choices.md`.  
   - **Iterative**: No.

3. **Execute Prompt 1.2: Enhance Requirements Document**  
   - **Needed**: Inspiration document from step 2.  
   - **Input**: `[YOUR-APP]-requirements-instruction.md` + `[YOUR-APP]-spring-boot-design-choices.md`.  
   - **Output**: `[YOUR-APP]-requirements.md` (enhanced requirements).  
   - **Iterative**: No.  
   - **Prompt**: See Prompt 1.2 in `prompts/spring-framework-to-spring-boot.prompt.md`.

4. **Execute Prompt 2: Generate General Analysis Instructions**  
   - **Needed**: None.  
   - **Input**: None.  
   - **Output**: `spring-framework-analysis.instructions.md` (general analysis guide).  
   - **Iterative**: No.  
   - **Prompt**: See Prompt 2 in `prompts/spring-framework-to-spring-boot.prompt.md`.  
   - **Follow-up**: After executing Prompt 2, ask the developer if they want to perform the follow-up prompt: "Independent of this repository, is this instruction missing something?" Print the full follow-up prompt details for the developer to decide.

5. **Execute Prompt 3: Generate Application-Specific Guide**  
   - **Needed**: Requirements and analysis instructions.  
   - **Input**: `[YOUR-APP]-requirements.md` + `spring-framework-analysis.instructions.md` + repository analysis.  
   - **Output**: `[YOUR-APP]-spring-framework-to-spring-boot-guide.md` (tailored migration guide).  
   - **Iterative**: Yes (divide analysis into iterations).  
   - **Prompt**: See Prompt 3 in `prompts/spring-framework-to-spring-boot.prompt.md`.

6. **Execute Prompt 4: Generate Progress Document**  
   - **Needed**: Application-specific guide.  
   - **Input**: `[YOUR-APP]-spring-framework-to-spring-boot-guide.md`.  
   - **Output**: `[YOUR-APP]-spring-framework-to-spring-boot-progress.md` (progress tracker).  
   - **Iterative**: No.  
   - **Prompt**: See Prompt 4 in `prompts/spring-framework-to-spring-boot.prompt.md`.

### Phase 2: Implementation
For this phase we are performing the actual migration from Spring Framework to Spring Boot using the created documents. The agent should work in small increments, updating the progress document and flagging any uncertainties with `OBSERVE`. The progress document should serve as the main tracker for the migration status so unnecessary context don't need to be read. The guide instead includes all the steps necessary to migrate the application.
1. **Execute Prompt 5: Perform Migration Changes**  
   - **Needed**: Guide and progress documents.  
   - **Input**: `[YOUR-APP]-spring-framework-to-spring-boot-guide.md` + `[YOUR-APP]-spring-framework-to-spring-boot-progress.md` + repository code.  
   - **Output**: Code changes, updated `[YOUR-APP]-spring-framework-to-spring-boot-progress.md` (with `OBSERVE` if needed).  
   - **Iterative**: Yes (small increments, update progress each time).  
   - **Prompt**: See Prompt 5 in `prompts/spring-framework-to-spring-boot.prompt.md`.

2. **Handle OBSERVE Items**  
   - **Needed**: When `OBSERVE` flagged in progress.  
   - **Input**: Progress document.  
   - **Output**: Pause iteration, inform developer of needed input.  
   - **Iterative**: As needed.

### Phase 3: Completion
After all migration steps are completed, the agent performs final verification and resolves any remaining issues.
1. **Final Verification**  
   - **Needed**: All steps completed.  
   - **Input**: Repository state.  
   - **Output**: Confirmation of functionality.  
   - **Iterative**: No.

2. **Resolve Remaining OBSERVE**  
   - **Needed**: Any unresolved flags.  
   - **Input**: Developer input.  
   - **Output**: Resolved progress.  
   - **Iterative**: As needed.

3. **Use Prompt 6: Update Documents if Concerns Arise**  
   - **Needed**: Concerns during implementation.  
   - **Input**: Concerns list.  
   - **Output**: Updated guide and progress.  
   - **Iterative**: As needed.  
   - **Prompt**: See Prompt 6 in `prompts/spring-framework-to-spring-boot.prompt.md`.

### Developer Guidance
- Clearly state required inputs.
- Summarize progress after iterations.
- Use `OBSERVE` for human decisions.

---

## 💬 Conversation Section

Copilot should always verify if the developer is satisfied with the output of each step before proceeding to the next step. This ensures quality control and allows for adjustments or clarifications.

---

## 👨‍💻 Agent Responsibilities
- Autonomous execution of analysis, changes, documentation.
- Incremental, non-breaking progress.
- Adhere to requirements, minimize debt, suggest improvements.
- Clear communication and updates.

---

## ✨ Success Criteria
- App functional on Spring Boot.
- Modern standards, minimal debt.
- All `OBSERVE` resolved, documents complete.

---

## 📝 Notes
- Replace `[YOUR-APP]` with app name (e.g., `logsender`).
- Use this guide as primary context.
- Mark uncertainties with `OBSERVE`.

---

## 📊 Progress

Track the completion of each step in the migration process. Mark with [x] when completed or if already completed but not marked.

### Phase 1: Initialization
- [ ] 1. Request Initial Requirements
- [ ] 2. Request Inspiration Document
- [ ] 3. Execute Prompt 1.2: Enhance Requirements Document
- [ ] 4. Execute Prompt 2: Generate General Analysis Instructions
- [ ] 5. Execute Prompt 3: Generate Application-Specific Guide
- [ ] 6. Execute Prompt 4: Generate Progress Document

### Phase 2: Implementation
- [ ] 1. Execute Prompt 5: Perform Migration Changes (iterative)
- [ ] 2. Handle OBSERVE Items (as needed)

### Phase 3: Completion
- [ ] 1. Final Verification
- [ ] 2. Resolve Remaining OBSERVE
- [ ] 3. Use Prompt 6: Update Documents if Concerns Arise (as needed)
