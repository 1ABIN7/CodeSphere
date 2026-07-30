package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.config.JudgeEngineConfig;
import com.CodeSphere.backend.dto.ApiRunResponse;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.service.ApiAssessmentRunnerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Runs a candidate Node.js HTTP service and its HTTP checks in the same
 * network-disabled Docker container. The service is reachable only by the
 * trusted harness through localhost:3000, never by the host or internet.
 */
@Service
public class ApiAssessmentRunnerServiceImpl implements ApiAssessmentRunnerService {
    private static final String IMAGE = "node:20-slim";
    private final ObjectMapper objectMapper;
    private final JudgeEngineConfig config;
    public ApiAssessmentRunnerServiceImpl(ObjectMapper objectMapper, JudgeEngineConfig config) { this.objectMapper = objectMapper; this.config = config; }

    @Override
    public ApiRunResponse run(Question question, String candidateCode) {
        if (!config.isUseDocker()) throw new IllegalStateException("The API sandbox is not enabled. Set judge.use-docker=true on the judge host.");
        if (candidateCode.length() > config.getMaxCodeLength()) throw new IllegalArgumentException("API submission is too large.");
        List<ApiCase> cases = parseCases(question.getApiTestCases());
        if (cases.isEmpty()) throw new IllegalStateException("This API task has no HTTP test cases yet.");
        Path directory = null;
        try {
            directory = Files.createTempDirectory(Paths.get(config.getTempDir()), "api-task-");
            Files.writeString(directory.resolve("solution.js"), candidateCode, StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("harness.js"), harnessScript(cases), StandardCharsets.UTF_8);
            String volume = directory.toAbsolutePath() + ":/app:ro";
            List<String> command = List.of("docker", "run", "--rm", "--network", "none", "--memory", "256m", "--memory-swap", "256m", "--cpus", "1", "--pids-limit", "64", "--read-only", "--tmpfs", "/tmp:rw,noexec,nosuid,size=16m", "-v", volume, IMAGE, "sh", "-c", "node /app/solution.js >/tmp/server.log 2>&1 & pid=$!; node /app/harness.js; status=$?; kill $pid 2>/dev/null || true; exit $status");
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output;
            try (InputStream stream = process.getInputStream()) { output = new String(stream.readAllBytes(), StandardCharsets.UTF_8); }
            if (!process.waitFor(12, TimeUnit.SECONDS)) { process.destroyForcibly(); return unavailable(cases, "API service exceeded its time limit."); }
            String json = output.lines().filter(line -> line.startsWith("__API_RESULT__")).reduce((a, b) -> b).map(line -> line.substring("__API_RESULT__".length())).orElse(null);
            if (json == null) return unavailable(cases, "The API service did not start correctly.");
            List<HarnessResult> raw = objectMapper.readValue(json, new TypeReference<List<HarnessResult>>() {});
            List<ApiRunResponse.CaseResult> visible = new ArrayList<>(); int passed = 0;
            for (int index = 0; index < cases.size(); index++) {
                ApiCase testCase = cases.get(index); HarnessResult result = raw.get(index);
                if (result.passed()) passed++;
                visible.add(new ApiRunResponse.CaseResult(testCase.hidden() ? "Hidden test" : displayName(testCase), result.passed(), testCase.hidden() && !result.passed() ? "Hidden test did not pass" : result.message()));
            }
            double score = Math.round(passed * 1000D / cases.size()) / 10D;
            return new ApiRunResponse(passed == cases.size() ? "ACCEPTED" : "WRONG_ANSWER", passed, cases.size(), score, visible, passed == cases.size() ? "All HTTP checks passed." : "Some HTTP checks did not pass.");
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt(); return unavailable(cases, "API sandbox could not be started.");
        } finally { if (directory != null) delete(directory); }
    }

    private String harnessScript(List<ApiCase> cases) throws IOException {
        String checks = objectMapper.writeValueAsString(cases);
        return """
                const http = require('http');
                const checks = %s;
                const normalize = v => { try { return JSON.stringify(JSON.parse(v)); } catch { return String(v).trim(); } };
                const request = c => new Promise(resolve => { const body = c.body == null ? '' : (typeof c.body === 'string' ? c.body : JSON.stringify(c.body)); const req = http.request({hostname:'127.0.0.1',port:3000,path:c.path||'/',method:c.method||'GET',headers:{'content-type':'application/json','content-length':Buffer.byteLength(body)}}, res => { let data=''; res.on('data',x=>data+=x); res.on('end',()=>{ const bodyMatches = c.expectedBody == null || normalize(data)===normalize(typeof c.expectedBody==='string'?c.expectedBody:JSON.stringify(c.expectedBody)); resolve({passed:res.statusCode===Number(c.expectedStatus||200)&&bodyMatches,message:'HTTP '+res.statusCode}); }); }); req.on('error',e=>resolve({passed:false,message:'Service unavailable'})); req.setTimeout(2500,()=>{req.destroy();resolve({passed:false,message:'Request timed out'});}); req.write(body); req.end(); });
                (async()=>{ for(let i=0;i<20;i++){ try { await new Promise((resolve,reject)=>{const r=http.get('http://127.0.0.1:3000/',x=>{x.resume();resolve()});r.on('error',reject)}); break; } catch { await new Promise(r=>setTimeout(r,100)); } } const results=[]; for(const check of checks) results.push(await request(check)); console.log('__API_RESULT__'+JSON.stringify(results)); })();
                """.formatted(checks);
    }
    private List<ApiCase> parseCases(String json) { try { return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, new TypeReference<List<ApiCase>>() {}); } catch (Exception e) { throw new IllegalStateException("The API task test cases are invalid."); } }
    private ApiRunResponse unavailable(List<ApiCase> cases, String message) { return new ApiRunResponse("SANDBOX_UNAVAILABLE", 0, cases.size(), 0, List.of(), message); }
    private String displayName(ApiCase c) { return c.name() == null || c.name().isBlank() ? "HTTP test" : c.name(); }
    private void delete(Path directory) { try (var paths = Files.walk(directory)) { paths.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) {} }); } catch (IOException ignored) {} }
    private record ApiCase(String name, String method, String path, Object body, Integer expectedStatus, Object expectedBody, boolean hidden) {}
    private record HarnessResult(boolean passed, String message) {}
}
