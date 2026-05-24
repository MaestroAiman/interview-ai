# InterviewAI — Jakarta EE 10 Web Application

AI-powered interview simulator built with pure Jakarta EE 10 (JSF 4, CDI, JAX-RS).

## Setup in IntelliJ IDEA

### Step 1 — Import project
File → Open → select the project root folder (where pom.xml is).
IntelliJ auto-detects it as a Maven project.

### Step 2 — Set Project SDK
File → Project Structure → Project → SDK → Java 21

### Step 3 — Add Tomcat 10 server
Run → Edit Configurations → + → Tomcat Server → Local
- Name: `Tomcat 10 - InterviewAI`
- Application server: click Configure → add your Tomcat 10 installation folder
  (download from https://tomcat.apache.org/download-10.cgi if not installed)
- Deployment tab → + → Artifact → `interview-ai:war exploded`
- Application context: `/interview-ai`
- HTTP port: `8080`
- On 'Update' action: Redeploy
- On frame deactivation: Update classes and resources

### Step 4 — Add firebase-service-account.json
Place the file at: `src/main/resources/firebase-service-account.json`
(Download from Firebase Console → Project Settings → Service Accounts)

### Step 5 — Set environment variable
In the Tomcat run configuration → Startup/Connection tab → Environment variables:
```
ANTHROPIC_API_KEY=your_actual_claude_api_key_here
```

### Step 6 — Run
Click the green Run button → Tomcat starts → open http://localhost:8080/interview-ai

## Alternative: Run with Maven Cargo
```
mvn clean package cargo:run
```
Then visit: http://localhost:8080/interview-ai/landing.xhtml
