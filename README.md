# AI Practical Assessment - Support Ticket Management System

A comprehensive Support Ticket Management System built on AEM as a Cloud Service. Features a state machine-driven ticket lifecycle, RESTful API endpoints, and a vanilla JavaScript frontend with real-time form validation.

## Features

- **Ticket Lifecycle Management** — Create, read, update tickets with state machine enforcement
- **RESTful API** — `/bin/api/tickets` endpoints for ticket CRUD operations
- **Form Validation** — Client-side and server-side validation with detailed error messages
- **User Assignment** — Assign tickets to AEM users with existence validation
- **Comment System** — Add immutable comments to tickets for audit trail
- **Search & Filter** — Full-text search and status-based filtering
- **Responsive Design** — Mobile-friendly UI with dark mode support
- **BEM Styling** — Consistent CSS naming convention across components

## Modules

* [core:](core/README.md) OSGi bundle with TicketService, TicketCreateServlet, validation logic
* [ui.apps:](ui.apps/README.md) AEM components (Ticket Create Form), templates, client libraries
* [ui.apps.structure:](ui.apps.structure/) Defines repository content structure
* [ui.content:](ui.content/README.md) Initial content, sample data, and permissions configuration
* [ui.config:](ui.config/) OSGi configurations, service user mapping, RepoInit scripts, CSRF settings
* [ui.frontend:](ui.frontend/) Webpack-based build for vanilla JavaScript and SCSS
* [it.tests:](it.tests/README.md) Integration tests using AEM Testing Clients
* [ui.tests:](ui.tests/README.md) End-to-end Cypress tests for user flows
* [dispatcher:](dispatcher/) Cloud-optimized Dispatcher configuration
* [all:](all/) Content package aggregating all modules for deployment

## AEM SDK Setup

Before building and deploying, obtain and start the AEM SDK Quickstart:

1. **Download the AEM SDK** from [Adobe Managed Services](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-as-a-cloud-service-sdk)
   - Log in with your Adobe credentials
   - Download the AEM SDK Quickstart JAR for your operating system

2. **Extract and start AEM**
   ```bash
   # Extract the SDK
   unzip aem-sdk-quickstart-*.zip
   
   # Navigate to the extracted directory
   cd aem-sdk-quickstart-*/
   
   # Start AEM (author instance on port 4502)
   java -jar aem-quickstart.jar
   ```
   - AEM will start with a demo user `admin` / password `admin`
   - Wait for the startup message: `[OK] Quickstart finished OK`

3. **Verify AEM is running**
   - Visit http://localhost:4502 in your browser
   - Log in with `admin` / `admin`
   - Verify the author environment loads

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage

## API Usage Examples

The project provides a RESTful API for ticket management. Below are example curl commands for the main endpoints.

### Create a ticket

```bash
curl -X POST http://localhost:4502/bin/api/tickets \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "title": "Fix login issue",
    "description": "Users unable to log in after update",
    "priority": "HIGH",
    "assignedto": "admin"
  }'
```

### List all tickets

```bash
curl -X GET http://localhost:4502/bin/api/tickets \
  -u admin:admin
```

### List tickets with search and filter

```bash
# Search by keyword
curl -X GET "http://localhost:4502/bin/api/tickets?search=login" \
  -u admin:admin

# Filter by status
curl -X GET "http://localhost:4502/bin/api/tickets?status=Open" \
  -u admin:admin

# Search and filter together
curl -X GET "http://localhost:4502/bin/api/tickets?search=login&status=Open" \
  -u admin:admin
```

### Get a specific ticket

```bash
curl -X GET "http://localhost:4502/bin/api/tickets?id=ticket-a1b2c3d4" \
  -u admin:admin
```

### Update ticket fields

```bash
curl -X PUT "http://localhost:4502/bin/api/tickets?id=ticket-a1b2c3d4" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "title": "Fixed: login issue resolved",
    "priority": "MEDIUM"
  }'
```

### Change ticket status

```bash
curl -X PUT "http://localhost:4502/bin/api/tickets?id=ticket-a1b2c3d4" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "newStatus": "In Progress"
  }'
```

### Add a comment to a ticket

```bash
curl -X POST "http://localhost:4502/bin/api/tickets/comments" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "id": "ticket-a1b2c3d4",
    "message": "Working on the fix"
  }'
```

### Get all comments for a ticket

```bash
curl -X GET "http://localhost:4502/bin/api/tickets/comments?id=ticket-a1b2c3d4" \
  -u admin:admin
```

## Documentation

The build process also generates documentation in the form of README.md files in each module directory for easy reference. Depending on the options you select at build time, the content may be customized to your project.

## Testing

There are three levels of testing contained in the project:

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test

### Integration tests

This allows running integration tests that exercise the capabilities of AEM via
HTTP calls to its API. To run the integration tests, run:

    mvn clean verify -Plocal

Test classes must be saved in the `src/main/java` directory (or any of its
subdirectories), and must be contained in files matching the pattern `*IT.java`.

The configuration provides sensible defaults for a typical local installation of
AEM. If you want to point the integration tests to different AEM author and
publish instances, you can use the following system properties via Maven's `-D`
flag.

| Property              | Description                                         | Default value           |
|-----------------------|-----------------------------------------------------|-------------------------|
| `it.author.url`       | URL of the author instance                          | `http://localhost:4502` |
| `it.author.user`      | Admin user for the author instance                  | `admin`                 |
| `it.author.password`  | Password of the admin user for the author instance  | `admin`                 |
| `it.publish.url`      | URL of the publish instance                         | `http://localhost:4503` |
| `it.publish.user`     | Admin user for the publish instance                 | `admin`                 |
| `it.publish.password` | Password of the admin user for the publish instance | `admin`                 |

The integration tests in this archetype use the [AEM Testing
Clients](https://github.com/adobe/aem-testing-clients) and showcase some
recommended [best
practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices) to
be put in use when writing integration tests for AEM.

## Static Analysis

The `analyse` module performs static analysis on the project for deploying into AEMaaCS. It is automatically
run when executing

    mvn clean install

from the project root directory. Additional information about this analysis and how to further configure it
can be found here https://github.com/adobe/aemanalyser-maven-plugin

### UI tests

They will test the UI layer of your AEM application using Cypress framework.

Check README file in `ui.tests` module for more details.

Examples of UI tests in different frameworks can be found here: https://github.com/adobe/aem-test-samples

## ClientLibs

The frontend module is made available using an [AEM ClientLib](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/clientlibs.html). When executing the NPM build script, the app is built and the [`aem-clientlib-generator`](https://github.com/wcm-io-frontend/aem-clientlib-generator) package takes the resulting build output and transforms it into such a ClientLib.

A ClientLib will consist of the following files and directories:

- `css/`: CSS files which can be requested in the HTML
- `css.txt` (tells AEM the order and names of files in `css/` so they can be merged)
- `js/`: JavaScript files which can be requested in the HTML
- `js.txt` (tells AEM the order and names of files in `js/` so they can be merged
- `resources/`: Source maps, non-entrypoint code chunks (resulting from code splitting), static assets (e.g. icons), etc.

## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

## Documentation

- [Data Model](data-model.md) — JCR structure, queries, and service user configuration
- [Implementation Plan](implementation-plan.md) — Project phases and deliverables
- [API Contract](api-contract.md) — RESTful API specifications
- [Requirements](Requirements-Document.md) — Project requirements and acceptance criteria
- [AI Prompts](ai-prompts/) — Record of AI-assisted development and code review
