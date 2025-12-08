---
description: 'Reusable prompts for Spring Framework to Spring Boot migration tasks'
mode: agent
---

# Spring Framework to Spring Boot Migration Prompts

This document contains reusable prompts for specific tasks in the migration process. Use these prompts with an AI agent to guide the migration from Spring Framework to Spring Boot.

---

## Prompt 1.1: Create Requirements Inspiration Document

> [ℹ️] **For Developers:** This prompt analyzes your existing Spring Boot code patterns to document current design choices, creating a baseline for migration requirements.

**Objective:** Analyze existing Spring Boot design choices in a reference repository.

**Instructions:**

Analyze the design choices made in this repository regarding its use of Spring Boot. Summarize your findings in a document named `[YOUR-APP]-spring-boot-design-choices.md`.

Your analysis should cover (but not be limited to):

- **Dependency injection strategy** (constructor injection, field injection, configuration classes, component scanning)
- **Configuration approach** (YAML/properties, profiles, environment separation)
- **Security design** (authentication, authorization, filters, security config classes, password handling, token handling, CSRF, method-level security)
- **Data access choices** (JPA, repositories, transactions, query patterns)
- **Error handling** (exception mappers, controllers, global handlers)
- **REST API design** (controllers, DTOs, input validation, response mapping)
- **Testing strategy** (SpringBootTest, slices, mocking frameworks)
- **Other relevant Spring features** (AOP, events, schedulers, caching, etc.)

Be as detailed as possible and connect each observed design choice to its implications (benefits, drawbacks, trade-offs).

---

## Prompt 1.2: Create Requirements Document

> [ℹ️] **For Developers:** This prompt creates a concise requirements document by enhancing your initial requirements with insights from the design choices analysis.

**Objective:** Enhance the requirements instruction document with comprehensive migration requirements.

**Instructions:**

You will have a document named `[YOUR-APP]-requirements-instruction` with requirements and design choices for switching a repository from Spring Framework to Spring Boot.

Enhance this document so it is complete using `[YOUR-APP]-spring-boot-design-choices` as inspiration, but keep it short and concise.

---

## Prompt 2: Generate General Analysis Document

> [ℹ️] **For Developers:** This prompt generates detailed instructions for how the AI should analyze your repository in order to perform the migration, ensuring a structured approach that enables safe, incremental migration. This document is general and can be used independent of application to migrate. 

**Objective:** Create comprehensive analysis instructions for the migration process.

**Instructions:**

Create a step-by-step in-depth description as a `.md` file of how to analyze a code repository that will be migrated from Spring Framework to Spring Boot. The description should explain in detail how the repository should be examined, what components need to be identified, and what information must be collected.

It should be written so that it can be placed inside a code repository and used as instructions for an AI agent to follow during the analysis phase. Go in depth when describing the analysis so that the AI agent can perform a complete analysis. Instruct the agent to mark unanswered questions in the analysis with `OBSERVE` so that the developer can manually investigate.

**Format:**

The analysis document should be structured to enable incremental migration where the application remains functional after each iteration. Structure the resulting document with clear, logical increments that:

- Allow the application to work after each migration step
- Minimize the risk of breaking the entire application
- Enable testing and verification at each stage
- Prioritize foundational changes before dependent features
- Group related changes together to maintain consistency

The goal is to avoid situations where everything crashes. Instead, each increment should be self-contained and leave the application in a working state.

> **💡 Developer Tip:** Enhance document creation with several models  
> **Follow-up Prompt:** Independent of this repository, is this instruction missing something?

---

## Prompt 3: Generate Application-Specific Guide

> [ℹ️] **For Developers:** This prompt creates a customized migration guide specific to your application by analyzing the codebase and applying your requirements, identifying what needs to change and suggesting improvements.

**Objective:** Create a tailored migration guide for the specific application.

> **⚠️ Note:** This step requires iteration

**Task:**

Using the `spring-framework-analysis.instructions.md` and `[YOUR-APP]-requirements.md` as instructions, go through this repository to perform an analysis of how to change the application to use Spring Boot instead of Spring Framework with XML configuration.

**Priority:** The requirements document holds higher importance. If you find conflicting instructions between the requirements and the analysis instructions, always choose the option specified in the requirements document.

**Remember:**

Focus should be on building a technically up-to-date and correct application with minimal technical debt. Therefore, if you identify something that could be improved or refactored in order to utilize Spring Boot in an optimal way, include that in the analysis. However, the main focus of the analysis is the Spring Boot migration, so finding other refactoring suggestions should not be prioritized.

**Result:**

This whole analysis should result in a document called `[YOUR-APP]-spring-framework-to-spring-boot-guide.md`. This document should focus on creating a guide on how this particular repository can be moved from Spring Framework to Spring Boot.

**Strategy:**

You should divide the analysis into several iterations to improve performance and result. It is more important that each section is covered in depth than that it is done fast.

---

## Prompt 4: Generate Progress Document

> [ℹ️] **For Developers:** This prompt creates a progress tracking document that allows the AI to work incrementally through the migration guide while marking areas that need your attention.

**Objective:** Create a progress tracking document for iterative migration.

**Instructions:**

Generate a progress document `[YOUR-APP]-spring-framework-to-spring-boot-progress.md` that can be used to iterate over the `[YOUR-APP]-spring-framework-to-spring-boot-guide.md`. 

The progress guide should:
- Give context to enable the agent to work in iterations
- Be updated when the agent iterates over the migration guide
- Mark areas needing developer attention with `OBSERVE` (e.g., design choices or similar)

**Example:**

Generate a progress document `logsender-spring-framework-to-spring-boot-progress.md` that can be used to iterate over the `logsender-spring-framework-to-spring-boot-guide.md`. The progress guide should give context to enable the agent to work in iterations. The progress guide will be updated when the agent iterates over the guide. If the agent finds something that needs the developer's attention, like a design choice or similar, this should be marked with `OBSERVE` in the progress document.

---

## Prompt 5: Perform Spring Framework to Spring Boot Change

> [ℹ️] **For Developers:** This is the main execution prompt where the AI performs the actual migration work in small increments, updating progress and flagging issues for your review.

**Objective:** Execute the migration in small, tracked increments.

> **⚠️ Note:** This step requires iteration

### Conditions

You will have two documents:

1. `[YOUR-APP]-spring-framework-to-spring-boot-guide.md` - which explains how this application should be migrated from Spring Framework to Spring Boot
2. `[YOUR-APP]-spring-framework-to-spring-boot-progress.md` - which includes the progress you have made going over the guide and migrating the application as well as `OBSERVE` markers where you need the developer's attention

### Action

You need to use the guide to perform the change from Spring Framework to Spring Boot for this repository. 

**Requirements:**
- Perform the change in small increments
- Update the progress document as you go
- The documents are there to help you, but you are expected to read necessary files to get enough context for each iteration

### Starting Point

Start by looking at the progress document to see what has been performed.

### Remember

- **Communication:** Any doubt should be communicated to the developer
- **Escalation:** If you need more information in the guide, let the developer know by adding an `OBSERVE` in the progress and end that iteration
- **Focus:** Building a technically up-to-date and correct application with minimal technical debt
- **Improvement:** If you identify something that could be improved or refactored to utilize Spring Boot in an optimal way, suggest those changes

---

## Prompt 6: Update Guide and Progress Manually

> [ℹ️] **For Developers:** Use this prompt when concerns arise during migration to update the guide and progress documents, ensuring the AI has accurate instructions going forward.

**Objective:** Address concerns and update documentation based on implementation findings.

### Problem

During the implementation of changing Spring Framework to Spring Boot, concerns have been raised.

### Solution

The guide `[YOUR-APP]-spring-framework-to-spring-boot-guide` needs to be updated to address these concerns: `[LIST CONCERNS]`.

After updating the guide, verify if the progress document needs to be updated as well.

