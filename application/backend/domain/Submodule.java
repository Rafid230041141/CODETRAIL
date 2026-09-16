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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submodules")
public class Submodule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false)
    private int position;

    @OneToMany(mappedBy = "submodule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();

    protected Submodule() {
    }

    public Submodule(CourseModule module, String title, int position) {
        this.module = module;
        this.title = title;
        this.position = position;
    }

    public Long getId() { return id; }
    public CourseModule getModule() { return module; }
    public void setModule(CourseModule module) { this.module = module; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public List<Lesson> getLessons() { return lessons; }
}
