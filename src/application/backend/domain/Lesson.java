package application.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons", uniqueConstraints = @UniqueConstraint(name = "uk_lesson_slug", columnNames = "slug"))
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submodule_id", nullable = false)
    private Submodule submodule;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String bodyMarkdown;

    @Column(nullable = false, columnDefinition = "text")
    private String exampleCode;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean published;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private Simulation simulation;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    protected Lesson() {
    }

    public Lesson(Submodule submodule, String slug, String title, String summary, String bodyMarkdown,
                  String exampleCode, int position, boolean published) {
        this.submodule = submodule;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.bodyMarkdown = bodyMarkdown;
        this.exampleCode = exampleCode;
        this.position = position;
        this.published = published;
    }

    public Long getId() { return id; }
    public Submodule getSubmodule() { return submodule; }
    public void setSubmodule(Submodule submodule) { this.submodule = submodule; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBodyMarkdown() { return bodyMarkdown; }
    public void setBodyMarkdown(String bodyMarkdown) { this.bodyMarkdown = bodyMarkdown; }
    public String getExampleCode() { return exampleCode; }
    public void setExampleCode(String exampleCode) { this.exampleCode = exampleCode; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Simulation getSimulation() { return simulation; }
    public void setSimulation(Simulation simulation) { this.simulation = simulation; }
    public List<QuizQuestion> getQuizQuestions() { return quizQuestions; }
}
