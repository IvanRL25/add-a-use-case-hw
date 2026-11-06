# Student (STU) Design

> Realizes: UC-STU-deactivate-student, FR-SEC-active-account
>
> Depends on: BR-student-lifecycle, FR-SEC-authorization, FR-SEC-deny-unauthorized
>
> See: [use case](../requirements/use-cases.md#uc-stu-deactivate-student-the-course-admininstructor-deactivates-a-student), [business rules](../requirements/business-rules.md#course-administration), [SRS security requirements](../requirements/software-requirements-specification.md#security-requirements), and [architectural design](architectural-design.md#authorization).

## Overview

Deactivation is an explicit student-lifecycle operation, not a side effect of the general student edit form. An authorized course admin or instructor views a student, confirms the documented consequence, and disables the existing `PeerEvaluationUser.enabled` flag. The student, weekly activity reports, and peer evaluations remain intact. A disabled student remains visible in student management and is handled by the reactivation use case.

Every protected request validates the account identified by its JWT against the current enabled state. This closes the existing-token gap: deactivation takes effect on the next protected request, even if the token was issued before the operation.

## Flow

```mermaid
sequenceDiagram
    actor Actor as Course admin / assigned instructor
    participant List as Students.vue
    participant Dialog as DeactivateStudentDialog
    participant API as StudentController
    participant Guard as StudentLifecycleAuthorizationManager
    participant Service as StudentService
    participant DB as StudentRepository
    participant Auth as JWT active-account validator

    Actor->>List: Choose an active student (steps 1-3)
    List->>Dialog: Present consequence and confirmation (step 4)
    alt Actor cancels
        Actor->>Dialog: Cancel (extension 3a)
        Dialog-->>List: Close without request
    else Actor confirms
        Actor->>Dialog: Confirm (step 5)
        Dialog->>API: PATCH /students/{studentId}/deactivation
        API->>Guard: Course-admin ownership or section-instructor assignment
        Guard->>DB: Resolve actor scope and student
        Guard-->>API: Allow or 403 (extensions 2a, 5b)
        API->>Service: deactivateStudent(studentId)
        Service->>DB: Scoped active-student lookup
        alt Student is active
            Service->>DB: Set enabled=false and save
            DB-->>API: Updated student
            API-->>Dialog: 200 with disabled student (step 6)
        else Student already disabled
            Service-->>API: 409 Conflict (extension 5a)
        end
    end

    Note over Auth: On every protected request, validate the JWT subject's enabled state
    Auth-->>Actor: 401 if the account has been deactivated (extension 6a)
```

## Components and API

| Component | Change | Responsibility |
|---|---|---|
| `frontend/src/pages/students/Students.vue` | Revise | Offers Deactivate only for active students and refreshes status after success. |
| `frontend/src/pages/students/DeactivateStudentDialog.vue` | New | Shows the access-revocation warning; confirms or cancels without sending a request. |
| `frontend/src/pages/students/EditStudentForm.vue` | Revise | Removes the editable status switch so general edits cannot bypass confirmation. |
| `frontend/src/apis/student/index.ts` and `types.ts` | Revise | Exposes the lifecycle request and its typed `Result` response. |
| `student/StudentController` | Revise | Adds the lifecycle endpoint and returns the normal `Result` envelope. |
| `student/StudentService` | Revise | Performs a caller-scoped active-student lookup, rejects an already-disabled student, and sets `enabled` false. |
| `student/StudentSecurityService` | Revise | Resolves course-admin ownership or instructor assignment for a named student. |
| `security/authorizationmanagers/StudentLifecycleAuthorizationManager` | New | Fails closed at the route boundary for the lifecycle operation. |
| `security/SecurityConfiguration` | Revise | Binds the endpoint to the lifecycle guard and configures active-account JWT validation. |
| `user/UserRepository` and JWT validation configuration | Revise | Resolves the JWT's `userId` and rejects disabled accounts on every protected request. |

| Endpoint | Caller | Request | Success | Errors |
|---|---|---|---|---|
| `PATCH /api/v1/students/{studentId}/deactivation` | Course admin who owns the student's course, or instructor assigned to the student's course section | No body | `200` `Result` with the disabled `StudentDto` | `403` unavailable/out of scope; `409` already deactivated |

## Decisions

**Dedicated lifecycle endpoint and confirmation.** Use a dedicated `PATCH` rather than the general student `PUT`. This makes BR-student-lifecycle, confirmation, conflict handling, and tests explicit. The rejected alternative lets an unrelated profile edit silently change `enabled`.

**Two-point lifecycle authorization.** The route guard verifies course-admin ownership or assigned-instructor membership; the service repeats that scope in its repository lookup. The rejected alternative is role-only or unscoped `findById`, which permits cross-section access.

**Database-backed active-account JWT validation.** Each protected JWT request checks that the `userId` claim still names an enabled account, returning `401` and allowing the frontend interceptor to clear local session state. The rejected alternative is waiting for the two-hour JWT expiry; that contradicts FR-SEC-active-account after a student is deactivated. A token-version or revocation-list design was rejected for this use case because it needs new persistent state; the enabled-state lookup reuses the existing account model.

## Data model and reuse

No schema or migration is needed. The operation updates the existing `PeerEvaluationUser.enabled` field; the SRS Business Domain Model remains unchanged. It reuses the student vertical slice, the shared user model, the JWT/resource-server path, `Result` error handling, and the existing course/section associations. No Level-1 architecture change is required.

## Tests

| Use case, flow | Level | Asserts |
|---|---|---|
| Main success scenario | Unit and integration | An active student in the actor's permitted scope becomes disabled; related work remains and the response contains the disabled student. |
| 2a / 5b unavailable or unauthorized | Integration | Out-of-scope, non-owning, or unassigned callers receive `403`; no student changes. |
| 3a cancellation | Frontend component | Closing the dialog sends no request and leaves the status unchanged. |
| 5a already deactivated | Unit and integration | A disabled student produces `409` with the specified message and no state change. |
| 6a existing JWT after deactivation | Integration | A token issued before deactivation receives `401` on its next protected request; after reactivation and authentication, protected access is restored. |

## Open questions / risks

The per-request active-account lookup adds one account lookup to protected JWT requests. At Project Pulse's cohort scale, correctness of immediate access revocation outweighs that cost. If scale later makes this material, replace the lookup with a persisted token-version or revocation mechanism without weakening FR-SEC-active-account.
