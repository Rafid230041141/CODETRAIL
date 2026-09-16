package application.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private int questionOrder;

    @Column(nullable = false, length = 600)
    private String prompt;

    @Column(nullable = false, length = 300)
    private String optionA;
    @Column(nullable = false, length = 300)
    private String optionB;
    @Column(nullable = false, length = 300)
    private String optionC;
    @Column(nullable = false, length = 300)
    private String optionD;

    @Column(nullable = false)
    private int correctIndex;

    @Column(nullable = false, length = 500)
    private String explanation;

    protected QuizQuestion() {
    }

    public QuizQuestion(Lesson lesson, int questionOrder, String prompt, String optionA, String optionB,
                        String optionC, String optionD, int correctIndex, String explanation) {
        this.lesson = lesson;
        this.questionOrder = questionOrder;
        this.prompt = prompt;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }

    public Long getId() { return id; }
    public Lesson getLesson() { return lesson; }
    public int getQuestionOrder() { return questionOrder; }
    public String getPrompt() { return prompt; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
}
