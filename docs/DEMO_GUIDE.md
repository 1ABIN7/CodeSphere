# CodeSphere — End-to-End Demonstration Guide

Use separate accounts for an administrator, a candidate, and optionally an evaluator.

## 1. Start the platform

1. Start Docker Desktop if coding tasks will be judged.
2. Start PostgreSQL and the backend, then the frontend.
3. Open `http://localhost:5173` and confirm the login page loads.

## 2. Administrator workflow

1. Sign in as an admin. You are taken to `/admin`.
2. Open **Question bank** and filter by question type.
3. Create an MCQ or written question. For executable coding or debugging work, open the Task Center, define test cases, then use its generated question-bank entry.
4. Open **Assessments**, create an assessment, add questions, set the duration and result visibility, then publish it.
5. Assign the assessment to the candidate.

## 3. Candidate workflow

1. Sign in as the candidate and open **Assessments**.
2. Start the assigned assessment. Demonstrate the countdown, question navigation, and autosave.
3. Answer an MCQ and a written question. For a coding task, submit to the judge and show verdict/test-case feedback. For SQL or API tasks, use the dedicated run-tests button.
4. Upload a file if the assessment includes a file-upload question.
5. Submit the assessment. Completed work moves to **Assessment History** rather than remaining active.

## 4. Evaluation and result release

1. Sign in as an evaluator or admin and open **Evaluation**.
2. Open a submitted written or file response. Show the candidate answer beside the question and rubric.
3. Save a review. A second reviewer can submit another review; the system uses the consensus score when both are complete.
4. As an admin, open the assessment’s candidate-result controls and select **Send final scores**.
5. Return to the candidate account and show the released result and feedback in **Assessment History**.

## 5. Reporting

1. Return to the admin account and open **Reports**.
2. Show assessment averages, candidate ranking, difficulty analysis, and question effectiveness.
3. Highlight readable elapsed times: seconds below a minute, minutes for longer attempts, and hours for long attempts.

## 6. Security and AI demonstration

1. As admin, open **Security**. Show username-linked login/logout activity and the encrypted-at-rest IP address displayed for an authorized admin.
2. Start an assessment as a candidate, switch away from the assessment tab, then return to **Security** as admin. Show the Assessment monitoring event.
3. Open **AI insights**, choose a candidate with coding submissions, and show the deterministic trend/language/verdict charts. Generate the advisory Gemini summary.
4. As a candidate, open **Coding Help** and ask a conceptual programming question. Explain that it provides tutoring and hints, not live-assessment answers.

## Demo checklist

- [ ] Admin login and dashboard
- [ ] Question/task creation and assessment publishing
- [ ] Candidate assignment and candidate login
- [ ] Timed assessment, autosave, and judged response
- [ ] Manual evaluation and final-score release
- [ ] Candidate result history
- [ ] Admin reporting dashboard
- [ ] Security activity and assessment monitoring
- [ ] Candidate Coding Help and admin AI Insights

## Optional enhancements to mention

- Google OAuth login is configured from environment variables.
- Docker-based judging provides isolated execution for supported code languages.
- SQL and API task runners use dedicated test flows.
- In-app notifications surface assignments and released results.
