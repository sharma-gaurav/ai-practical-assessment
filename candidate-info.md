# Candidate Information

**Name:** Gaurav Sharma  
**Role:** Software Engineer  
**Primary Technology Stack:** Java, Vanilla JavaScript, AEM  

**Primary AI Tool Used:** Claude (via Claude Code)  
**Project Option Selected:** Support Ticket Management System (AEM-based)  

**Assessment Start Date:** 2026-09-01  
**Submission Date:** TBD  

## Project Summary

A full-stack Support Ticket Management System built on AEM as a Cloud Service. The system allows internal users to create, manage, search, and progress support tickets through a defined lifecycle with status validation, commenting, and priority management.

## Tools Used

- **Backend:** AEM as a Cloud Service, Java, OSGi, Sling Models
- **Frontend:** Vanilla JavaScript, HTML5, CSS3, Webpack
- **Database:** AEM JCR (Java Content Repository) with AEM's built-in persistence
- **Testing:** JUnit, Sling Mocks, AEM Test Clients
- **UI Testing:** Cypress for end-to-end tests
- **Build Tool:** Maven
- **AI Tool:** Claude Code

## Setup Summary

Refer to [README.md](README.md) for complete setup instructions. Quick start:

1. Clone the repository
2. Run `mvn clean install -PautoInstallSinglePackage` to build and deploy to local AEM SDK
3. Access Author at `http://localhost:4502/` and Publisher at `http://localhost:4503/`
4. Frontend accessible via deployed components in AEM Pages