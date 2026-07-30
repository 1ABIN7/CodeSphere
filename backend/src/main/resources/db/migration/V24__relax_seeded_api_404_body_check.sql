-- API tasks require a 404 for an unknown route, but do not prescribe its response body.
UPDATE questions
SET api_test_cases = '[{"name":"Health check","method":"GET","path":"/health","expectedStatus":200,"expectedBody":"{\"ok\":true}","hidden":false},{"name":"Missing route","method":"GET","path":"/missing","expectedStatus":404,"hidden":true}]'
WHERE question_type = 'API_IMPLEMENTATION'
  AND title LIKE 'API %:%';
