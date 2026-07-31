-- The original generated SQL prompts referred to a parameter without supplying a value.
-- These tasks use a fixed threshold so they can be run in the isolated SQL editor.
UPDATE questions
SET content = regexp_replace(
    content,
    'whose salary is at least the requested minimum',
    'whose salary is at least 120000',
    'g'
)
WHERE question_type = 'SQL'
  AND title LIKE 'SQL %:%';

UPDATE questions
SET sql_test_cases = '[{"name":"Salary threshold","setupSql":"","expectedRows":[{"name":"Ada","salary":120000},{"name":"Cam","salary":120000}],"hidden":false},{"name":"No qualifying salaries","setupSql":"UPDATE employees SET salary = salary - 100000","expectedRows":[],"hidden":true}]'
WHERE question_type = 'SQL'
  AND title LIKE 'SQL %:%';
