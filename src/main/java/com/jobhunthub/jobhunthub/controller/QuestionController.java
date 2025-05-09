package com.jobhunthub.jobhunthub.controller;

import com.jobhunthub.jobhunthub.dto.EvaluateResponseRequest;
import com.jobhunthub.jobhunthub.dto.QuestionDTO;
import com.jobhunthub.jobhunthub.model.Question;
import com.jobhunthub.jobhunthub.service.QuestionService;
import com.jobhunthub.jobhunthub.model.User;
import com.jobhunthub.jobhunthub.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jobhunthub.jobhunthub.utils.AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
// OAuth2 specific imports
import org.springframework.security.core.Authentication;

/**
 * REST Controller for handling interview questions.
 * Provides endpoints for both behavioral and technical questions.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionService service;
    private final UserService userService;

    // Constructor injection
    public QuestionController(QuestionService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    // Retrieves a random unanswered question for the user
    @GetMapping("/{type}/question")
    public ResponseEntity<Question> getRandomQuestion(
            @PathVariable String type,
            Authentication authentication) {
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        Question question = service.getRandomQuestion(currentUser,
                Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.ok(question);
    }

    // Submits user's response for AI evaluation
    @PostMapping("/{type}/evaluate")
    public ResponseEntity<Question> evaluateResponse(
            @PathVariable String type,
            @RequestBody EvaluateResponseRequest request,
            Authentication authentication) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        Question result = service.evaluateResponse(
                request.getQuestion(),
                request.getResponse(),
                currentUser,
                Question.QuestionType.valueOf(type.toUpperCase())
        );
        return ResponseEntity.ok(result);
    }

    // Gets count of successfully answered questions for today
    @GetMapping("/{type}/count")
    public ResponseEntity<Map<String, Long>> getTodayCount(
            @PathVariable String type,
            Authentication authentication) {
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        long count = service.getTodayCount(currentUser,
                Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Resets all questions of specific type for user (marks them as unanswered)
    @PostMapping("/{type}/reset")
    public ResponseEntity<Void> resetQuestions(
            @PathVariable String type,
            Authentication authentication) {
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        service.resetAllQuestions(currentUser,
                Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.ok().build();
    }

    // Resets a specific question (marks it as unanswered)
    @PostMapping("/{type}/reset-date")
    public ResponseEntity<Void> resetQuestionDate(
            @PathVariable String type,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        service.resetQuestionDate(request.get("question"), currentUser,
                Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.ok().build();
    }

    // Adds a new question for the user
    @PostMapping("/{type}/add")
    public ResponseEntity<Question> addQuestion(
            @PathVariable String type,
            @RequestBody Question question,
            Authentication authentication) {
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        Question savedQuestion = service.addQuestion(question, currentUser,
                Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
    }

    // Gets all questions of a specific type
    @GetMapping("/{type}/all")
    public ResponseEntity<List<QuestionDTO>> getAllQuestions(
            @PathVariable String type,
            Authentication authentication) {
        logger.info("--- ENTERED getAllQuestions with type: {} ---", type);
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        List<QuestionDTO> questionDTOs = service.getQuestionsByUser(currentUser,
                Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.ok(questionDTOs);
    }

    // Deletes a question after security checks
    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable String type,
            @PathVariable Long id,
            Authentication authentication) {
        User currentUser = AuthenticationUtils.getCurrentUser(authentication, userService);
        service.deleteQuestion(id, currentUser, Question.QuestionType.valueOf(type.toUpperCase()));
        return ResponseEntity.noContent().build();
    }
}
