package com.CodeSphere.backend.seeder;

import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds practical document/work-sample upload questions. */
@Component
@Profile("dev")
@Order(7)
public class OriginalFileUploadQuestionSeeder implements CommandLineRunner {
    private final QuestionBankRepository questions;
    public OriginalFileUploadQuestionSeeder(QuestionBankRepository questions) { this.questions = questions; }
    @Override public void run(String... args) { int added = 0; for (Item item : items()) { if (questions.existsByTitleIgnoreCase(item.title)) continue; Question q = new Question(); q.setTitle(item.title); q.setContent(item.prompt); q.setType("FILE_UPLOAD"); q.setQuestionType("FILE_UPLOAD"); q.setCategory("File upload"); q.setDifficulty(item.difficulty); q.setPoints(item.points); q.setNegativeScore(0); q.setStatus(ApprovalStatus.APPROVED); questions.save(q); added++; } System.out.printf("[OriginalFileUploadQuestionSeeder] %d file-upload questions added.%n", added); }
    private record Item(String title, String prompt, String difficulty, int points) { }
    private List<Item> items() { return List.of(
        new Item("Resume Upload", "Upload your current resume as a PDF or DOCX. Include education, relevant experience, skills, and contact information.", "EASY", 5),
        new Item("CV Upload", "Upload your curriculum vitae (CV) as a PDF or DOCX. Include academic, professional, research, and publication experience as applicable.", "EASY", 5),
        new Item("Portfolio Submission", "Upload a PDF portfolio, slide deck, or ZIP file that demonstrates two or more relevant projects. Briefly label your contribution to each project.", "MEDIUM", 10),
        new Item("Work Sample", "Upload one representative work sample relevant to this role. Include a short cover page explaining the goal, your role, and the outcome.", "MEDIUM", 10),
        new Item("Project README", "Upload a ZIP file containing a small project and README. The README should explain setup, usage, design decisions, and known limitations.", "MEDIUM", 10),
        new Item("Technical Design Document", "Upload a PDF or DOCX technical design document for a feature of your choice. Include requirements, architecture, trade-offs, and testing considerations.", "HARD", 15),
        new Item("Data Analysis Report", "Upload a PDF, DOCX, or slide deck that summarizes an analysis. Include the question, method, findings, limitations, and recommendation.", "MEDIUM", 10),
        new Item("Presentation Deck", "Upload a PPTX or PDF presentation explaining a solution to a business or technical problem. Keep it concise and audience-focused.", "MEDIUM", 10),
        new Item("Code Review Exercise", "Upload a PDF or DOCX code-review report for a small code sample. Identify issues, risks, and recommended improvements.", "HARD", 15),
        new Item("Professional Certification", "Upload a relevant professional certification, transcript, or completion document as a PDF or image packaged in a ZIP file.", "EASY", 5)
    ); }
}
