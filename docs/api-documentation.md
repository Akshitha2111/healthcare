# API documentation

Base URL (dev): `http://localhost:8080` (or `/api` through the Vite proxy at `http://localhost:5173`).
All bodies are JSON. Auth is the `hda_token` HttpOnly cookie (or `Authorization: Bearer <jwt>`).
State-changing requests that use the cookie must send `X-Requested-With: XMLHttpRequest`.

**Error format (all failures)**
```json
{ "timestamp": "2026-10-31T10:00:00Z", "status": 400, "code": "VALIDATION_FAILED",
  "message": "Request validation failed", "path": "/api/auth/register",
  "fieldErrors": { "password": "Password must contain at least one letter and one number" } }
```
Codes: `VALIDATION_FAILED, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, CSRF_CHECK_FAILED, NOT_FOUND, CONFLICT, RATE_LIMITED,
AI_SERVICE_UNAVAILABLE (503), AI_SERVICE_ERROR (502), INTERNAL_ERROR`.

Paged responses: `{ "content": [...], "page": 0, "size": 10, "totalElements": 3, "totalPages": 1 }` (`size` ≤ 50).

## Auth
| Method & path | Role | Notes |
|---|---|---|
| `POST /api/auth/register` | public | `{fullName,email,password}` → `201 {user}` + cookie. 409 if e-mail exists |
| `POST /api/auth/login` | public | `{email,password}` → `200 {user}` + cookie. 401 `Invalid email or password` |
| `POST /api/auth/logout` | public | `204`, clears cookie |
| `GET /api/auth/me` | any | `{id,fullName,email,role,createdAt}` |

## Patient (`ROLE_PATIENT`)
| | |
|---|---|
| `GET /api/patients/me` | `{fullName,email,phone,dateOfBirth,gender,memberSince}` |
| `PUT /api/patients/me` | `{fullName, phone?, dateOfBirth?, gender?}`; gender ∈ FEMALE, MALE, OTHER, PREFER_NOT_TO_SAY |
| `GET /api/patients/me/summary` | `{totalPredictions, upcomingAppointments, predictionsByMonth[{month,count}]}` |

## Symptoms & predictions
`GET /api/symptoms` (any authenticated) → `{count,minSymptoms,maxSymptoms,symptoms:[{name,label}]}`

`POST /api/predictions` (patient, rate-limited) — request
```json
{ "symptoms": ["itching", "skin_rash", "nodal_skin_eruptions"] }
```
`201` response (values illustrative; real values come from the model)
```json
{ "id": 12, "createdAt": "2026-10-31T10:00:00Z", "predictedClass": "Fungal infection",
  "modelName": "logistic_regression", "modelVersion": "1.0.0", "scoreType": "uncalibrated_model_score",
  "symptoms": [{"name":"itching","label":"Itching"}],
  "results": [{"rank":1,"disease":"Fungal infection","score":0.61},{"rank":2,"disease":"Allergy","score":0.21}],
  "description": "…", "generalGuidance": ["…"], "urgentCareNotice": "…", "limitations": ["…"],
  "explanation": {"method":"…","influentialSymptoms":[{"symptom":"itching","label":"Itching","influence":0.3}],"caveat":"…"},
  "disclaimer": "This is an educational prediction, not a medical diagnosis." }
```
Errors: 400 (invalid/unsupported symptoms), 429, 503 (AI timeout/unreachable), 502 (AI error).

`GET /api/predictions/my?page=&size=&q=&from=YYYY-MM-DD&to=YYYY-MM-DD&sort=asc|desc` → paged
`{id,createdAt,predictedClass,topScore,symptoms[],modelVersion}`
`GET /api/predictions/{id}` → full prediction (404 unless it belongs to the caller).

## Doctors & appointments
| | |
|---|---|
| `GET /api/doctors` | active doctors `{id,name,specialty,availableDays[],startTime,endTime,slotMinutes,active}` |
| `GET /api/doctors/{id}/slots?date=YYYY-MM-DD` | `[{time:"09:00:00",available:true}]` (empty if not a working day) |
| `GET /api/appointments/my?page=&size=&status=&upcoming=` | patient's own; `cancellable` flag included |
| `POST /api/appointments` | `{doctorId,date,time}` → 201 `PENDING`. 400 invalid slot/past/too far (90 d); 409 taken |
| `PATCH /api/appointments/{id}/cancel` | own PENDING/CONFIRMED future appointment; 404 if not yours; 409 if not cancellable |

Status transitions: `PENDING→CONFIRMED|CANCELLED`, `CONFIRMED→COMPLETED|CANCELLED`; COMPLETED/CANCELLED are final;
COMPLETED only after the appointment time.

## Admin (`ROLE_ADMIN`)
| | |
|---|---|
| `GET /api/admin/dashboard` | `{totalPatients,newPatientsLast30Days,activeDoctors,totalAppointments,pendingAppointments,appointmentsByStatus{},totalPredictions,predictionsLast7Days}` |
| `GET /api/admin/analytics` | `{minGroupSize,predictionsPerDay[30],topSymptoms[],topPredictedConditions[],modelUsage[]}` |
| `GET /api/admin/appointments?status=&page=&size=` | includes `patientName` and `allowedTransitions` |
| `PATCH /api/admin/appointments/{id}/status` | `{status}`; 409 on an invalid transition |
| `GET/POST /api/admin/doctors`, `PUT /api/admin/doctors/{id}` | `{name,specialty,availableDays[],startTime,endTime,slotMinutes,active}` |

## Health
`GET /actuator/health/liveness` (process up) · `GET /actuator/health/readiness` (DB + AI service) · `GET /actuator/health` (all).

## Flask AI service (internal)
| | |
|---|---|
| `GET /health` | `200 {"status":"ok","model_loaded":true,"model_version":…}` or `503 {"status":"degraded"}` |
| `GET /symptoms` | `{success,count,min_symptoms,max_symptoms,symptoms:[{name,label}]}` |
| `POST /predict` | `{"symptoms":[…]}` → `{success,model_version,model_name,score_type,top_prediction,predictions[{rank,disease,score}],symptoms_used,explanation,description,general_guidance,urgent_care_notice,limitations,disclaimer}` |

Errors: `{"success":false,"error":{"code":"INVALID_SYMPTOMS","message":"unsupported symptom(s): foo"}}` (400),
`INVALID_JSON` (400), `UNAUTHORIZED` (401 when `AI_SERVICE_API_KEY` is set), `MODEL_UNAVAILABLE` (503), `INTERNAL_ERROR` (500).
Rules: 2–20 distinct symptoms; names are normalised (`"High Fever"` → `high_fever`).
