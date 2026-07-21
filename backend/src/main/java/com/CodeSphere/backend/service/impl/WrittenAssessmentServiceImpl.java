package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.WrittenAssessmentService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WrittenAssessmentServiceImpl implements WrittenAssessmentService {

    private final AssessmentAnswerRepository answerRepository;
    private final QuestionBankRepository questionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public AssessmentAnswer saveAndQueueWrittenAnswer(Long sessionId, Long questionId, String rawHtmlAnswer) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        // 1. Sanitize the rich text HTML input safely
        String sanitizedHtml = Jsoup.clean(rawHtmlAnswer, Safelist.relaxed());

        // 2. Extract plain text to perform an accurate word count validation
        String plainText = Jsoup.parse(sanitizedHtml).text().trim();
        int wordCount = plainText.isEmpty() ? 0 : plainText.split("\\s+").length;

        if (wordCount < question.getMinWordCount() || wordCount > question.getMaxWordCount()) {
            throw new IllegalArgumentException("Word count validation failed. Current: " + wordCount
                    + ", Required: [" + question.getMinWordCount() + " - " + question.getMaxWordCount() + "]");
        }

        // 3. Upsert answer record
        AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                .orElse(new AssessmentAnswer());

        answer.setSessionId(sessionId);
        answer.setQuestionId(questionId);
        answer.setSelectedAnswer(sanitizedHtml);
        answer.setEvaluationStatus("PENDING_EVALUATION");

        AssessmentAnswer savedAnswer = answerRepository.save(answer);

        // 4. Push event message payload to RabbitMQ for downstream manual/AI grading
        String routingMessage = String.format("{\"sessionId\":%d,\"questionId\":%d,\"answerId\":%d}",
                sessionId, questionId, savedAnswer.getId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_EVALUATION,
                RabbitMQConfig.ROUTING_KEY_EVALUATION,
                routingMessage
        );

        return savedAnswer;
    }
}