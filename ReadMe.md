## Running the Application with Gradle (`batchRun`)

The application uses custom Gradle project properties passed via the `-P` flag to control environment selection, database routing, credentials, and job runtime parameters.

### 1. Custom Project Properties

| Flag / Property | Allowed / Typical Values | Default | Description |
| :--- | :--- | :--- | :--- |
| `-Penv` | `local`, `dev`, `qa`, `prod` | `local` | Sets target profile; loads `application-<env>.yaml` and `-Dspring.profiles.active`. |
| `-Pdb` | `funds`, `emfs` | `funds` | Routes target database configuration and schema names. |
| `-PjobArgs` | `"key1=val1 key2=val2"` | _None_ | Passes dynamic arguments as Spring Batch `JobParameters` to `main()`. |
| `-Pusername` | e.g. `my_db_user` | `postgres` | Overrides the DB username (falls back to `~/.gradle/gradle.properties`). |
| `-Ppassword` | e.g. `my_db_pass` | `postgres` | Overrides the DB password (falls back to `~/.gradle/gradle.properties`). |
| `-PawsRegion` | `us-east-1`, `ca-central-1` | `us-east-1` | Target AWS region when resolving cloud secrets (e.g. AWS SSM). |
| `-PawsProfile` | e.g. `dev-role`, `default` | `default` | Named AWS CLI profile from `~/.aws/credentials` for cloud access. |

---

### 2. Common Usage Examples

#### Run Locally with Defaults
Runs against `funds` DB using the `local` profile:
- ```bash
  ./gradlew batchRun -Penv=local -Pdb=funds
  ```